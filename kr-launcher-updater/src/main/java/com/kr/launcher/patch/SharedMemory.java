package com.kr.launcher.patch;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

/**
 * Windows named shared memory + named Mutex for cross-process IPC with
 * HPatchZ.exe.
 * Corresponds to KRSharedMemory.cs.
 *
 * Uses JNA to call Win32 APIs:
 * - CreateFileMappingW: creates/opens a named file mapping backed by the system
 * page file.
 * - MapViewOfFile: maps the file mapping into this process's address space.
 * - CreateMutexW: creates/opens a named mutex for cross-process
 * synchronization.
 * - WaitForSingleObject / ReleaseMutex: acquire/release the mutex.
 *
 * HPatchZ.exe is a native Windows program that opens the same named shared
 * memory
 * and mutex by name, so this implementation is wire-compatible with the C#
 * version.
 *
 * The shared memory contains 6 ulong (8 bytes each) = 48 bytes total:
 * [0] PatchedFileCount
 * [1] FileTotalCount
 * [2] PatchingFileCurrBytes
 * [3] PatchingFileTotalBytes
 * [4] PatchedCurrBytes
 * [5] PatchTotalBytes
 */
public class SharedMemory implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(SharedMemory.class);

    /** Kernel32 function declarations needed beyond JNA's built-in Kernel32. */
    public interface Kernel32Ext extends StdCallLibrary {
        Kernel32Ext INSTANCE = Native.load("kernel32", Kernel32Ext.class, W32APIOptions.DEFAULT_OPTIONS);

        WinNT.HANDLE CreateFileMappingW(WinNT.HANDLE hFile, Pointer lpAttributes, int flProtect,
                int dwMaximumSizeHigh, int dwMaximumSizeLow, String lpName);

        Pointer MapViewOfFile(WinNT.HANDLE hFileMappingObject, int dwDesiredAccess,
                int dwFileOffsetHigh, int dwFileOffsetLow, int dwNumberOfBytesToMap);

        boolean UnmapViewOfFile(Pointer lpBaseAddress);

        WinNT.HANDLE CreateMutexW(Pointer lpMutexAttributes, boolean bInitialOwner, String lpName);

        boolean ReleaseMutex(WinNT.HANDLE hMutex);

        int WaitForSingleObject(WinNT.HANDLE hHandle, int dwMilliseconds);

        boolean CloseHandle(WinNT.HANDLE hObject);
    }

    // Win32 constants
    private static final int PAGE_READWRITE = 0x04;
    private static final int FILE_MAP_ALL_ACCESS = 0x000F001F;
    private static final int WAIT_OBJECT_0 = 0x00000000;
    private static final int WAIT_TIMEOUT = 0x00000102;
    private static final int WAIT_FAILED = 0xFFFFFFFF;
    private static final int INFINITE = 0xFFFFFFFF;

    private static final Kernel32Ext K32 = Kernel32Ext.INSTANCE;

    private WinNT.HANDLE fileMappingHandle;
    private WinNT.HANDLE mutexHandle;
    private Pointer mappedView;
    private int mappedSize;

    /**
     * Create or open a named shared memory region.
     * Corresponds to C# KRSharedMemory.Create(key, size) which uses
     * MemoryMappedFile.CreateOrOpen(key, size) and new Mutex(false, key +
     * "_mutex").
     *
     * @param key  unique name (e.g., "launcher_shared_memory_1234_abc123")
     * @param size size in bytes (4096 in C# code)
     * @return 0 on success, Win32 HRESULT on error
     */
    public int create(String key, int size) {
        if (mappedView != null) {
            log.info("Shared memory was already created");
            return 0;
        }
        try {
            // Create or open named file mapping backed by system page file.
            // Equivalent to C# MemoryMappedFile.CreateOrOpen(key, size).
            // C# MemoryMappedFile internally uses INVALID_HANDLE_VALUE (-1) for hFile
            // to indicate page file backing. NULL (0) is invalid per MSDN.
            fileMappingHandle = K32.CreateFileMappingW(
                    WinBase.INVALID_HANDLE_VALUE,
                    null,
                    PAGE_READWRITE,
                    0,
                    size,
                    key);
            if (fileMappingHandle == null || fileMappingHandle.getPointer().equals(Pointer.NULL)) {
                int err = Native.getLastError();
                log.error("CreateFileMappingW failed, key: {}, error: {}", key, err);
                return err;
            }

            // Map view of file into this process's address space.
            mappedView = K32.MapViewOfFile(fileMappingHandle, FILE_MAP_ALL_ACCESS, 0, 0, 0);
            if (mappedView == null) {
                int err = Native.getLastError();
                log.error("MapViewOfFile failed, key: {}, error: {}", key, err);
                K32.CloseHandle(fileMappingHandle);
                fileMappingHandle = null;
                return err;
            }

            // Zero-initialize the shared memory region (C# uses WriteArray with new
            // byte[size]).
            byte[] zeros = new byte[size];
            mappedView.write(0, zeros, 0, size);

            // Create or open named mutex for cross-process synchronization.
            // Equivalent to C# new Mutex(false, key + "_mutex").
            mutexHandle = K32.CreateMutexW(null, false, key + "_mutex");
            if (mutexHandle == null || mutexHandle.getPointer().equals(Pointer.NULL)) {
                int err = Native.getLastError();
                log.error("CreateMutexW failed, key: {}, error: {}", key, err);
                K32.UnmapViewOfFile(mappedView);
                mappedView = null;
                K32.CloseHandle(fileMappingHandle);
                fileMappingHandle = null;
                return err;
            }

            mappedSize = size;
            log.info("Shared memory created: key={}, size={}", key, size);
            return 0;
        } catch (Exception e) {
            log.error("Failed to create shared memory, key: {}", key, e);
            // Return the actual HRESULT (matching C# where the exception
            // propagates to StartAsync and is returned as ex.HResult).
            return com.kr.launcher.util.ExceptionUtils.getHResult(e);
        }
    }

    /**
     * Read count ulong values from shared memory starting at byte offset.
     * Corresponds to C# KRSharedMemory.ReadUlong(int offset, int count, out ulong[]
     * data, TimeSpan? timeout).
     *
     * @param offset    byte offset
     * @param count     number of ulongs to read
     * @param timeoutMs timeout in milliseconds, -1 = INFINITE, 0 = non-blocking
     * @return array of ulong values, or null on failure (lock timeout or error)
     */
    public long[] readUlong(int offset, int count, long timeoutMs) {
        long[] data = new long[count];
        if (!acquireMutex(timeoutMs)) {
            log.error("Failed to lock shared memory");
            return null;
        }
        try {
            for (int i = 0; i < count; i++) {
                // Read 8 bytes as little-endian long (Windows ulong is 8 bytes)
                data[i] = mappedView.getLong(offset + i * 8L);
            }
            return data;
        } catch (Exception e) {
            log.error("Failed to read data from shared memory", e);
            return null;
        } finally {
            releaseMutex();
        }
    }

    /**
     * Read a single ulong value at byte offset.
     * Corresponds to C# KRSharedMemory.ReadUlong(int offset, out ulong data,
     * TimeSpan? timeout).
     */
    public long readUlong(int offset, long timeoutMs) {
        if (!acquireMutex(timeoutMs)) {
            log.error("Failed to lock shared memory");
            return -1;
        }
        try {
            return mappedView.getLong(offset);
        } catch (Exception e) {
            log.error("Failed to read data from shared memory", e);
            return -1;
        } finally {
            releaseMutex();
        }
    }

    /**
     * Write a single ulong value at byte offset.
     */
    public boolean writeUlong(int offset, long value) {
        if (!acquireMutex(0)) {
            return false;
        }
        try {
            mappedView.setLong(offset, value);
            return true;
        } catch (Exception e) {
            log.error("Failed to write to shared memory", e);
            return false;
        } finally {
            releaseMutex();
        }
    }

    /**
     * Acquire the named mutex.
     * 
     * @param timeoutMs -1 = INFINITE, 0 = non-blocking, >0 = wait up to timeoutMs
     * @return true if acquired, false on timeout or error
     */
    private boolean acquireMutex(long timeoutMs) {
        if (mutexHandle == null) {
            return false;
        }
        int waitMs;
        if (timeoutMs < 0) {
            waitMs = INFINITE;
        } else if (timeoutMs > Integer.MAX_VALUE) {
            waitMs = Integer.MAX_VALUE;
        } else {
            waitMs = (int) timeoutMs;
        }
        try {
            int result = K32.WaitForSingleObject(mutexHandle, waitMs);
            if (result == WAIT_OBJECT_0) {
                return true;
            }
            if (result == WAIT_TIMEOUT) {
                log.error("Mutex wait timed out");
            } else {
                log.error("Mutex wait failed, result: {}", result);
            }
            return false;
        } catch (Exception e) {
            log.error("Mutex wait exception", e);
            return false;
        }
    }

    private void releaseMutex() {
        if (mutexHandle != null) {
            try {
                K32.ReleaseMutex(mutexHandle);
            } catch (Exception e) {
                log.warn("Failed to release mutex", e);
            }
        }
    }

    @Override
    public void close() {
        try {
            if (mappedView != null) {
                K32.UnmapViewOfFile(mappedView);
                mappedView = null;
            }
            if (fileMappingHandle != null) {
                K32.CloseHandle(fileMappingHandle);
                fileMappingHandle = null;
            }
            if (mutexHandle != null) {
                K32.CloseHandle(mutexHandle);
                mutexHandle = null;
            }
        } catch (Exception e) {
            log.warn("Error closing shared memory", e);
        }
    }
}
