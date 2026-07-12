package cn.tealc.wutheringwavestool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Singleton;
import com.kuro.kujiequ.model.sign.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * web-kujiequ.exe 守护进程管理器。
 * <p>
 * 使用 --daemon 模式启动，通过 stdin 发送 JSON 命令热切换页面/用户。
 * 窗口关闭即进程退出；下次调用时自动重新启动。
 * 启动时通过 CLI 参数注入认证信息，运行中通过 stdin auth 热切换。
 * </p>
 *
 * @author Leck
 */
@Singleton
public class WebKujiequManager {
    private static final Logger LOG = LoggerFactory.getLogger(WebKujiequManager.class);

    private static final String SERVER_ID = "76402e5b20be2c39f095a152090afddc";
    private static final String CHANNEL_ID = "19";
    private static final String GAME_ID = "3";
    private static final String EXE_NAME = "web-kujiequ.exe";
    private static final long START_TIMEOUT_SECONDS = 10;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantLock writeLock = new ReentrantLock();

    private Process process;
    private BufferedWriter stdinWriter;
    private Thread stdoutReaderThread;
    private Thread stderrReaderThread;

    private String token;
    private String userId;
    private String roleId;
    private String did;
    private String exePath;

    public WebKujiequManager() {
    }

    /**
     * 设置当前用户信息。若守护进程已运行则通过 stdin 热切换认证。
     */
    public void setUserInfo(UserInfo userInfo) {
        if (userInfo == null) {
            LOG.warn("setUserInfo: userInfo is null");
            return;
        }
        this.token = userInfo.getToken();
        this.userId = userInfo.getUserId();
        this.roleId = userInfo.getRoleId();
        this.did = userInfo.getDevCode();
        LOG.info("UserInfo set: userId={}, roleId={}", userId, roleId);

        if (isRunning()) {
            try {
                sendAuthCommand();
            } catch (IOException e) {
                LOG.warn("热切换认证失败: {}", e.getMessage());
            }
        }
    }

    public void setExePath(String exePath) {
        this.exePath = exePath;
    }

