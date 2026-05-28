package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.util.DecodeUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kuro.launcher.model.LocalCacheUser;
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

    public Optional<List<LocalCacheUser>> readLocalCacheUser() {
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            System.err.println("错误: 无法找到 APPDATA 环境变量。");
            return Optional.empty();
        }
        String path1 = Paths.get(appData, "KR_G152", "A1381", "KRSDKUserLauncherCache.json").toString();
        String path2 = Paths.get(appData, "KR_G153", "A1730", "KRSDKUserLauncherCache.json").toString();
        List<LocalCacheUser> cacheUser1 = getCacheUser(path1, SourceType.DEFAULT);
        List<LocalCacheUser> cacheUser2 = getCacheUser(path2, SourceType.DEFAULT);
        cacheUser1.addAll(cacheUser2);
        if (cacheUser1.isEmpty()){
            return Optional.empty();
        }
        return Optional.of(cacheUser1);
    }


    private List<LocalCacheUser> getCacheUser(String path,SourceType type){
        File file = new File(path);
        if (!file.exists()) {
            LOG.info("错误: 文件未找到: {}", path);
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
