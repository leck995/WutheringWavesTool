package cn.tealc.wutheringwavestool.service;

import com.google.inject.Singleton;
import com.kuro.kujiequ.model.sign.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * web-kujiequ.exe 启动器（无 --daemon 版）。
 * <p>
 * 每次 open 都新建进程，命令行注入完整用户信息。
 * 切换用户/页面时会先关掉旧进程，再起新进程，避免 A/B 用户粘住。
 * </p>
 *
 * 用法：
 * <pre>
 *   manager.setExePath(".../web-kujiequ.exe");
 *   manager.setUserInfo(userA);
 *   manager.openGrowthCalculator();
 *
 *   manager.setUserInfo(userB);
 *   manager.openRoleBox(); // 内部 stop 旧进程再 start 新进程
 * </pre>
 */
@Singleton
public class WebKujiequManager {
    private static final Logger LOG = LoggerFactory.getLogger(WebKujiequManager.class);

    private static final String SERVER_ID = "76402e5b20be2c39f095a152090afddc";
    private static final String CHANNEL_ID = "19";
    private static final String GAME_ID = "3";
    private static final String EXE_NAME = "web-kujiequ.exe";

    private Process process;
    private Thread logThread;

    private String token;
    private String userId;
    private String roleId;
    private String did;
    private String serverId = SERVER_ID;
    private String channelId = CHANNEL_ID;
    private String gameId = GAME_ID;
    private String exePath;

    public WebKujiequManager() {
    }

    /**
     * 设置当前用户信息（仅保存在 Java 侧，下次 open 时注入到新进程）。
     */
    public void setUserInfo(UserInfo userInfo) {
        if (userInfo == null) {
            LOG.warn("setUserInfo: userInfo is null");
            return;
        }
        this.token = userInfo.getToken();
        this.userId = userInfo.getUserId();
        this.roleId = userInfo.getRoleId();
        // 确认 getDevCode() 就是 did；若字段名不同请改这里
        this.did = userInfo.getDevCode();
        LOG.info("UserInfo set: userId={}, roleId={}, didLen={}",
                userId, roleId, did == null ? 0 : did.length());
    }

    public void setServerId(String serverId) {
        if (serverId != null && !serverId.isBlank()) {
            this.serverId = serverId;
        }
    }

    public void setExePath(String exePath) {
        this.exePath = exePath;
    }

    /**
     * 无 daemon 模式下 start 不单独做任何事。
     * 保留方法是为了兼容旧调用；真正启动在 openXxx() 里。
     */
    public synchronized void start() throws IOException {
        // one-shot：不预启动进程
        resolveExePath(); // 仅校验 exe 是否存在
        LOG.debug("WebKujiequ one-shot mode: start() is a no-op, openXxx() will launch process");
    }

    /**
     * 停止当前窗口进程。
     */
    public synchronized void stop() {
        if (process == null) {
            return;
        }
        LOG.info("Stopping WebKujiequ process");
        if (process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
        if (logThread != null) {
            logThread.interrupt();
            logThread = null;
        }
        process = null;
        LOG.info("WebKujiequ process stopped");
    }

    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    public void openRoleBox() throws IOException {
        openPage("mc-role-box");
    }

    public void openResourceBriefing() throws IOException {
        openPage("resource-briefing");
    }

    public void openCalendar() throws IOException {
        openPage("mccalendar");
    }

    public void openGrowthCalculator() throws IOException {
        openPage("growth-calculator");
    }

    public void openMonthSign() throws IOException {
        openPage("mc-month-sign");
    }

    /**
     * 打开指定内置页面：先停旧进程，再以当前用户启动新进程。
     */
    public synchronized void openPage(String pageName) throws IOException {
        ensureUserReady();
        stop();
        launch(pageName, null);
    }

    /**
     * 打开自定义 URL：先停旧进程，再以当前用户启动新进程。
     */
    public synchronized void openUrl(String url) throws IOException {
        ensureUserReady();
        stop();
        launch(null, url);
    }

    // -------------------------------------------------------------------------

    private void ensureUserReady() throws IOException {
        if (token == null || token.isBlank()) {
            throw new IOException("token 为空，请先 setUserInfo()");
        }
        if (did == null || did.isBlank()) {
            throw new IOException("did 为空，请先 setUserInfo()（并确认 getDevCode() 返回 did）");
        }
    }

    private void launch(String pageName, String url) throws IOException {
        String exe = resolveExePath();
        List<String> cmd = buildCommand(exe, pageName, url);
        LOG.info("Starting WebKujiequ: page={}, userId={}, roleId={}",
                pageName != null ? pageName : url, userId, roleId);
        LOG.debug("Command: {}", redactCmd(cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        process = pb.start();

        // 读日志，避免管道堵死；用户关窗后进程结束
        Process p = process;
        logThread = Thread.startVirtualThread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOG.info("WebKujiequ: {}", line);
                }
            } catch (IOException e) {
                LOG.debug("WebKujiequ log stream closed: {}", e.getMessage());
            }
            LOG.info("WebKujiequ process ended");
        });
    }

    private List<String> buildCommand(String exe, String pageName, String url) {
        List<String> cmd = new ArrayList<>();
        cmd.add(exe);

        if (url != null && !url.isBlank()) {
            cmd.add("--url");
            cmd.add(url);
        } else if (pageName != null && !pageName.isBlank()) {
            cmd.add("--page");
            cmd.add(pageName);
        } else {
            cmd.add("--page");
            cmd.add("mc-role-box");
        }

        // 每次启动都注入完整用户信息（one-shot 核心）
        cmd.add("--token");
        cmd.add(nullToEmpty(token));
        cmd.add("--did");
        cmd.add(nullToEmpty(did));
        if (userId != null && !userId.isBlank()) {
            cmd.add("--user-id");
            cmd.add(userId);
        }
        if (roleId != null && !roleId.isBlank()) {
            cmd.add("--role-id");
            cmd.add(roleId);
        }
        cmd.add("--server-id");
        cmd.add(nullToEmpty(serverId));
        cmd.add("--channel-id");
        cmd.add(nullToEmpty(channelId));
        cmd.add("--game-id");
        cmd.add(nullToEmpty(gameId));
        return cmd;
    }

    private String resolveExePath() throws FileNotFoundException {
        if (exePath != null && !exePath.isBlank()) {
            File f = new File(exePath);
            if (f.exists()) {
                return f.getAbsolutePath();
            }
            LOG.warn("Custom exe path not found: {}", exePath);
        }

        File defaultPath = new File(System.getProperty("user.dir"),
                "helper" + File.separator + EXE_NAME);
        if (defaultPath.exists()) {
            return defaultPath.getAbsolutePath();
        }

        throw new FileNotFoundException(
                "web-kujiequ.exe not found. Expected at: " + defaultPath.getAbsolutePath()
                        + "\nUse setExePath() to configure a custom path.");
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String redactCmd(List<String> cmd) {
        List<String> copy = new ArrayList<>(cmd);
        for (int i = 0; i < copy.size() - 1; i++) {
            String a = copy.get(i);
            if ("--token".equals(a) || "--did".equals(a)) {
                copy.set(i + 1, "***");
            }
        }
        return String.join(" ", copy);
    }
}