    /**
     * 启动守护进程。已运行则直接返回。
     */
    public synchronized void start() throws IOException {
        if (isRunning()) {
            LOG.debug("WebKujiequ daemon already running");
            return;
        }

        cleanup();

        String exe = resolveExePath();
        List<String> cmd = buildStartCommand(exe);
        LOG.info("Starting WebKujiequ daemon: {}", exe);
        LOG.debug("Command: {}", String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        process = pb.start();

        stdinWriter = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

        CountDownLatch readyLatch = new CountDownLatch(1);
        StringBuilder errorMessage = new StringBuilder();

        stdoutReaderThread = Thread.startVirtualThread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    //LOG.debug("WebKujiequ: {}", line);
                    if (line.startsWith("OK ready")) {
                        readyLatch.countDown();
                    } else if (line.startsWith("ERR")) {
                        errorMessage.append(line);
                        readyLatch.countDown();
                    }
                }
            } catch (IOException e) {
                LOG.debug("WebKujiequ stdout closed: {}", e.getMessage());
            }
            LOG.info("WebKujiequ daemon process ended");
        });

        stderrReaderThread = Thread.startVirtualThread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOG.warn("WebKujiequ stderr: {}", line);
                }
            } catch (IOException e) {
                // ignore
            }
        });

        try {
            boolean ready = readyLatch.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!ready) {
                throw new IOException("WebKujiequ 守护进程启动超时 (" + START_TIMEOUT_SECONDS + "s)");
            }
            if (errorMessage.length() > 0) {
                throw new IOException("WebKujiequ 启动出错: " + errorMessage);
            }
            LOG.info("WebKujiequ daemon ready");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("等待 WebKujiequ 守护进程时被中断", e);
        }
    }

    /**
     * 停止守护进程。
     */
    public void stop() {
        if (!isRunning()) {
            return;
        }
        LOG.info("Stopping WebKujiequ daemon");
        try {
            ObjectNode cmd = objectMapper.createObjectNode();
            cmd.put("cmd", "quit");
            sendCommand(cmd);
        } catch (IOException e) {
            LOG.warn("Failed to send quit command: {}", e.getMessage());
        }

        try {
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
        cleanup();
        LOG.info("WebKujiequ daemon stopped");
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
     * 打开指定内置页面。
     * 进程存活时通过 stdin 热切换；已关闭则自动重启守护进程。
     */
    public synchronized void openPage(String pageName) throws IOException {
        if (!isRunning()) {
            start();
        }
        sendCommand(buildOpenCommand(pageName, null));
    }

    /**
     * 打开自定义 URL。进程存活时热切换，已关闭则自动重启。
     */
    public synchronized void openUrl(String url) throws IOException {
        if (!isRunning()) {
            start();
        }
        sendCommand(buildOpenCommand(null, url));
    }

    private List<String> buildStartCommand(String exe) {
        List<String> cmd = new ArrayList<>();
        cmd.add(exe);
        cmd.add("--daemon");
        if (token != null && !token.isBlank()) {
            cmd.add("--token");
            cmd.add(token);
        }
        if (did != null && !did.isBlank()) {
            cmd.add("--did");
            cmd.add(did);
        }
        if (userId != null && !userId.isBlank()) {
            cmd.add("--user-id");
            cmd.add(userId);
        }
        if (roleId != null && !roleId.isBlank()) {
            cmd.add("--role-id");
            cmd.add(roleId);
        }
        cmd.add("--server-id");
        cmd.add(SERVER_ID);
        cmd.add("--channel-id");
        cmd.add(CHANNEL_ID);
        cmd.add("--game-id");
        cmd.add(GAME_ID);
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

        File defaultPath = new File(System.getProperty("user.dir"), "helper" + File.separator + EXE_NAME);
        if (defaultPath.exists()) {
            return defaultPath.getAbsolutePath();
        }

        throw new FileNotFoundException(
                "web-kujiequ.exe not found. Expected at: " + defaultPath.getAbsolutePath()
                        + "\nUse setExePath() to configure a custom path.");
    }

    private ObjectNode buildAuthCommand() {
        ObjectNode cmd = objectMapper.createObjectNode();
        cmd.put("cmd", "auth");
        if (token != null) {
            cmd.put("token", token);
        }
        if (did != null) {
            cmd.put("did", did);
        }
        if (userId != null) {
            cmd.put("userId", userId);
        }
        if (roleId != null) {
            cmd.put("roleId", roleId);
        }
        cmd.put("serverId", SERVER_ID);
        cmd.put("channelId", CHANNEL_ID);
        cmd.put("gameId", Integer.parseInt(GAME_ID));
        return cmd;
    }

    private void sendAuthCommand() throws IOException {
        sendCommand(buildAuthCommand());
    }

    private ObjectNode buildOpenCommand(String pageName, String url) {
        ObjectNode cmd = objectMapper.createObjectNode();
        cmd.put("cmd", "open");
        if (pageName != null) {
            cmd.put("page", pageName);
        }
        if (url != null) {
            cmd.put("url", url);
        }
        return cmd;
    }

    private void sendCommand(ObjectNode command) throws IOException {
        writeLock.lock();
        try {
            String json = objectMapper.writeValueAsString(command);
            LOG.debug("Sending: {}", json);
            stdinWriter.write(json);
            stdinWriter.newLine();
            stdinWriter.flush();
        } finally {
            writeLock.unlock();
        }
    }

    private void cleanup() {
        if (stdoutReaderThread != null) {
            stdoutReaderThread.interrupt();
            stdoutReaderThread = null;
        }
        if (stderrReaderThread != null) {
            stderrReaderThread.interrupt();
            stderrReaderThread = null;
        }
        try {
            if (stdinWriter != null) {
                stdinWriter.close();
            }
        } catch (IOException e) {
            // ignore
        }
        stdinWriter = null;
        process = null;
    }
}
