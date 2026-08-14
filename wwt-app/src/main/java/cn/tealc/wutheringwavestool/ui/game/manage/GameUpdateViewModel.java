package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.service.GameUpdateService;
import cn.tealc.wutheringwavestool.service.TaskManageService;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import cn.tealc.teafx.utils.message.MessageInfo;
import com.google.inject.Inject;
import com.kr.launcher.model.CheckUpdateResult;
import com.kr.launcher.model.ResStateInfo;
import de.saxsys.mvvmfx.SceneLifecycle;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 游戏更新 ViewModel：检查更新 → 展示当前/最新版本 → 开始更新 → 进度/状态。
 */
public class GameUpdateViewModel extends BaseViewModel implements SceneLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(GameUpdateViewModel.class);

    @Inject
    private GameUpdateService updateService;
    @Inject
    private TaskManageService taskManageService;

    private final StringProperty currentVersion = new SimpleStringProperty("-");
    private final StringProperty latestVersion = new SimpleStringProperty("-");
    private final BooleanProperty hasUpdate = new SimpleBooleanProperty(false);
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty progressText = new SimpleStringProperty("0%");
    private final StringProperty status = new SimpleStringProperty("就绪");
    private final StringProperty stateDesc = new SimpleStringProperty("");
    private final BooleanProperty busy = new SimpleBooleanProperty(false);

    private CheckUpdateResult checkResult;

    @Override
    public void onViewAdded() {
        init();
    }

    private void init() {
        String installed = Config.setting().getGameInstalledVersion();
        if (installed != null && !installed.isEmpty()) {
            currentVersion.set(installed);
        }
    }

    /** 检查更新（后台任务）。 */
    public void checkUpdate() {
        if (busy.get()) {
            return;
        }
        busy.set(true);
        status.set(LanguageManager.getString("ui.game_manager.update.checking"));
        Task<CheckUpdateResult> task = new Task<>() {
            @Override
            protected CheckUpdateResult call() {
                return updateService.checkUpdate();
            }
        };
        task.setOnSucceeded(e -> {
            CheckUpdateResult result = task.getValue();
            handleCheckResult(result);
            busy.set(false);
        });
        task.setOnFailed(e -> {
            busy.set(false);
            Throwable ex = task.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "未知错误";
            status.set(LanguageManager.getString("ui.game_manager.update.check_fail"));
            NotificationManager.message(MessageInfo.error("检查更新失败: " + msg));
        });
        taskManageService.execute(task);
    }

    private void handleCheckResult(CheckUpdateResult result) {
        if (result == null || !result.succ || result.stateInfo == null) {
            if (result != null) {
                latestVersion.set("-");
                hasUpdate.set(false);
                stateDesc.set(result.errorMessage != null ? result.errorMessage : "检查失败");
                status.set(LanguageManager.getString("ui.game_manager.update.check_fail"));
            }
            return;
        }
        ResStateInfo state = result.stateInfo;
        if (state.usingVersion != null && !state.usingVersion.isEmpty()) {
            currentVersion.set(state.usingVersion);
        }
        latestVersion.set(state.newVersion != null ? state.newVersion : "-");
        switch (state.state) {
            case ResStateInfo.STATE_UP_TO_DATE -> {
                hasUpdate.set(false);
                stateDesc.set(LanguageManager.getString("ui.game_manager.update.up_to_date"));
                status.set(LanguageManager.getString("ui.game_manager.update.up_to_date"));
            }
            case ResStateInfo.STATE_NEED_DOWNLOAD, ResStateInfo.STATE_DOWNLOADING,
                 ResStateInfo.STATE_PRE_DOWNLOAD -> {
                hasUpdate.set(true);
                stateDesc.set(LanguageManager.getString("ui.game_manager.update.need_update")
                        + (state.newVersion != null ? " -> " + state.newVersion : ""));
                status.set(LanguageManager.getString("ui.game_manager.update.ready"));
            }
            case ResStateInfo.STATE_REPAIRING -> {
                hasUpdate.set(true);
                stateDesc.set(LanguageManager.getString("ui.game_manager.update.repair"));
                status.set(LanguageManager.getString("ui.game_manager.update.ready"));
            }
            default -> {
                hasUpdate.set(false);
                stateDesc.set("更新状态: " + state.state);
            }
        }
        this.checkResult = result;
    }

    /** 开始更新（后台任务）。 */
    public void update() {
        if (busy.get()) {
            return;
        }
        if (checkResult == null || checkResult.updateInfo == null) {
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.game_manager.update.check_first")));
            return;
        }
        File validDir = validateGameDir();
        if (validDir == null) {
            return;
        }
        busy.set(true);
        progress.set(0);
        status.set(LanguageManager.getString("ui.game_manager.update.updating"));
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                final boolean[] successHolder = {false};
                updateService.runUpdate(checkResult,
                        (state, doneSize, totalSize, doneCount, totalCount) -> {
                            long pct = totalSize > 0
                                    ? (long) (doneSize * 100.0 / totalSize) : 0;
                            updateProgress(pct, 100);
                            updateMessage("state=" + state + " " + pct + "%");
                        },
                        result -> successHolder[0] = result.success);
                if (!successHolder[0]) {
                    throw new IllegalStateException("更新失败");
                }
                return null;
            }
        };
        task.progressProperty().addListener((obs, o, n) -> Platform.runLater(() -> {
            double p = n.doubleValue();
            progress.set(p);
            progressText.set(String.format("%.1f%%", p * 100));
        }));
        task.messageProperty().addListener((obs, o, n) -> Platform.runLater(() ->
                status.set(n != null ? n : "更新中")));
        task.setOnSucceeded(e -> {
            busy.set(false);
            progressText.set("100%");
            status.set(LanguageManager.getString("ui.game_manager.update.done"));
            // 更新本地已安装版本显示
            String v = Config.setting().getGameInstalledVersion();
            if (v != null && !v.isEmpty()) {
                currentVersion.set(v);
            }
            hasUpdate.set(false);
        });
        task.setOnFailed(e -> {
            busy.set(false);
            Throwable ex = task.getException();
            // 保留“有更新”状态，便于用户重试
            hasUpdate.set(true);
            status.set(LanguageManager.getString("ui.game_manager.update.fail")
                    + (ex != null && ex.getMessage() != null ? ": " + ex.getMessage() : ""));
        });
        taskManageService.execute(task);
    }

    private File validateGameDir() {
        File gameDir = GameResourcesManager.getGameDir();
        if (gameDir == null) {
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.game_manager.update.no_dir")));
            return null;
        }
        return gameDir;
    }

    public void pause() {
        updateService.pause();
    }

    public void resume() {
        updateService.resume();
    }

    public void stop() {
        updateService.stop();
    }

    public String getCurrentVersion() {
        return currentVersion.get();
    }

    public StringProperty currentVersionProperty() {
        return currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion.get();
    }

    public StringProperty latestVersionProperty() {
        return latestVersion;
    }

    public boolean isHasUpdate() {
        return hasUpdate.get();
    }

    public BooleanProperty hasUpdateProperty() {
        return hasUpdate;
    }

    public double getProgress() {
        return progress.get();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public String getProgressText() {
        return progressText.get();
    }

    public StringProperty progressTextProperty() {
        return progressText;
    }

    public String getStatus() {
        return status.get();
    }

    public StringProperty statusProperty() {
        return status;
    }

    public String getStateDesc() {
        return stateDesc.get();
    }

    public StringProperty stateDescProperty() {
        return stateDesc;
    }

    public boolean isBusy() {
        return busy.get();
    }

    public BooleanProperty busyProperty() {
        return busy;
    }

    @Override
    public void onViewRemoved() {
    }
}