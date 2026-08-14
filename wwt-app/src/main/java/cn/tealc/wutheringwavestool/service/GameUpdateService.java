package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import com.google.inject.Singleton;
import com.kr.launcher.config.KRAppConfLoader;
import com.kr.launcher.config.ResourceConfigManager;
import com.kr.launcher.flow.CheckUpdateFlow;
import com.kr.launcher.flow.ResUpdateModule;
import com.kr.launcher.flow.UpdateFlow;
import com.kr.launcher.model.CheckUpdateResult;
import com.kr.launcher.model.LauncherConfig;
import com.kr.launcher.model.ResStateInfo;
import com.kr.launcher.model.UpdateInfo;
import com.kr.launcher.model.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * 游戏更新 Service：封装官方更新管线（{@link ResUpdateModule}）为可由 ViewModel 调用的门面。
 *
 * <p>负责：从资源抽出 hpatchz.exe / KRApp.conf 到运行目录 → 构造 {@link ResourceConfigManager}
 * → 对外暴露 checkUpdate / update / pause / resume / stop，并在更新完成后把已安装版本写回
 * {@code Config.setting().gameInstalledVersion}。</p>
 */
@Singleton
public class GameUpdateService {
    private static final Logger LOG = LoggerFactory.getLogger(GameUpdateService.class);

    private static final String HPATCHZ = "/kr/hpatchz.exe";
    private static final String KRAPP_CONF = "/kr/KRApp.conf";

    private ResourceConfigManager configManager;
    private ResUpdateModule updateModule;

    /**
     * 初始化更新管线：抽取资源、加载 KRApp.conf、构造 configManager。可重复调用。
     * @throws IllegalStateException 当游戏目录未设置或初始化失败时
     */
    public synchronized void ensureInit() {
        if (configManager != null && updateModule != null) {
            return;
        }
        File gameDir = GameResourcesManager.getGameDir();
        if (gameDir == null) {
            throw new IllegalStateException("游戏目录未设置");
        }
        try {
            extractResources();
            LauncherConfig config = KRAppConfLoader.loadFromKRAppConf("KRApp.conf");
            configManager = new ResourceConfigManager(config, gameDir.getAbsolutePath());
            updateModule = new ResUpdateModule(configManager);
        } catch (Exception e) {
            LOG.error("初始化游戏更新管线失败", e);
            configManager = null;
            updateModule = null;
            throw new IllegalStateException("初始化游戏更新管线失败: " + e.getMessage(), e);
        }
    }

    /** 从资源抽取出 hpatchz.exe 与 KRApp.conf 到运行目录（工作目录）。 */
    private void extractResources() throws IOException {
        extract("/kr/hpatchz.exe", "hpatchz.exe");
        extract("/kr/KRApp.conf", "KRApp.conf");
    }

    private void extract(String resource, String targetName) throws IOException {
        Path target = Path.of(targetName);
        if (Files.exists(target)) {
            return;
        }
        try (InputStream is = getClass().getResourceAsStream(resource)) {
            Objects.requireNonNull(is, "缺失资源: " + resource);
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        }
        LOG.debug("已释放 {} -> {}", resource, target.toAbsolutePath());
    }

    /** 检查更新（同步阻塞）。 */
    public CheckUpdateResult checkUpdate() {
        ensureInit();
        final CheckUpdateResult[] holder = new CheckUpdateResult[1];
        updateModule.checkUpdate(result -> holder[0] = result);
        CheckUpdateResult result = holder[0];
        // 仅当确实已是最新时才把版本同步为“已安装版本”，避免“检查即显示已更新”的误导。
        if (result.succ && result.stateInfo != null
                && result.stateInfo.state == ResStateInfo.STATE_UP_TO_DATE) {
            syncInstalledVersion(result.stateInfo);
        }
        return result;
    }

    /** 启动更新（同步阻塞），进度/完成经回调上报。 */
    public void update(UpdateFlow.ProgressCallback progress, UpdateFlow.CompleteCallback complete) {
        ensureInit();
        updateModule.update(progress, result -> {
            if (result.success) {
                syncInstalledVersionAfterUpdate();
            }
            complete.onComplete(result);
        });
    }

    /** 使用给定检查结果启动更新（真正把状态注入执行，不依赖模块过期缓存）。 */
    public void runUpdate(CheckUpdateResult checkResult,
            UpdateFlow.ProgressCallback progress, UpdateFlow.CompleteCallback complete) {
        ensureInit();
        ResStateInfo stateInfo = checkResult.stateInfo;
        UpdateInfo updateInfo = checkResult.updateInfo;
        if (stateInfo == null || updateInfo == null) {
            complete.onComplete(failResult("更新状态或更新信息为空"));
            return;
        }
        updateModule.update(stateInfo, updateInfo, progress, result -> {
            if (result.success) {
                syncInstalledVersionAfterUpdate();
            }
            complete.onComplete(result);
        });
    }

    public void pause() {
        if (updateModule != null) {
            updateModule.pause();
        }
    }

    public void resume() {
        if (updateModule != null) {
            updateModule.resume();
        }
    }

    public void stop() {
        if (updateModule != null) {
            updateModule.stop();
        }
    }

    /** 把服务器/管线判定版本同步到已安装版本字段。 */
    private void syncInstalledVersion(ResStateInfo stateInfo) {
        if (stateInfo != null && stateInfo.newVersion != null && !stateInfo.newVersion.isEmpty()) {
            persistVersion(stateInfo.newVersion);
        }
    }

    private void syncInstalledVersionAfterUpdate() {
        if (configManager != null) {
            String server = configManager.gameServerConfig != null
                    && configManager.gameServerConfig.defaultConfig != null
                    ? configManager.gameServerConfig.defaultConfig.config.version : null;
            if (server != null && !server.isEmpty()) {
                persistVersion(server);
            }
        }
    }

    private void persistVersion(String version) {
        try {
            Config.setting().setGameInstalledVersion(version);
            Config.setting().save();
        } catch (Exception e) {
            LOG.warn("写回已安装版本失败: {}", version, e);
        }
    }

    public ResourceConfigManager getConfigManager() {
        return configManager;
    }

    public ResUpdateModule getUpdateModule() {
        return updateModule;
    }

    private UpdateResult failResult(String msg) {
        UpdateResult r = new UpdateResult();
        r.success = false;
        r.errorMessage = msg;
        return r;
    }
}