package cn.tealc.wwt.game.resource.internal.legacy.util;

import cn.tealc.wwt.game.resource.internal.legacy.model.UpdateResult;

import java.io.*;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;

/**
 * Maps Java exceptions to C# HRESULT values.
 *
 * C# code uses `catch (Exception e) { ... e.HResult ... }` to obtain the
 * runtime HRESULT for error reporting. Java's {@code e.hashCode()} is unrelated
 * and produces meaningless values, so this utility restores the C# semantics.
 *
 * Strategy: check Java exception TYPES first (locale-independent, mirroring C#
 * type-based HRESULT dispatch), then fall back to message matching for cases
 * where Java has no dedicated type (disk full, share violation on non-English
 * Windows). Message matching is a best-effort heuristic and may fail on
 * non-English locales — this is an inherent limitation of Java vs C#'s native
 * HRESULT access.
 *
 * HRESULT values match {@link UpdateResult} constants and the mappings enforced
 * throughout the C# launcher (FileUtils.Move, KRDownloadCall, etc.):
 *
 * <ul>
 * <li>{@link NoSuchFileException} / {@link java.io.FileNotFoundException}
 * &rarr; {@code ERROR_CODE_FILE_MISSING} (-2147024894)</li>
 * <li>{@link AccessDeniedException} / AccessControlException (resolved via
 * reflection — see {@link #ACCESS_CONTROL_EXCEPTION_CLASS})
 * &rarr; {@code ERROR_CODE_FILE_PERMISSION_DENY} (-2147024891)</li>
 * <li>{@link OverlappingFileLockException} / file lock / sharing violation
 * &rarr; {@code ERROR_CODE_FILE_OCCUPANCY} (-2147024864)</li>
 * <li>Disk full / no space (message-based)
 * &rarr; {@code ERROR_CODE_DISK_NOT_ENOUGH_SPACE} (-2147024784)</li>
 * <li>Any other {@link IOException}
 * &rarr; {@code COR_E_IOEXCEPTION} (-2146232800)</li>
 * <li>Any other {@link Exception}
 * &rarr; {@code COR_E_EXCEPTION} (-2146233088)</li>
 * </ul>
 */
public final class ExceptionUtils {

    /** .NET COR_E_IOEXCEPTION = 0x80131620. */
    public static final int COR_E_IOEXCEPTION = -2146232800;

    /** .NET COR_E_EXCEPTION = 0x80131500. */
    public static final int COR_E_EXCEPTION = -2146233088;

    /**
     * Lazily resolved {@code java.security.AccessControlException} class.
     *
     * <p>
     * {@code AccessControlException} is deprecated for removal in Java 17+
     * (along with {@link SecurityManager}). Resolving it via reflection avoids a
     * compile-time dependency: on JVMs where the class still exists, the check
     * works as a normal {@code instanceof}; on JVMs where it has been removed,
     * {@link ClassNotFoundException} leaves this field {@code null} and the
     * permission check silently falls through to the {@link AccessDeniedException}
     * and message-based paths. The lookup runs once at class-init.
     */
    private static final Class<?> ACCESS_CONTROL_EXCEPTION_CLASS = loadAccessControlExceptionClass();

    private static Class<?> loadAccessControlExceptionClass() {
        try {
            return Class.forName("java.security.AccessControlException");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private ExceptionUtils() {
    }

    /**
     * Map a Throwable to its C# HRESULT equivalent.
     *
     * The input is {@code Throwable} so callers can pass any caught exception
     * without narrowing first; non-{@link Exception} throwables (e.g.
     * {@link Error}) fall through to the generic COR_E_EXCEPTION value.
     *
     * Type-based checks are performed BEFORE message-based checks to maximize
     * locale independence, mirroring how C# dispatches on the strongly-typed
     * exception hierarchy before falling back to the native HRESULT.
     */
    public static int getHResult(Throwable t) {
        if (t == null) {
            return 0;
        }

        // Unwrap checked-exception wrappers (e.g., UncheckedIOException from FileUtils)
        // so type-based checks can inspect the underlying IOException.
        if (t instanceof UncheckedIOException && t.getCause() != null) {
            return getHResult(t.getCause());
        }

        // === Type-based checks (locale-independent) ===

        // File-not-found variants: NoSuchFileException covers both file and
        // directory "not found" in java.nio.file; FileNotFoundException is the
        // java.io equivalent.
        if (t instanceof NoSuchFileException || t instanceof java.io.FileNotFoundException) {
            return UpdateResult.ERROR_CODE_FILE_MISSING;
        }

        // Permission / access denied variants. AccessDeniedException covers the
        // java.nio.file path; AccessControlException (thrown by SecurityManager)
        // is checked via the reflection-resolved class so the code remains
        // compilable after the class is removed from the JDK.
        if (t instanceof AccessDeniedException
                || (ACCESS_CONTROL_EXCEPTION_CLASS != null && ACCESS_CONTROL_EXCEPTION_CLASS.isInstance(t))) {
            return UpdateResult.ERROR_CODE_FILE_PERMISSION_DENY;
        }

        // File lock / occupancy: OverlappingFileLockException is thrown when a
        // region of a file is already locked by the same JVM — locale-independent
        // signal of file occupancy. Corresponds to C# detecting lock via HResult.
        if (t instanceof OverlappingFileLockException) {
            return UpdateResult.ERROR_CODE_FILE_OCCUPANCY;
        }

        // === Message-based checks (locale-dependent fallback) ===
        // Java has no dedicated types for disk-full or share-violation, so we
        // inspect the message. This is inherently weaker than C#'s ex.HResult
        // but covers the common English-Windows case.
        String msg = t.getMessage() != null ? t.getMessage().toLowerCase() : "";

        // Disk full detection.
        if (msg.contains("no space") || msg.contains("disk full")
                || msg.contains("not enough space") || msg.contains("insufficient disk space")
                || msg.contains("espacio insuficiente") || msg.contains("磁盘空间不足")) {
            return UpdateResult.ERROR_CODE_DISK_NOT_ENOUGH_SPACE;
        }

        // Lock / sharing / occupancy detection (message-based for non-typed cases).
        if (msg.contains("being used by another process")
                || msg.contains("sharing violation")
                || msg.contains("lock")
                || msg.contains("occupied")
                || msg.contains("being used by another program")
                || msg.contains("被另一进程使用") || msg.contains("被其他程序使用")) {
            return UpdateResult.ERROR_CODE_FILE_OCCUPANCY;
        }

        // FileSystemException may carry a permission-related reason.
        if (t instanceof FileSystemException) {
            if (msg.contains("permission") || msg.contains("access") || msg.contains("denied")) {
                return UpdateResult.ERROR_CODE_FILE_PERMISSION_DENY;
            }
        }

        // === Generic fallbacks ===
        if (t instanceof IOException) {
            return COR_E_IOEXCEPTION;
        }
        return COR_E_EXCEPTION;
    }
}
