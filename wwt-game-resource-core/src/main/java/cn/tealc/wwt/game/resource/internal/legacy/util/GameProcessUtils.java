package cn.tealc.wwt.game.resource.internal.legacy.util;

import cn.tealc.wwt.game.resource.internal.legacy.config.ResourceConfigManager;
import cn.tealc.wwt.game.resource.internal.legacy.model.GameServerConfig;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Kills game processes that may hold locks on files being moved/patched.
 * Corresponds to KRLauncher.KRGameProcess.KRGameProcessUtils.
 *
 * Matches C# behavior: enumerates processes by name, then filters by full
 * executable path (case-insensitive). Only kills processes whose MainModule
 * filename matches the expected game exe path.
 */
public class GameProcessUtils {
    private static final Logger log = LoggerFactory.getLogger(GameProcessUtils.class);

    /** Psapi extension for GetModuleFileNameExW. */
    public interface PsapiExt extends StdCallLibrary {
        PsapiExt INSTANCE = Native.load("psapi", PsapiExt.class, W32APIOptions.DEFAULT_OPTIONS);

        int GetModuleFileNameExW(WinNT.HANDLE hProcess, Pointer hModule, char[] lpFilename, int nSize);
    }

    // Win32 access rights
    private static final int PROCESS_QUERY_INFORMATION = 0x0400;
    private static final int PROCESS_VM_READ = 0x0010;
    private static final int PROCESS_TERMINATE = 0x0001;

    private static final PsapiExt PSAPI = PsapiExt.INSTANCE;

    public static void killProcess(ResourceConfigManager configManager) {
        if (configManager == null)
            return;
        try {
            GameServerConfig serverConfig = configManager.gameServerConfig;
            if (serverConfig == null) {
                log.warn("GameServerConfig Is Empty, Skip Kill Game Process");
                return;
            }
            List<String> keyFileCheckList = serverConfig.keyFileCheckList;
            if (keyFileCheckList == null || keyFileCheckList.isEmpty()) {
                return;
            }
            String gameDirPath = configManager.gameDirPath;
            for (String item : keyFileCheckList) {
                if (item == null)
                    continue;
                if (item.endsWith(".exe")) {
                    log.info("Try Kill Exe Process: {}", item);
                    // C# KRGameProcessUtils.cs:38 uses EndsWith(".exe")
                    // case-sensitively.
                    String fullExePath = PathUtils.combine(gameDirPath, item);
                    fullExePath = PathUtils.normalize(fullExePath);
                    try {
                        killProcessByPath(fullExePath, item);
                    } catch (Exception e) {
                        log.warn("Kill Process Exception, Process: {}, ErrorMessage: {}", item, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("KillProcess outer exception: {}", e.getMessage());
        }
    }

    /**
     * Kill all processes whose executable path matches fullExePath.
     * Matches C# ProcessUtils.KillProcess which uses GetProcessesByName +
     * MainModule.FileName filter.
     *
     * On Windows: uses JNA to enumerate processes (EnumProcesses), get each
     * process's executable path (GetModuleFileNameExW), and terminate matching
     * processes (TerminateProcess).
     */
    private static void killProcessByPath(String fullExePath, String exeName) throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            // Non-Windows fallback: kill by name
            String imageName = new File(exeName).getName();
            int dot = imageName.lastIndexOf('.');
            String baseName = dot > 0 ? imageName.substring(0, dot) : imageName;
            new ProcessBuilder("pkill", "-f", baseName).redirectErrorStream(true).start().waitFor();
            return;
        }

        // Windows: enumerate processes and filter by full path
        String expectedPath = PathUtils.normalize(fullExePath).toLowerCase();
        int[] processIds = new int[1024];
        IntByReference bytesReturned = new IntByReference();
        if (!Psapi.INSTANCE.EnumProcesses(processIds, processIds.length * 4, bytesReturned)) {
            log.warn("EnumProcesses failed");
            return;
        }
        int processCount = bytesReturned.getValue() / 4;

        for (int i = 0; i < processCount; i++) {
            int pid = processIds[i];
            if (pid == 0)
                continue;

            WinNT.HANDLE hProcess = Kernel32.INSTANCE.OpenProcess(
                    PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, false, pid);
            if (hProcess == null)
                continue;

            try {
                char[] filename = new char[260];
                int len = PSAPI.GetModuleFileNameExW(hProcess, null, filename, filename.length);
                if (len > 0) {
                    String exePath = new String(filename, 0, len);
                    exePath = PathUtils.normalize(exePath).toLowerCase();
                    if (exePath.equals(expectedPath)) {
                        log.info("Killing process PID={} path={}", pid, exePath);
                        // Open with terminate access and kill
                        WinNT.HANDLE hTerm = Kernel32.INSTANCE.OpenProcess(PROCESS_TERMINATE, false, pid);
                        if (hTerm != null) {
                            Kernel32.INSTANCE.TerminateProcess(hTerm, 1);
                            Kernel32.INSTANCE.CloseHandle(hTerm);
                        }
                    }
                }
            } finally {
                Kernel32.INSTANCE.CloseHandle(hProcess);
            }
        }
    }
}
