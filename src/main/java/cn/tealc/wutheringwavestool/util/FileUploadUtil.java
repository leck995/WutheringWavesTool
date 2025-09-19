package cn.tealc.wutheringwavestool.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import cn.tealc.wutheringwavestool.model.CardInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 文件上传下载工具类，用于同步抽卡记录到云端
 */
public class FileUploadUtil {
    private static final Logger LOG = LoggerFactory.getLogger(FileUploadUtil.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 智能同步抽卡记录
     * 根据本地和云端数据情况自动选择上传、下载或合并操作
     *
     * @param uid 用户UID
     * @return 同步结果描述
     */
    public static CompletableFuture<String> syncCardPoolData(String uid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean hasLocalData = hasLocalCardPoolData(uid);
                String cloudFileUrl = checkCloudCardPoolData(uid);
                boolean hasCloudData = cloudFileUrl != null;

                LOG.info("Sync check for uid {}: local={}, cloud={}", uid, hasLocalData, hasCloudData);

                if (hasLocalData && !hasCloudData) {
                    // 只有本地数据，上传到云端
                    String uploadUrl = uploadCardPoolDataInternal(uid);
                    return uploadUrl != null ? "已上传本地数据到云端: " + uploadUrl : "上传失败";

                } else if (!hasLocalData && hasCloudData) {
                    // 只有云端数据，下载到本地
                    boolean downloadSuccess = downloadCardPoolData(uid, cloudFileUrl);
                    return downloadSuccess ? "已从云端恢复数据" : "下载失败";

                } else if (hasLocalData && hasCloudData) {
                    // 都有数据，比较并同步最新的
                    return syncBothExist(uid, cloudFileUrl);

                } else {
                    // 都没有数据
                    return "本地和云端都没有数据，请先获取抽卡记录";
                }

            } catch (Exception e) {
                LOG.error("Sync failed for uid: {}", uid, e);
                return "同步失败: " + e.getMessage();
            }
        });
    }

    /**
     * 上传抽卡记录文件到图床
     *
     * @param uid 用户UID
     * @return 上传后的文件URL，失败返回null
     */
    public static CompletableFuture<String> uploadCardPoolData(String uid) {
        return CompletableFuture.supplyAsync(() -> uploadCardPoolDataInternal(uid));
    }

    /**
     * 检查本地是否有抽卡记录数据
     */
    private static boolean hasLocalCardPoolData(String uid) {
        File dataDir = new File("data/" + uid);
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            return false;
        }

        File dataJson = new File(dataDir, "data.json");
        File poolJson = new File(dataDir, "pool.json");

        return dataJson.exists() && poolJson.exists() &&
               dataJson.length() > 0 && poolJson.length() > 0;
    }

    /**
     * 检查云端是否有抽卡记录数据
     * @return 如果存在返回最新文件的URL，否则返回null
     */
    private static String checkCloudCardPoolData(String uid) {
        try {
            EnvConfig.load();
            String baseUrl = EnvConfig.getApiUrl().replace("/upload", "");
            String folderPath = "/file/" + EnvConfig.getUploadFolder() + "/" + uid + "/";

            // 这里需要实现文件列表API，或者尝试访问已知的文件名模式
            // 由于图床可能没有提供文件列表API，我们采用另一种方案：
            // 在上传时记录文件URL到本地配置，然后检查这些URL是否still available

            return checkKnownCloudFiles(uid, baseUrl, folderPath);

        } catch (Exception e) {
            LOG.error("Failed to check cloud data for uid: {}", uid, e);
            return null;
        }
    }

    /**
     * 检查已知的云端文件是否存在
     */
    private static String checkKnownCloudFiles(String uid, String baseUrl, String folderPath) {
        // 这里可以实现检查机制，比如：
        // 1. 从本地配置文件读取之前上传的URLs
        // 2. 或者使用固定的文件名模式
        // 由于当前图床API限制，我们暂时返回null，表示无法检测
        // 在实际使用中，可以通过其他方式（如数据库记录）来追踪上传的文件

        LOG.warn("Cloud file detection not fully implemented due to API limitations");
        return null;
    }

    /**
     * 从云端下载抽卡记录数据
     */
    private static boolean downloadCardPoolData(String uid, String cloudFileUrl) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cloudFileUrl))
                    .timeout(Duration.ofMillis(EnvConfig.getTimeout()))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                // 解压ZIP文件到本地目录
                return extractZipToUserData(uid, response.body());
            } else {
                LOG.error("Failed to download cloud data: status {}", response.statusCode());
                return false;
            }

        } catch (Exception e) {
            LOG.error("Download failed for uid: {}", uid, e);
            return false;
        }
    }

    /**
     * 解压ZIP文件到用户数据目录
     */
    private static boolean extractZipToUserData(String uid, byte[] zipData) {
        File userDataDir = new File("data/" + uid);
        userDataDir.mkdirs();

        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    File outputFile = new File(userDataDir, entry.getName());

                    // 安全检查：确保文件在目标目录内
                    if (!outputFile.getCanonicalPath().startsWith(userDataDir.getCanonicalPath())) {
                        LOG.warn("Unsafe zip entry: {}", entry.getName());
                        continue;
                    }

                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                    LOG.info("Extracted: {}", outputFile.getName());
                }
                zis.closeEntry();
            }
            return true;

        } catch (Exception e) {
            LOG.error("Failed to extract zip for uid: {}", uid, e);
            return false;
        }
    }

    /**
     * 处理本地和云端都有数据的情况
     * 永远执行安全的合并策略，不依赖时间戳比较
     */
    private static String syncBothExist(String uid, String cloudFileUrl) {
        try {
            // 下载云端数据并与本地数据合并
            boolean mergeSuccess = downloadAndMergeData(uid, cloudFileUrl);
            if (!mergeSuccess) {
                return "合并数据失败";
            }

            // 上传合并后的完整数据，替换云端备份
            String uploadUrl = uploadCardPoolDataInternal(uid);
            return uploadUrl != null ? "已同步并更新云端数据: " + uploadUrl : "上传合并数据失败";

        } catch (Exception e) {
            LOG.error("Failed to sync both exist for uid: {}", uid, e);
            return "同步失败: " + e.getMessage();
        }
    }

    /**
     * 下载云端数据并与本地数据合并
     * 使用现有的update()方法进行安全合并
     */
    private static boolean downloadAndMergeData(String uid, String cloudFileUrl) {
        try {
            // 1. 备份本地数据
            File localDataDir = new File("data/" + uid);
            File backupDir = new File("data/" + uid + "_backup_" + System.currentTimeMillis());
            if (localDataDir.exists()) {
                copyDirectory(localDataDir, backupDir);
                LOG.info("Created backup for uid {}: {}", uid, backupDir.getName());
            }

            // 2. 下载云端数据到临时目录
            File tempDir = new File("data/" + uid + "_temp_" + System.currentTimeMillis());
            tempDir.mkdirs();

            boolean downloadSuccess = downloadToDirectory(cloudFileUrl, tempDir);
            if (!downloadSuccess) {
                LOG.error("Failed to download cloud data for uid: {}", uid);
                cleanupDirectory(tempDir);
                return false;
            }

            // 3. 读取本地和云端数据
            Map<String, List<CardInfo>> localData = readCardPoolData(localDataDir);
            Map<String, List<CardInfo>> cloudData = readCardPoolData(tempDir);

            // 4. 使用现有的update方法合并数据
            Map<String, List<CardInfo>> mergedData = mergeCardPoolData(localData, cloudData);

            // 5. 保存合并后的数据
            boolean saveSuccess = saveCardPoolData(uid, mergedData);

            // 6. 清理临时目录
            cleanupDirectory(tempDir);

            if (saveSuccess) {
                // 清理备份（可选，根据需要保留）
                cleanupDirectory(backupDir);
                LOG.info("Successfully merged and saved data for uid: {}", uid);
                return true;
            } else {
                // 恢复备份
                if (backupDir.exists()) {
                    cleanupDirectory(localDataDir);
                    copyDirectory(backupDir, localDataDir);
                    LOG.info("Restored backup for uid: {}", uid);
                }
                return false;
            }

        } catch (Exception e) {
            LOG.error("Failed to download and merge data for uid: {}", uid, e);
            return false;
        }
    }

    /**
     * 下载云端文件到指定目录
     */
    private static boolean downloadToDirectory(String cloudFileUrl, File targetDir) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cloudFileUrl))
                    .timeout(Duration.ofMillis(EnvConfig.getTimeout()))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                return extractZipToDirectory(targetDir, response.body());
            } else {
                LOG.error("Failed to download cloud data: status {}", response.statusCode());
                return false;
            }

        } catch (Exception e) {
            LOG.error("Download failed", e);
            return false;
        }
    }

    /**
     * 解压ZIP文件到指定目录
     */
    private static boolean extractZipToDirectory(File targetDir, byte[] zipData) {
        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    File outputFile = new File(targetDir, entry.getName());

                    // 安全检查：确保文件在目标目录内
                    if (!outputFile.getCanonicalPath().startsWith(targetDir.getCanonicalPath())) {
                        LOG.warn("Unsafe zip entry: {}", entry.getName());
                        continue;
                    }

                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
                zis.closeEntry();
            }
            return true;

        } catch (Exception e) {
            LOG.error("Failed to extract zip", e);
            return false;
        }
    }

    /**
     * 读取抽卡池数据
     */
    private static Map<String, List<CardInfo>> readCardPoolData(File dataDir) {
        Map<String, List<CardInfo>> result = new LinkedHashMap<>();
        if (dataDir == null || !dataDir.exists()) {
            return result;
        }

        File poolJson = new File(dataDir, "pool.json");
        if (poolJson.exists()) {
            try {
                result = objectMapper.readValue(poolJson, new TypeReference<Map<String, List<CardInfo>>>() {});
            } catch (IOException e) {
                LOG.error("Failed to read pool data from: {}", poolJson.getAbsolutePath(), e);
            }
        }
        return result;
    }

    /**
     * 合并两个抽卡池数据
     * 使用与CardPoolRequestTask相同的合并逻辑
     */
    private static Map<String, List<CardInfo>> mergeCardPoolData(Map<String, List<CardInfo>> localData, Map<String, List<CardInfo>> cloudData) {
        Map<String, List<CardInfo>> mergedData = new LinkedHashMap<>(localData);

        for (Map.Entry<String, List<CardInfo>> entry : cloudData.entrySet()) {
            String poolName = entry.getKey();
            List<CardInfo> cloudList = entry.getValue();

            List<CardInfo> localList = mergedData.get(poolName);
            if (localList != null && !localList.isEmpty()) {
                // 使用相同的update逻辑合并
                List<CardInfo> merged = updateCardInfoList(localList, cloudList);
                mergedData.put(poolName, merged);
            } else {
                // 本地没有该池数据，直接使用云端数据
                mergedData.put(poolName, new ArrayList<>(cloudList));
            }
        }

        return mergedData;
    }

    /**
     * 与CardPoolRequestTask中的update方法相同的合并逻辑
     * 将本地旧数据与云端数据进行整合
     */
    private static List<CardInfo> updateCardInfoList(List<CardInfo> oldData, List<CardInfo> newData) {
        if (newData.isEmpty()) {
            return oldData;
        } else if (oldData.isEmpty()) {
            return newData;
        }

        CardInfo last = newData.get(newData.size() - 1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime newTime = LocalDateTime.parse(last.getTime(), formatter);

        List<CardInfo> list = new ArrayList<>(newData);
        for (int i = 0; i < oldData.size(); i++) {
            CardInfo first = oldData.get(i);
            LocalDateTime oldTime = LocalDateTime.parse(first.getTime(), formatter);
            if (oldTime.isBefore(newTime)) {
                list.addAll(oldData.subList(i, oldData.size()));
                break;
            } else if (oldTime.isEqual(newTime)) { // 当存在相等时间时，找到同一时间最早的数据
                for (int j = i; j < oldData.size(); j++) {
                    CardInfo first2 = oldData.get(j);
                    LocalDateTime oldTime2 = LocalDateTime.parse(first2.getTime(), formatter);
                    if (oldTime2.isBefore(newTime)) {
                        list.addAll(oldData.subList(j, oldData.size()));
                        break;
                    }
                }
                break;
            }
        }
        return list;
    }

    /**
     * 保存抽卡池数据
     */
    private static boolean saveCardPoolData(String uid, Map<String, List<CardInfo>> data) {
        try {
            File userDataDir = new File("data/" + uid);
            userDataDir.mkdirs();

            File poolJson = new File(userDataDir, "pool.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(poolJson, data);

            LOG.info("Saved merged card pool data for uid: {}", uid);
            return true;

        } catch (IOException e) {
            LOG.error("Failed to save card pool data for uid: {}", uid, e);
            return false;
        }
    }

    /**
     * 复制目录
     */
    private static void copyDirectory(File source, File target) throws IOException {
        if (source.isDirectory()) {
            target.mkdirs();
            File[] files = source.listFiles();
            if (files != null) {
                for (File file : files) {
                    copyDirectory(file, new File(target, file.getName()));
                }
            }
        } else {
            Files.copy(source.toPath(), target.toPath());
        }
    }

    /**
     * 清理目录
     */
    private static void cleanupDirectory(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        cleanupDirectory(file);
                    }
                    file.delete();
                }
            }
            dir.delete();
        }
    }

    /**
     * 内部上传方法
     */
    private static String uploadCardPoolDataInternal(String uid) {
        try {
            // 创建临时压缩文件
            File tempZipFile = createCardPoolZip(uid);
            if (tempZipFile == null) {
                LOG.error("Failed to create zip file for uid: {}", uid);
                return null;
            }

            // 上传文件
            String uploadUrl = uploadFile(tempZipFile, uid);

            // 清理临时文件
            tempZipFile.delete();

            return uploadUrl;
        } catch (Exception e) {
            LOG.error("Failed to upload card pool data for uid: {}", uid, e);
            return null;
        }
    }
    private static File createCardPoolZip(String uid) {
        try {
            File dataDir = new File("data/" + uid);
            if (!dataDir.exists() || !dataDir.isDirectory()) {
                LOG.warn("Data directory not found for uid: {}", uid);
                return null;
            }

            // 创建临时zip文件
            File tempZipFile = File.createTempFile("cardpool_" + uid + "_", ".zip");

            // 使用Java的zip功能压缩文件
            try (var fos = new java.io.FileOutputStream(tempZipFile);
                 var zos = new java.util.zip.ZipOutputStream(fos)) {

                File[] files = dataDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            var entry = new java.util.zip.ZipEntry(file.getName());
                            zos.putNextEntry(entry);
                            Files.copy(file.toPath(), zos);
                            zos.closeEntry();
                        }
                    }
                }
            }

            LOG.info("Created zip file for uid {}: {}", uid, tempZipFile.getName());
            return tempZipFile;

        } catch (IOException e) {
            LOG.error("Failed to create zip file for uid: {}", uid, e);
            return null;
        }
    }

    /**
     * 上传文件到图床
     */
    private static String uploadFile(File file, String uid) throws IOException, InterruptedException {
        EnvConfig.load();

        String apiUrl = EnvConfig.getApiUrl();
        String authCode = EnvConfig.getAuthCode();
        String baseFolder = EnvConfig.getUploadFolder();
        int timeout = EnvConfig.getTimeout();

        if (apiUrl == null || authCode == null) {
            throw new IllegalStateException("Missing required environment configuration");
        }

        // 构建上传路径: baseFolder/uid/
        String uploadFolder = baseFolder + "/" + uid;
        String uploadUrl = apiUrl + "?authCode=" + authCode + "&uploadFolder=" + uploadFolder;

        // 读取文件内容
        byte[] fileContent = Files.readAllBytes(file.toPath());

        // 构建multipart/form-data请求
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        StringBuilder requestBody = new StringBuilder();

        requestBody.append("--").append(boundary).append("\r\n");
        requestBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                   .append(file.getName()).append("\"\r\n");
        requestBody.append("Content-Type: application/zip\r\n\r\n");

        // 创建完整的请求体
        byte[] prefix = requestBody.toString().getBytes();
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes();

        byte[] fullRequestBody = new byte[prefix.length + fileContent.length + suffix.length];
        System.arraycopy(prefix, 0, fullRequestBody, 0, prefix.length);
        System.arraycopy(fileContent, 0, fullRequestBody, prefix.length, fileContent.length);
        System.arraycopy(suffix, 0, fullRequestBody, prefix.length + fileContent.length, suffix.length);

        // 发送HTTP请求
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(Duration.ofMillis(timeout))
                .POST(HttpRequest.BodyPublishers.ofByteArray(fullRequestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // 解析响应
            JsonNode jsonResponse = objectMapper.readTree(response.body());
            if (jsonResponse.isArray() && jsonResponse.size() > 0) {
                JsonNode firstItem = jsonResponse.get(0);
                if (firstItem.has("src")) {
                    String relativePath = firstItem.get("src").asText();
                    // 构建完整URL (假设使用相同的域名)
                    String baseUrl = apiUrl.substring(0, apiUrl.lastIndexOf("/"));
                    String fullUrl = baseUrl.replace("/upload", "") + relativePath;
                    LOG.info("File uploaded successfully for uid {}: {}", uid, fullUrl);
                    return fullUrl;
                }
            }
        }

        LOG.error("Upload failed for uid {}. Status: {}, Response: {}", uid, response.statusCode(), response.body());
        return null;
    }
}