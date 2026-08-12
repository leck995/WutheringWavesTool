package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.util.DecodeUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kuro.launcher.model.LocalCacheUser;
import com.kuro.model.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 用于读取官方启动器中用户的服务
 *
 * @author leck
 * @date 2026/05/27
 */
@Singleton
public class LauncherUserService {
    private static final Logger LOG = LoggerFactory.getLogger(LauncherUserService.class);
    private ObjectMapper objectMapper;
    @Inject
    public LauncherUserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 读取本地启动器缓存的所有用户数据
     * 扫描 KR_G152（国服）和 KR_G153（国际服）下所有二级目录中的 KRSDKUserLauncherCache.json
     */
    public Optional<List<LocalCacheUser>> readLocalCacheUser() {
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            System.err.println("错误: 无法找到 APPDATA 环境变量。");
            return Optional.empty();
        }
        List<LocalCacheUser> allUsers = new ArrayList<>();
        // KR_G152 目录下的用户标记为国服
        allUsers.addAll(readFromParentDir(Paths.get(appData, "KR_G152").toString(), SourceType.DEFAULT));
        // KR_G153 目录下的用户标记为国际服
        allUsers.addAll(readFromParentDir(Paths.get(appData, "KR_G153").toString(), SourceType.GLOBAL));
        if (allUsers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(allUsers);
    }

    /**
     * 遍历父目录下的所有一级子目录，读取每个子目录中的 KRSDKUserLauncherCache.json
     *
     * @param parentDir 父目录路径（如 KR_G152 或 KR_G153）
     * @param type      用户来源类型，国服为 DEFAULT，国际服为 GLOBAL
     */
    private List<LocalCacheUser> readFromParentDir(String parentDir, SourceType type) {
        List<LocalCacheUser> allUsers = new ArrayList<>();
        File dir = new File(parentDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return allUsers;
        }
        File[] subDirs = dir.listFiles(File::isDirectory);
        if (subDirs == null) {
            return allUsers;
        }
        for (File subDir : subDirs) {
            String path = Paths.get(subDir.getAbsolutePath(), "KRSDKUserLauncherCache.json").toString();
            allUsers.addAll(getCacheUser(path, type));
        }
        return allUsers;
    }


    private List<LocalCacheUser> getCacheUser(String path,SourceType type){
        File file = new File(path);
        if (!file.exists()) {
            LOG.info("读取本地用户数据失败: 文件未找到: {}", path);
            return new ArrayList<>();
        }
        if (file.length() == 0) {
            LOG.debug("KRSDKUserLauncherCache 文件为空: {}", path);
            return new ArrayList<>();
        }
        try {
            List<LocalCacheUser> localCacheUsers = objectMapper.readValue(file, new TypeReference<List<LocalCacheUser>>() {
            });
            localCacheUsers.forEach(user -> {
                user.setOauthCode(DecodeUtil.decodeXor5(user.getOauthCode()));
                user.setType(type);
            });
            LOG.debug("从 KRSDKUserLauncherCache 读取到 {} 个用户数据", localCacheUsers.size());
            return localCacheUsers;
        } catch (IOException e) {
            LOG.debug("从 KRSDKUserLauncherCache 读取数据失败", e);
            return new ArrayList<>();
        }
    }
}
