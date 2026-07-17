package cn.tealc.wutheringwavestool.service;

import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.webkujiequ.AuthConfig;
import cn.tealc.wutheringwavestool.model.webkujiequ.PagePreset;
import cn.tealc.wutheringwavestool.ui.kujiequ.web.KuroWebShell;
import cn.tealc.wutheringwavestool.ui.kujiequ.web.WebKujiequView;
import com.google.inject.Singleton;
import com.kuro.kujiequ.model.sign.UserInfo;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 库街区 H5 手机视图管理器（JavaFX WebView）。
 * <p>
 * 替代原先 {@code web-kujiequ.exe} 进程方案：单例 Stage + WebView，
 * 换用户/页面时 forceReload，关窗释放页面，应用退出 dispose。
 * </p>
 *
 * <pre>
 *   manager.setUserInfo(userA);
 *   manager.openGrowthCalculator();
 *
 *   manager.setUserInfo(userB);
 *   manager.openRoleBox();
 * </pre>
 */
@Singleton
public class WebKujiequManager {
    private static final Logger LOG = LoggerFactory.getLogger(WebKujiequManager.class);

    private static final String SERVER_ID = "76402e5b20be2c39f095a152090afddc";
    private static final String CHANNEL_ID = "19";
    private static final String GAME_ID = "3";

    private final Object lock = new Object();

    private AuthConfig auth = new AuthConfig()
            .serverId(SERVER_ID)
            .channelId(CHANNEL_ID)
            .gameId(GAME_ID);

    private KuroWebShell shell;
    private WebKujiequView hostView;
    private boolean stopped;

    public WebKujiequManager() {
    }

    /**
     * 设置当前用户（仅内存；下次 open 注入）。
     */
    public void setUserInfo(UserInfo userInfo) {
        if (userInfo == null) {
            LOG.warn("setUserInfo: userInfo is null");
            return;
        }
        AuthConfig next = auth.copy()
                .token(userInfo.getToken())
                .did(userInfo.getDevCode())
                .userId(userInfo.getUserId())
                .roleId(userInfo.getRoleId());
        if (auth.serverId == null || auth.serverId.isBlank()) {
            next.serverId(SERVER_ID);
        }
        next.channelId(CHANNEL_ID).gameId(GAME_ID);
        setAuth(next);
        LOG.info("UserInfo set: userId={}, roleId={}, didLen={}",
                auth.userId, auth.roleId, auth.did == null ? 0 : auth.did.length());
    }

    public void setServerId(String serverId) {
        if (serverId != null && !serverId.isBlank()) {
            synchronized (lock) {
                auth.serverId(serverId);
            }
        }
    }

    /**
     * @deprecated 已改为内嵌 WebView，无需 exe。保留方法避免旧调用编译失败。
     */
    @Deprecated
    public void setExePath(String exePath) {
        LOG.debug("setExePath ignored (embedded WebView): {}", exePath);
    }

    /** 兼容旧调用：懒初始化。 */
    public void start() {
        stopped = false;
        LOG.debug("WebKujiequ embedded mode: start() ready");
    }

    /**
     * 应用退出时调用：销毁 Stage 与 WebView，避免泄漏。
     */
    public void stop() {
        stopped = true;
        runOnFx(() -> {
            if (hostView != null) {
                hostView.dispose();
                hostView = null;
            } else if (shell != null && !shell.isDisposed()) {
                shell.dispose();
            }
            shell = null;
            LOG.info("WebKujiequManager stopped / disposed");
            return null;
        });
    }

    public boolean isRunning() {
        return hostView != null && hostView.isShowing() && !stopped;
    }

    public void openRoleBox() {
        openPage(PagePreset.MC_ROLE_BOX.id());
    }

    public void openResourceBriefing() {
        openPage(PagePreset.RESOURCE_BRIEFING.id());
    }

    public void openCalendar() {
        openPage(PagePreset.MC_CALENDAR.id());
    }

    public void openGrowthCalculator() {
        openPage(PagePreset.GROWTH_CALCULATOR.id());
    }

    public void openMonthSign() {
        openPage(PagePreset.MC_MONTH_SIGN.id());
    }

