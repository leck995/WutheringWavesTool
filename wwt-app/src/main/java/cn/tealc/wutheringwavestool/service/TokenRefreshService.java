package cn.tealc.wutheringwavestool.service;

import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kuro.kujiequ.KujiequManager;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.model.ResponseBody;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @description: Token 过期检测与 B-At 自动刷新服务。
 * - 监听 TOKEN_EXPIRED 通知，触发用户可见的提示，并自动清除对应用户的 B-At 缓存；
 * - 监听 ACCOUNT_UPDATE 通知，在用户更新 token 后清除旧 B-At 缓存；
 * - 启动时预取主用户的 B-At token；
 * - 定时刷新所有已缓存用户的 B-At token（每30分钟）。
 * @author: Leck
 * @create: 2026-06-21
 */
@Singleton
public class TokenRefreshService {
    private static final Logger LOG = LoggerFactory.getLogger(TokenRefreshService.class);
    private static final long B_AT_REFRESH_INTERVAL_MINUTES = 30; // B-At 定时刷新间隔

    private final UserInfoService userInfoService;
    private final KujiequManager kujiequManager;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "bat-refresh-scheduler");
        t.setDaemon(true);
        return t;
    });

    @Inject
    public TokenRefreshService(UserInfoService userInfoService, KujiequManager kujiequManager) {
        this.userInfoService = userInfoService;
        this.kujiequManager = kujiequManager;
    }

    /**
     * 启动服务：订阅通知、预取 B-At、启动定时刷新
     */
    public void start() {
        // 订阅 Token 过期通知
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.TOKEN_EXPIRED, (key, payload) -> {
            if (payload.length >= 1 && payload[0] instanceof UserInfo userInfo) {
                handleTokenExpired(userInfo);
            }
        });

        // 订阅账号更新通知 → 清除旧 B-At 并预取新 B-At
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.ACCOUNT_UPDATE, (key, payload) -> {
            Platform.runLater(() -> {
                kujiequManager.invalidateAllAccessTokens();
                LOG.info("账号已更新，B-At 缓存已全部清除");
                // 立即预取主用户的 B-At
                Platform.runLater(this::prefetchMainUserBAt);
            });
        });

        // 启动时预取 B-At（主用户）
        prefetchMainUserBAt();

        // 启动定时 B-At 刷新（每30分钟，初始延迟2分钟）
        scheduler.scheduleAtFixedRate(
                this::refreshAllBAtTokens,
                2, B_AT_REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES
        );

        LOG.info("TokenRefreshService 已启动（B-At 定时刷新: 每{}分钟）", B_AT_REFRESH_INTERVAL_MINUTES);
    }


    /**
     * 处理 Token 过期事件：清除 B-At 缓存并通知用户
     */
    private void handleTokenExpired(UserInfo userInfo) {
        kujiequManager.invalidateAccessToken(userInfo.getUserId());

        Platform.runLater(() -> {
            String roleName = userInfo.getRoleName() != null ? userInfo.getRoleName() : userInfo.getRoleId();
            NotificationManager.message(
                    MessageInfo.warning(String.format("「%s」的库街区 Token 已过期，请前往账号页面更新 Token", roleName))
            );
        });
    }


    /**
     * 启动时预取主用户的 B-At token，让所有 API 调用无需等待首次 B-At 请求
     */
    private void prefetchMainUserBAt() {
        UserInfo mainUser = userInfoService.getMainUser();
        if (mainUser != null && mainUser.getToken() != null && !mainUser.getToken().isBlank()) {
            if (kujiequManager.isAccessTokenCached(mainUser.getUserId())) {
                LOG.debug("B-At 已有缓存，跳过预取: userId={}", mainUser.getUserId());
                return;
            }
            Thread.startVirtualThread(() -> {
                boolean success = kujiequManager.prefetchAccessToken(mainUser);
                if (success) {
                    LOG.info("B-At 启动预取成功: {}", mainUser.getRoleName());
                } else {
                    LOG.warn("B-At 启动预取失败，将在首次 API 请求时自动获取");
                }
            });
        }
    }


    /**
     * 定时刷新所有已缓存用户的 B-At token
     */
    private void refreshAllBAtTokens() {
        List<UserInfo> users = userInfoService.getAllUsers();
        if (users == null || users.isEmpty()) {
            return;
        }
        for (UserInfo user : users) {
            if (user.getToken() != null && !user.getToken().isBlank()) {
                refreshBAtToken(user);
            }
        }
    }


    /**
     * 异步刷新指定用户的 B-At token
     */
    private void refreshBAtToken(UserInfo userInfo) {
        // 只有在有缓存时才需要提前刷新；无缓存时首次请求会自动获取
        if (!kujiequManager.isAccessTokenCached(userInfo.getUserId())) {
            return;
        }

        Thread.startVirtualThread(() -> {
            // 先清除缓存，强制下次 getAccessToken 重新获取
            kujiequManager.invalidateAccessToken(userInfo.getUserId());
            boolean success = kujiequManager.prefetchAccessToken(userInfo);
            if (success) {
                LOG.debug("B-At 定时刷新成功: {}", userInfo.getRoleName());
            } else {
                // 失败没关系，下次 API 调用时会自动重试
                LOG.debug("B-At 定时刷新失败（将在需要时自动获取）: {}", userInfo.getRoleName());
            }
        });
    }


    /**
     * 主动检查已保存用户的主 Token 是否仍然有效。
     * 调用 Kuro API 轻量检测（使用 GameRoleSeekTask）。
     * 如果无效则发布 TOKEN_EXPIRED 通知。
     */
    public void checkAllUsersTokenHealth() {
        List<UserInfo> users = userInfoService.getAllUsers();
        if (users == null || users.isEmpty()) {
            return;
        }
        for (UserInfo user : users) {
            if (user.getToken() != null && !user.getToken().isBlank()) {
                checkUserTokenHealth(user);
            }
        }
    }


    /**
     * 检测单个用户主 token 是否有效，通过调用 Kuro 轻量 API
     */
    private void checkUserTokenHealth(UserInfo userInfo) {
        Thread.startVirtualThread(() -> {
            try {
                ResponseBody<?> result = kujiequManager.seekGameRole(
                        userInfo.getToken(), Boolean.TRUE.equals(userInfo.getIsWeb()));
                if (result != null && result.getCode() == 200) {
                    LOG.debug("Token 健康检测通过: {}", userInfo.getRoleName());
                } else if (result != null) {
                    String msg = result.getMsg();
                    if (msg != null && msg.contains("登录已过期")) {
                        LOG.warn("Token 健康检测失败（已过期）: {}", userInfo.getRoleName());
                        NotificationManager.publish(NotificationKey.TOKEN_EXPIRED, userInfo, msg);
                    }
                }
            } catch (Exception e) {
                LOG.error("Token 健康检测异常: {}", userInfo.getRoleName(), e);
            }
        });
    }
}
