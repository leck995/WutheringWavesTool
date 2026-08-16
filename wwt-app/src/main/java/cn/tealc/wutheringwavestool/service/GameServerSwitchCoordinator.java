package cn.tealc.wutheringwavestool.service;

import cn.tealc.wwt.game.resource.GameDownloadSource;
import cn.tealc.wwt.game.resource.GameServerSwitchService;
import cn.tealc.wwt.game.resource.ServerSwitchPhase;
import cn.tealc.wwt.game.resource.ServerSwitchResult;
import cn.tealc.wwt.game.resource.ServerSwitchStatus;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.jna.GameAppListener;
import cn.tealc.wutheringwavestool.model.GameEdition;
import cn.tealc.wutheringwavestool.model.GameInstallation;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/** JavaFX-facing adapter for the reduced mainland/Bilibili server switch service. */
@Singleton
public final class GameServerSwitchCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(GameServerSwitchCoordinator.class);

    private final GameServerSwitchService switchService;
    private final TaskManageService taskManageService;
    private final GameInstallationManager installationManager;
    private final ReadOnlyBooleanWrapper operating = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper(0);
    private final ReadOnlyStringWrapper statusText = new ReadOnlyStringWrapper("准备就绪");
    private final ReadOnlyStringWrapper detailText = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<GameDownloadSource> currentSource = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyBooleanWrapper switchAvailable = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper mainlandCacheReady = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper bilibiliCacheReady = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper mainlandTargetReady = new ReadOnlyBooleanWrapper(true);
    private final ReadOnlyBooleanWrapper bilibiliTargetReady = new ReadOnlyBooleanWrapper(true);

    private volatile ServerSwitchTask activeTask;
    private ServerSwitchStatus lastStatus;
    private ServerSwitchStatus chinaStatus;

    @Inject
    public GameServerSwitchCoordinator(GameServerSwitchService switchService,
            TaskManageService taskManageService, GameInstallationManager installationManager) {
        this.switchService = switchService;
        this.taskManageService = taskManageService;
        this.installationManager = installationManager;
    }

    public void refresh() {
        refreshChinaStatus();
        GameInstallation activeInstallation = installationManager.activeInstallation();
        Path gameDirectory = getGameDirectoryOrNull();
        if (activeInstallation == null || gameDirectory == null) {
            clearActiveStatus("请先选择游戏安装目录", "");
            return;
        }
        try {
            lastStatus = activeInstallation.getEdition() == GameEdition.CHINA
                    ? chinaStatus : switchService.inspect(gameDirectory);
            if (lastStatus == null) {
                clearActiveStatus("无法读取当前游戏安装目录", "请重新配置该安装实例");
                return;
            }
            currentSource.set(lastStatus.activeSource());
            switchAvailable.set(activeInstallation.getEdition() == GameEdition.CHINA
                    && isDomestic(lastStatus.activeSource()));
            if (lastStatus.activeSource() != null) {
                installationManager.updateSource(activeInstallation.getEdition(),
                        SourceType.fromGameDownloadSource(lastStatus.activeSource()));
            }
            if (lastStatus.recoveryPending()) {
                statusText.set("检测到未完成的切换操作");
                detailText.set("下一次下载或切换时会尝试恢复原文件");
            } else if (lastStatus.activeSource() == GameDownloadSource.BILIBILI) {
                statusText.set("当前服务器：BiliBili");
                detailText.set(lastStatus.mainlandCacheReady() ? "国内官服切换缓存已就绪" : "国内官服切换缓存未准备");
            } else if (lastStatus.activeSource() == GameDownloadSource.MAINLAND) {
                statusText.set("当前服务器：国内官服");
                detailText.set(lastStatus.bilibiliCacheReady() ? "BiliBili 切换缓存已就绪" : "BiliBili 切换缓存未准备");
            } else if (lastStatus.activeSource() == GameDownloadSource.GLOBAL) {
                statusText.set("当前服务器：国际服");
                detailText.set("国际服使用独立游戏目录");
            } else {
                statusText.set("无法识别当前服务器");
                detailText.set("可重新下载必要文件修复当前服务器");
            }
        } catch (Exception e) {
            LOG.warn("读取服务器切换状态失败", e);
            clearActiveStatus("读取服务器状态失败", messageOf(e));
        }
    }

    public void switchTo(SourceType target) {
        if (target == null || activeTask != null) {
            return;
        }
        if (GameAppListener.getInstance().isRunning()) {
            statusText.set("请先关闭游戏再切换服务器");
            detailText.set("");
            return;
        }
        GameEdition targetEdition = GameInstallationManager.editionOf(target);
        Path targetDirectory = installationManager.gameDirectory(targetEdition).orElse(null);
        if (targetDirectory == null || !installationManager.isConfigured(targetEdition)) {
            statusText.set("请先配置目标服务器的游戏目录");
            detailText.set(target == SourceType.GLOBAL ? "尚未配置国际服目录" : "尚未配置国内服目录");
            return;
        }
        if (targetEdition == GameEdition.GLOBAL) {
            installationManager.updateSource(GameEdition.GLOBAL, SourceType.GLOBAL);
            installationManager.activate(GameEdition.GLOBAL);
            Config.setting().save();
            statusText.set("服务器切换完成");
            detailText.set("当前服务器：国际服");
            refresh();
            return;
        }

        GameDownloadSource targetSource = supportedSource(target);
        refreshChinaStatus();
        if (chinaStatus == null || !isDomestic(chinaStatus.activeSource())) {
            statusText.set("无法识别国内服游戏目录");
            detailText.set("请确认目录属于国内官服或 BiliBili");
            return;
        }
        if (chinaStatus.activeSource() == targetSource) {
            activateChina(target);
            return;
        }
        boolean cacheReady = targetSource == GameDownloadSource.MAINLAND
                ? chinaStatus.mainlandCacheReady() : chinaStatus.bilibiliCacheReady();
        if (!cacheReady) {
            statusText.set("目标服务器切换缓存未准备");
            detailText.set("请先在资源管理中下载对应服务器的必要文件");
            return;
        }
        startTask("正在切换服务器", task -> switchService.switchTo(targetDirectory, targetSource,
                (phase, completed, total, detail) -> report(task, phase, completed, total, detail)),
                result -> {
                    installationManager.updateSource(GameEdition.CHINA,
                            SourceType.fromGameDownloadSource(result.source()));
                    installationManager.activate(GameEdition.CHINA);
                    Config.setting().save();
                    statusText.set("服务器切换完成");
                    detailText.set(result.source() == GameDownloadSource.BILIBILI ? "当前服务器：BiliBili" : "当前服务器：国内官服");
                    refresh();
                });
    }

    /** Configures a physical installation, then performs the requested logical server switch. */
    public String configureAndSwitch(SourceType target, Path gameDirectory) {
        if (target == null || gameDirectory == null) {
            return "游戏目录不能为空";
        }
        Path normalized = gameDirectory.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized.resolve("Wuthering Waves.exe"))) {
            return "所选目录中未找到 Wuthering Waves.exe";
        }
        var detected = switchService.detectActiveSource(normalized);
        if (detected.isEmpty()) {
            return "无法识别所选目录的服务器类型";
        }
        SourceType detectedSource = SourceType.fromGameDownloadSource(detected.get());
        if (GameInstallationManager.editionOf(detectedSource) != GameInstallationManager.editionOf(target)) {
            return target == SourceType.GLOBAL ? "所选目录不是国际服目录" : "所选目录不是国内服目录";
        }
        installationManager.configureInstallation(detectedSource, normalized, false);
        Config.setting().save();
        refresh();
        switchTo(target);
        return null;
    }

    public boolean isInstallationConfigured(SourceType source) {
        return installationManager.isConfigured(source);
    }

    public void redownloadRequiredFiles(SourceType source) {
        GameDownloadSource targetSource = supportedSource(source);
        if (targetSource == null || activeTask != null) {
            return;
        }
        Path gameDirectory = getGameDirectoryOrNull();
        if (gameDirectory == null) {
            refresh();
            return;
        }
        GameDownloadSource activeSource = lastStatus != null ? lastStatus.activeSource() : null;
        boolean applyToGame = activeSource == targetSource
                || (activeSource == null && Config.setting().getGameRootDirSource() == source);
        if (applyToGame && GameAppListener.getInstance().isRunning()) {
            statusText.set("请先关闭游戏再修复当前服务器");
            detailText.set("");
            return;
        }
        String initialStatus = applyToGame ? "正在重新下载并修复当前服务器" : "正在下载切换缓存";
        startTask(initialStatus, task -> switchService.redownloadRequiredFiles(gameDirectory, targetSource,
                applyToGame, (phase, completed, total, detail) -> report(task, phase, completed, total, detail)),
                result -> {
                    if (result.appliedToGame()) {
                        installationManager.updateSource(GameEdition.CHINA,
                                SourceType.fromGameDownloadSource(result.source()));
                        Config.setting().save();
                        statusText.set("当前服务器必要文件已修复");
                    } else {
                        statusText.set("切换缓存下载完成");
                    }
                    detailText.set("");
                    refresh();
                });
    }

    public void deleteCache(SourceType source) {
        GameDownloadSource targetSource = supportedSource(source);
        if (targetSource == null || activeTask != null) {
            return;
        }
        Path gameDirectory = getGameDirectoryOrNull();
        if (gameDirectory == null) {
            refresh();
            return;
        }
        startTask("正在删除切换缓存", task -> {
            switchService.deleteCache(gameDirectory, targetSource,
                    (phase, completed, total, detail) -> report(task, phase, completed, total, detail));
            return new ServerSwitchResult(targetSource, false, false, "");
        }, result -> {
            statusText.set("切换缓存已删除");
            detailText.set("");
            refresh();
        });
    }

    private void refreshChinaStatus() {
        Path chinaDirectory = installationManager.gameDirectory(GameEdition.CHINA).orElse(null);
        if (chinaDirectory == null) {
            chinaStatus = null;
            mainlandCacheReady.set(false);
            bilibiliCacheReady.set(false);
            mainlandTargetReady.set(true);
            bilibiliTargetReady.set(true);
            return;
        }
        try {
            chinaStatus = switchService.inspect(chinaDirectory);
            mainlandCacheReady.set(chinaStatus.mainlandCacheReady());
            bilibiliCacheReady.set(chinaStatus.bilibiliCacheReady());
            mainlandTargetReady.set(chinaStatus.activeSource() == GameDownloadSource.MAINLAND
                    || chinaStatus.mainlandCacheReady());
            bilibiliTargetReady.set(chinaStatus.activeSource() == GameDownloadSource.BILIBILI
                    || chinaStatus.bilibiliCacheReady());
            if (isDomestic(chinaStatus.activeSource())) {
                installationManager.updateSource(GameEdition.CHINA,
                        SourceType.fromGameDownloadSource(chinaStatus.activeSource()));
            }
        } catch (Exception e) {
            LOG.warn("读取国服安装实例状态失败", e);
            chinaStatus = null;
            mainlandCacheReady.set(false);
            bilibiliCacheReady.set(false);
            boolean needsConfiguration = !installationManager.isConfigured(GameEdition.CHINA);
            mainlandTargetReady.set(needsConfiguration);
            bilibiliTargetReady.set(needsConfiguration);
        }
    }

    private void activateChina(SourceType target) {
        installationManager.updateSource(GameEdition.CHINA, target);
        installationManager.activate(GameEdition.CHINA);
        Config.setting().save();
        statusText.set("服务器切换完成");
        detailText.set(target == SourceType.BILIBILI ? "当前服务器：BiliBili" : "当前服务器：国内官服");
        refresh();
    }

    private void clearActiveStatus(String status, String detail) {
        lastStatus = null;
        currentSource.set(null);
        switchAvailable.set(false);
        statusText.set(status);
        detailText.set(detail);
    }

    private void startTask(String initialStatus, Operation operation, ResultHandler successHandler) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> startTask(initialStatus, operation, successHandler));
            return;
        }
        ServerSwitchTask task = new ServerSwitchTask(operation);
        activeTask = task;
        operating.set(true);
        progress.set(0);
        progress.bind(task.progressProperty());
        statusText.set(initialStatus);
        detailText.set("");
        task.messageProperty().addListener((observable, oldValue, newValue) -> detailText.set(newValue));
        task.setOnSucceeded(event -> {
            activeTask = null;
            operating.set(false);
            progress.unbind();
            progress.set(1);
            successHandler.handle(task.getValue());
        });
        task.setOnFailed(event -> {
            activeTask = null;
            operating.set(false);
            progress.unbind();
            progress.set(0);
            Throwable exception = task.getException();
            LOG.warn("服务器切换操作失败", exception);
            statusText.set("服务器切换操作失败");
            detailText.set(messageOf(exception));
        });
        taskManageService.execute(task);
    }

    private static void report(ServerSwitchTask task, ServerSwitchPhase phase, long completed, long total, String detail) {
        if (total > 0) {
            task.reportProgress(completed, total, detail);
        } else if (phase == ServerSwitchPhase.APPLYING || phase == ServerSwitchPhase.VERIFYING) {
            task.reportIndeterminate(detail);
        } else {
            task.reportIndeterminate(detail);
        }
    }

    private static GameDownloadSource supportedSource(SourceType source) {
        if (source == SourceType.DEFAULT) {
            return GameDownloadSource.MAINLAND;
        }
        if (source == SourceType.BILIBILI) {
            return GameDownloadSource.BILIBILI;
        }
        return null;
    }

    private static boolean isDomestic(GameDownloadSource source) {
        return source == GameDownloadSource.MAINLAND || source == GameDownloadSource.BILIBILI;
    }

    private static Path getGameDirectoryOrNull() {
        var gameDirectory = GameResourcesManager.getGameDir();
        return gameDirectory != null ? gameDirectory.toPath().toAbsolutePath().normalize() : null;
    }

    private static String messageOf(Throwable exception) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "请关闭游戏和官方启动器后重试";
        }
        return exception.getMessage();
    }

    @FunctionalInterface
    private interface Operation {
        ServerSwitchResult run(ServerSwitchTask task) throws Exception;
    }

    @FunctionalInterface
    private interface ResultHandler {
        void handle(ServerSwitchResult result);
    }

    private static final class ServerSwitchTask extends Task<ServerSwitchResult> {
        private final Operation operation;

        private ServerSwitchTask(Operation operation) {
            this.operation = operation;
        }

        @Override
        protected ServerSwitchResult call() throws Exception {
            return operation.run(this);
        }

        private void reportProgress(long completed, long total, String detail) {
            updateProgress(completed, total);
            updateMessage(detail != null ? detail : "");
        }

        private void reportIndeterminate(String detail) {
            updateProgress(-1, 1);
            updateMessage(detail != null ? detail : "");
        }
    }

    public ReadOnlyBooleanProperty operatingProperty() { return operating.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty switchAvailableProperty() { return switchAvailable.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty mainlandCacheReadyProperty() {
        return mainlandCacheReady.getReadOnlyProperty();
    }
    public ReadOnlyBooleanProperty bilibiliCacheReadyProperty() {
        return bilibiliCacheReady.getReadOnlyProperty();
    }
    public ReadOnlyBooleanProperty mainlandTargetReadyProperty() {
        return mainlandTargetReady.getReadOnlyProperty();
    }
    public ReadOnlyBooleanProperty bilibiliTargetReadyProperty() {
        return bilibiliTargetReady.getReadOnlyProperty();
    }
    public ReadOnlyDoubleProperty progressProperty() { return progress.getReadOnlyProperty(); }
    public ReadOnlyObjectProperty<GameDownloadSource> currentSourceProperty() {
        return currentSource.getReadOnlyProperty();
    }
    public ReadOnlyStringProperty statusTextProperty() { return statusText.getReadOnlyProperty(); }
    public ReadOnlyStringProperty detailTextProperty() { return detailText.getReadOnlyProperty(); }
}
