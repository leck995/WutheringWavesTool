package cn.tealc.wutheringwavestool.service;

import cn.tealc.wwt.game.resource.GameDownloadSource;
import cn.tealc.wwt.game.resource.GameServerSwitchService;
import cn.tealc.wwt.game.resource.ServerSwitchPhase;
import cn.tealc.wwt.game.resource.ServerSwitchResult;
import cn.tealc.wwt.game.resource.ServerSwitchStatus;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.jna.GameAppListener;
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

import java.nio.file.Path;

/** JavaFX-facing adapter for the reduced mainland/Bilibili server switch service. */
@Singleton
public final class GameServerSwitchCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(GameServerSwitchCoordinator.class);

    private final GameServerSwitchService switchService;
    private final TaskManageService taskManageService;
    private final ReadOnlyBooleanWrapper operating = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper(0);
    private final ReadOnlyStringWrapper statusText = new ReadOnlyStringWrapper("准备就绪");
    private final ReadOnlyStringWrapper detailText = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<GameDownloadSource> currentSource = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyBooleanWrapper switchAvailable = new ReadOnlyBooleanWrapper(false);

    private volatile ServerSwitchTask activeTask;
    private ServerSwitchStatus lastStatus;

    @Inject
    public GameServerSwitchCoordinator(GameServerSwitchService switchService,
            TaskManageService taskManageService) {
        this.switchService = switchService;
        this.taskManageService = taskManageService;
    }

    public void refresh() {
        Path gameDirectory = getGameDirectoryOrNull();
        if (gameDirectory == null) {
            lastStatus = null;
            currentSource.set(null);
            switchAvailable.set(false);
            statusText.set("请先选择游戏安装目录");
            detailText.set("");
            return;
        }
        try {
            lastStatus = switchService.inspect(gameDirectory);
            currentSource.set(lastStatus.activeSource());
            switchAvailable.set(lastStatus.activeSource() == GameDownloadSource.MAINLAND
                    || lastStatus.activeSource() == GameDownloadSource.BILIBILI);
            if (lastStatus.activeSource() != null) {
                Config.setting().setGameRootDirSource(SourceType.fromGameDownloadSource(lastStatus.activeSource()));
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
                detailText.set("国际服不支持快速切换");
            } else {
                statusText.set("无法识别当前服务器");
                detailText.set("可重新下载必要文件修复当前服务器");
            }
        } catch (Exception e) {
            LOG.warn("读取服务器切换状态失败", e);
            currentSource.set(null);
            switchAvailable.set(false);
            statusText.set("读取切换缓存失败");
            detailText.set(messageOf(e));
        }
    }

    public void switchTo(SourceType target) {
        GameDownloadSource targetSource = supportedSource(target);
        if (targetSource == null || activeTask != null) {
            return;
        }
        if (lastStatus == null || (lastStatus.activeSource() != GameDownloadSource.MAINLAND
                && lastStatus.activeSource() != GameDownloadSource.BILIBILI)) {
            statusText.set("当前服务器不支持快速切换");
            detailText.set("国际服或无法识别的服务器需要重新下载对应资源");
            return;
        }
        if (GameAppListener.getInstance().isRunning()) {
            statusText.set("请先关闭游戏再切换服务器");
            detailText.set("");
            return;
        }
        Path gameDirectory = getGameDirectoryOrNull();
        if (gameDirectory == null) {
            refresh();
            return;
        }
        startTask("正在切换服务器", task -> switchService.switchTo(gameDirectory, targetSource,
                (phase, completed, total, detail) -> report(task, phase, completed, total, detail)),
                result -> {
                    Config.setting().setGameRootDirSource(SourceType.fromGameDownloadSource(result.source()));
                    Config.setting().save();
                    statusText.set("服务器切换完成");
                    detailText.set(result.source() == GameDownloadSource.BILIBILI ? "当前服务器：BiliBili" : "当前服务器：国内官服");
                    refresh();
                });
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
                        Config.setting().setGameRootDirSource(SourceType.fromGameDownloadSource(result.source()));
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
    public ReadOnlyDoubleProperty progressProperty() { return progress.getReadOnlyProperty(); }
    public ReadOnlyObjectProperty<GameDownloadSource> currentSourceProperty() {
        return currentSource.getReadOnlyProperty();
    }
    public ReadOnlyStringProperty statusTextProperty() { return statusText.getReadOnlyProperty(); }
    public ReadOnlyStringProperty detailTextProperty() { return detailText.getReadOnlyProperty(); }
}
