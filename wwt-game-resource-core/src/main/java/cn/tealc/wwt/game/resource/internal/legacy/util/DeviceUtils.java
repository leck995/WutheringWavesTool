package cn.tealc.wwt.game.resource.internal.legacy.util;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Generates a stable per-machine device id.
 * Corresponds to KRUtils/DeviceUtils.cs GetKRDeviceId().
 *
 * Strategy (matches C#):
 * 1. Try WMI Win32_ComputerSystemProduct.UUID (via wmic CLI).
 * 2. If the UUID is in the predefined invalid set, prefix with "uuid-" + soft id.
 * 3. If WMI fails entirely, fall back to soft id (random GUID cached in file).
 * 4. As an additional fallback, try MachineGuid from Windows registry.
 */
public final class DeviceUtils {
    private static final Logger log = LoggerFactory.getLogger(DeviceUtils.class);

    private static final String CACHE_FILE = "kr_starter_cached.json";
    private static volatile String cachedDeviceId = null;

    /** Matches C# s_predefineInvalidUuids. */
    private static final Set<String> PREDEFINE_INVALID_UUIDS = new HashSet<>();
    static {
        String[] invalid = {
            "03000200-0400-0500-0006-000700080009",
            "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF",
            "FEFEFEFE-FEFE-FEFE-FEFE-FEFEFEFEFEFE",
            "41564E49-494C-0044-0000-000000000000",
            "12345678-1234-5678-90AB-CDDEEFAABBCC",
            "E53E3B78-02B0-11E6-235E-F812B50DC619",
            "F0960000-0000-1000-8000-000000000000",
            "F0960000-0000-1000-8000-888888888788",
            "00000000-0000-0000-0000-000000000000",
            "2E4F2079-2E45-0000-0000-000000000000",
            "FFFFFFFF-FFFF-0000-0000-000000000000",
            "B139AB2A-30C0-11E8-92F3-B2BCAE914800",
            "03880288-0488-0588-8706-880700080009",
            "D8A8A887-E5DE-0000-0000-000000000000"
        };
        for (String s : invalid) {
            PREDEFINE_INVALID_UUIDS.add(s.toUpperCase());
        }
    }

    private DeviceUtils() {
    }

    /**
     * Get a stable device id, matching C# GetKRDeviceId() logic.
     * @param cacheDir directory to store the cached soft id; if null, uses java.io.tmpdir
     * @return non-null device id string (may be empty on failure)
     */
    public static String getKRDeviceId(String cacheDir) {
        if (cachedDeviceId != null) {
            return cachedDeviceId;
        }
        synchronized (DeviceUtils.class) {
            if (cachedDeviceId != null) {
                return cachedDeviceId;
            }

            // Step 1: Try WMI UUID (matches C# GetDeviceId).
            String wmiUuid = getWmiDeviceId();
            if (wmiUuid != null && !wmiUuid.isEmpty()) {
                if (!PREDEFINE_INVALID_UUIDS.contains(wmiUuid.toUpperCase())) {
                    cachedDeviceId = wmiUuid;
                    return cachedDeviceId;
                }
                // Invalid UUID: use soft id with "uuid-" prefix (matches C# line 112).
                String softId = getSoftDeviceId(cacheDir);
                cachedDeviceId = (softId != null && !softId.isEmpty())
                        ? "uuid-" + softId
                        : wmiUuid;
                return cachedDeviceId;
            }

            // Step 2: Fall back to soft device id (matches C# GetSoftDeviceId).
            String softId = getSoftDeviceId(cacheDir);
            if (softId != null && !softId.isEmpty()) {
                cachedDeviceId = softId;
                return cachedDeviceId;
            }

            // Step 3: Last resort — MachineGuid from registry.
            String machineGuid = getMachineGuid();
            cachedDeviceId = (machineGuid != null) ? machineGuid : "";
            return cachedDeviceId;
        }
    }

    /**
     * Get device id using the system temp dir as cache location.
     */
    public static String getKRDeviceId() {
        return getKRDeviceId(null);
    }

    /**
     * Query WMI for Win32_ComputerSystemProduct.UUID.
     * Corresponds to C# GetDeviceId() which uses ManagementObjectSearcher.
     * Uses `wmic csproduct get UUID` CLI since Java cannot directly access WMI.
     */
    private static String getWmiDeviceId() {
        try {
            ProcessBuilder pb = new ProcessBuilder("wmic", "csproduct", "get", "UUID");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("wmic csproduct get UUID timed out");
                return null;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // Skip header line "UUID" and empty lines
                    if (line.isEmpty() || line.equalsIgnoreCase("UUID")) {
                        continue;
                    }
                    // Found the UUID value
                    return line;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get WMI device id: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get or create a soft device id (random GUID cached in file).
     * Corresponds to C# GetSoftDeviceId().
     */
    private static String getSoftDeviceId(String cacheDir) {
        String dir = (cacheDir != null && !cacheDir.isEmpty())
                ? cacheDir
                : System.getProperty("java.io.tmpdir");
        try {
            Files.createDirectories(Path.of(dir));
        } catch (IOException e) {
            log.warn("Failed to create device id cache dir: {}", dir, e);
        }
        String cachePath = PathUtils.combine(dir, CACHE_FILE);

        // Try to read existing.
        try {
            if (Files.exists(Path.of(cachePath))) {
                String json = FileUtils.read(cachePath);
                if (json != null && !json.isEmpty()) {
                    int idx = json.indexOf("\"did\"");
                    if (idx >= 0) {
                        int colon = json.indexOf(':', idx);
                        int start = json.indexOf('"', colon + 1);
                        int end = json.indexOf('"', start + 1);
                        if (start >= 0 && end > start) {
                            String existing = json.substring(start + 1, end);
                            if (!existing.isEmpty()) {
                                return existing;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read cached device id: {}", e.getMessage());
        }

        // Generate new.
        String newId = UUID.randomUUID().toString();
        try {
            String json = "{\"did\":\"" + newId + "\"}";
            FileUtils.write(json, cachePath);
        } catch (Exception e) {
            log.warn("Failed to persist device id: {}", e.getMessage());
        }
        return newId;
    }

    /**
     * Read MachineGuid from Windows registry.
     * Corresponds to C# GetMachineGuid().
     * Path: HKLM\SOFTWARE\Microsoft\Cryptography\MachineGuid
     */
    public static String getMachineGuid() {
        try {
            String keyPath = "SOFTWARE\\Microsoft\\Cryptography";
            if (Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, keyPath)) {
                return Advapi32Util.registryGetStringValue(
                        WinReg.HKEY_LOCAL_MACHINE, keyPath, "MachineGuid");
            }
        } catch (Exception e) {
            log.warn("Failed to read MachineGuid from registry: {}", e.getMessage());
        }
        return null;
    }
}