    /**
     * 打开内置页面（任意线程可调）。
     */
    public void openPage(String pageName) {
        if (stopped) {
            LOG.warn("openPage ignored: manager stopped");
            return;
        }
        if (!ensureUserReady()) {
            return;
        }
        Optional<PagePreset> preset = PagePreset.parse(pageName);
        if (preset.isEmpty()) {
            String msg = "未知页面: " + pageName;
            LOG.error(msg);
            notifyError(msg);
            return;
        }
        openInternal(preset.get().defaultUrl(), preset.get());
    }

    /**
     * 打开自定义 URL。
     */
    public void openUrl(String url) {
        if (stopped) {
            LOG.warn("openUrl ignored: manager stopped");
            return;
        }
        if (!ensureUserReady()) {
            return;
        }
        if (url == null || url.isBlank()) {
            notifyError("URL 为空");
            return;
        }
        PagePreset preset = PagePreset.detectFromUrl(url).orElse(null);
        openInternal(url.trim(), preset);
    }

    // -------------------------------------------------------------------------

    private void setAuth(AuthConfig next) {
        synchronized (lock) {
            auth = next == null ? new AuthConfig() : next.copy().normalize();
            if (auth.channelId == null || auth.channelId.isBlank()) {
                auth.channelId = CHANNEL_ID;
            }
            if (auth.gameId == null || auth.gameId.isBlank()) {
                auth.gameId = GAME_ID;
            }
            if (auth.serverId == null || auth.serverId.isBlank()) {
                auth.serverId = SERVER_ID;
            }
            if (shell != null && !shell.isDisposed()) {
                shell.setAuth(auth);
            }
        }
    }

    private boolean ensureUserReady() {
        AuthConfig snap;
        synchronized (lock) {
            snap = auth.copy();
        }
        if (snap.token == null || snap.token.isBlank()) {
            notifyError("token 为空，请先选择库街区账号");
            return false;
        }
        if (snap.did == null || snap.did.isBlank()) {
            notifyError("did 为空，请重新登录库街区账号");
            return false;
        }
        return true;
    }

    private void openInternal(String baseUrl, PagePreset preset) {
        AuthConfig snap;
        synchronized (lock) {
            snap = auth.copy();
        }
        String entry = PagePreset.resolveEntryUrl(baseUrl, snap, preset);
        String title = preset != null ? preset.title() : "库街区";

        try {
            runOnFx(() -> {
                ensureHost();
                shell.setAuth(snap);
                hostView.setActivePreset(preset);
                hostView.setTitle(title);
                shell.open(entry, snap, true);
                hostView.showAndFocus();
                LOG.info("Opened page title={} url={}", title, entry);
                return null;
            });
        } catch (Exception e) {
            LOG.error("打开库街区页面失败: {}", e.getMessage(), e);
            notifyError("打开页面失败: " + e.getMessage());
        }
    }

    /** 必须在 FX 线程。 */
    private void ensureHost() {
        if (shell != null && shell.isDisposed()) {
            shell = null;
            hostView = null;
        }
        if (hostView != null && hostView.isDisposed()) {
            hostView = null;
            shell = null;
        }
        if (shell == null) {
            shell = new KuroWebShell();
            shell.setAuth(auth.copy());
        }
        if (hostView == null) {
            hostView = new WebKujiequView(shell);
            // 标题栏图标切换页面（与 openPage 同一路径）
            hostView.setPageOpenHandler(p -> {
                if (p != null) {
                    openPage(p.id());
                }
            });
            hostView.ensureCreated();
        } else {
            hostView.ensureCreated();
        }
        stopped = false;
    }

    private void notifyError(String message) {
        LOG.warn(message);
        try {
            if (Platform.isFxApplicationThread()) {
                NotificationManager.message(MessageInfo.warning(message));
            } else {
                Platform.runLater(() -> NotificationManager.message(MessageInfo.warning(message)));
            }
        } catch (Exception e) {
            LOG.debug("notify failed: {}", e.getMessage());
        }
    }

    private <T> T runOnFx(FxCallable<T> action) {
        if (Platform.isFxApplicationThread()) {
            try {
                return action.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        AtomicReference<T> ref = new AtomicReference<>();
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                ref.set(action.call());
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(60, TimeUnit.SECONDS)) {
                throw new RuntimeException("JavaFX action timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted waiting for JavaFX", e);
        }
        if (err.get() != null) {
            Throwable t = err.get();
            if (t instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(t);
        }
        return ref.get();
    }

    @FunctionalInterface
    private interface FxCallable<T> {
        T call() throws Exception;
    }
}
