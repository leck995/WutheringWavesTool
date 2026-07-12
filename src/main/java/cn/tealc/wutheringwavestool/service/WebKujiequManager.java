package cn.tealc.wutheringwavestool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kuro.kujiequ.model.sign.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * web-kujiequ.exe 守护进程管理器
 * <p>
 * 管理 web-kujiequ.exe 的完整生命周期，提供快捷方法打开库街区 H5 页面。
 * 使用 --daemon 模式启动，通过 stdin/stdout 与进程通信。
 * 窗口关闭即进程退出，下次调用时自动重新启动。
 * </p>
 *
 * @author Leck
 */
@Singleton
public class WebKujiequManager {
    private static final Logger LOG = LoggerFactory.getLogger(WebKujiequManager.class);

    /** 固定 serverId */
    private static final String SERVER_ID = "76402e5b20be2c39f095a152090afddc";
    /** 固定 channelId */
    private static final String CHANNEL_ID = "19";
    /** EXE 文件名 */
    private static final String EXE_NAME = "web-kujiequ.exe";
    /** 等待守护进程就绪的超时时间 */
    private static final long START_TIMEOUT_SECONDS = 10;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Process process;
    private BufferedWriter stdinWriter;
    private Thread stdoutReaderThread;
    private Thread stderrReaderThread;
    private final ReentrantLock writeLock = new ReentrantLock();

    /** 当前用户认证参数 */
    private String token;
    private String userId;
    private String roleId;
    private String did;

    /** 自定义 exe 路径，为空则使用默认 helper/ 目录 */
    private String exePath;

    public WebKujiequManager() {
    }

    // ==================== 配置方法 ====================

    /**
     * 设置当前用户信息，提取 token / userId / roleId / devCode→did
     *
     * @param userInfo 用户信息
     */
    public void setUserInfo(UserInfo userInfo) {
        if (userInfo == null) {
            LOG.warn("setUserInfo: userInfo is null, clearing auth params");
            this.token = null;
            this.userId = null;
            this.roleId = null;
            this.did = null;
            return;
        }
        this.token = userInfo.getToken();
        this.userId = userInfo.getUserId();
        this.roleId = userInfo.getRoleId();
        this.did = userInfo.getDevCode();
        LOG.info("UserInfo set: userId={}, roleId={}", userId, roleId);
    }

    /**
     * 设置自定义 exe 路径
     *
     * @param exePath web-kujiequ.exe 的绝对路径
     */
    public void setExePath(String exePath) {
        this.exePath = exePath;
    }

    // ==================== 生命周期方法 ====================

    /**
     * 启动守护进程。如果进程已在运行则直接返回。
     *
     * @throws IOException 如果 exe 找不到或启动超时
     */
    public synchronized void start() throws IOException {
        if (isRunning()) {
            LOG.debug("WebKujiequ daemon already running");
            return;
        }

        cleanup();

        String exe = resolveExePath();
        LOG.info("Starting WebKujiequ daemon: {}", exe);

        ProcessBuilder pb = new ProcessBuilder(exe, "--daemon");
        process = pb.start();

        stdinWriter = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

        // 用于等待 "OK ready"
        CountDownLatch readyLatch = new CountDownLatch(1);
        StringBuilder errorMessage = new StringBuilder();

        // 读取 stdout
        stdoutReaderThread = Thread.startVirtualThread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOG.debug("WebKujiequ: {}", line);
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

        // 读取 stderr
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

        // 等待守护进程就绪
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
     * 停止守护进程。发送 quit 命令并等待进程退出。
     */
    public void stop() {
        if (!isRunning()) {
            LOG.debug("WebKujiequ daemon not running");
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

        // 等待进程退出
        try {
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (process != null && process.isAlive()) {
            LOG.warn("WebKujiequ did not exit gracefully, force killing");
            process.destroyForcibly();
        }

        cleanup();
        LOG.info("WebKujiequ daemon stopped");
    }

    /**
     * 重启守护进程
     */
    public void restart() throws IOException {
        stop();
        start();
    }

    /**
     * 检查守护进程是否正在运行
     *
     * @return true 如果进程存活
     */
    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    // ==================== 快捷页面方法 ====================

    /**
     * 打开数据终端 (mc-role-box)
     */
    public void openRoleBox() throws IOException {
        openPage("mc-role-box");
    }

    /**
     * 打开资源简报 (resource-briefing)
     */
    public void openResourceBriefing() throws IOException {
        openPage("resource-briefing");
    }

    /**
     * 打开活动日历 (mccalendar)
     */
    public void openCalendar() throws IOException {
        openPage("mccalendar");
    }

    /**
     * 打开养成计算器 (growth-calculator)
     */
    public void openGrowthCalculator() throws IOException {
        openPage("growth-calculator");
    }

    /**
     * 打开每日签到 (mc-month-sign)
     */
    public void openMonthSign() throws IOException {
        openPage("mc-month-sign");
    }

    /**
     * 打开指定内置页面。先发送 AUTH 命令确保认证上下文就绪，再打开页面。
     *
     * @param pageName 页面名称，例如 mc-role-box, resource-briefing 等
     */
    public void openPage(String pageName) throws IOException {
        if (!isRunning()) {
            start();
        }
        sendAuthCommand();
        sendCommand(buildOpenCommand(pageName, null));
    }

    /**
     * 打开自定义 URL。先发送 AUTH 命令确保认证上下文就绪，再打开页面。
     *
     * @param url 完整 URL，支持 hash 路由
     */
    public void openUrl(String url) throws IOException {
        if (!isRunning()) {
            start();
        }
        sendAuthCommand();
        sendCommand(buildOpenCommand(null, url));
    }

    // ==================== 内部方法 ====================

    /**
     * 解析 exe 路径：自定义路径 > user.dir/helper/web-kujiequ.exe
     */
    private String resolveExePath() throws FileNotFoundException {
        if (exePath != null && !exePath.isBlank()) {
            File f = new File(exePath);
            if (f.exists()) return f.getAbsolutePath();
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

    /**
     * 构建 auth 命令 JSON，先发送确保守护进程有认证上下文
     */
    private ObjectNode buildAuthCommand() {
        ObjectNode cmd = objectMapper.createObjectNode();
        cmd.put("cmd", "auth");
        if (token != null) cmd.put("token", token);
        if (did != null) cmd.put("did", did);
        if (userId != null) cmd.put("userId", userId);
        if (roleId != null) cmd.put("roleId", roleId);
        cmd.put("serverId", SERVER_ID);
        cmd.put("channelId", CHANNEL_ID);
        return cmd;
    }

    private void sendAuthCommand() throws IOException {
        sendCommand(buildAuthCommand());
    }

    /**
     * 构建 open 命令 JSON
     */
    private ObjectNode buildOpenCommand(String pageName, String url) {
        ObjectNode cmd = objectMapper.createObjectNode();
        cmd.put("cmd", "open");
        if (pageName != null) {
            cmd.put("page", pageName);
        }
        if (url != null) {
            cmd.put("url", url);
        }
        if (token != null) cmd.put("token", token);
        if (did != null) cmd.put("did", did);
        if (userId != null) cmd.put("userId", userId);
        if (roleId != null) cmd.put("roleId", roleId);
        cmd.put("serverId", SERVER_ID);
        cmd.put("channelId", CHANNEL_ID);
        return cmd;
    }

    /**
     * 向守护进程 stdin 发送 JSON 命令（线程安全）
     */
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

    /**
     * 清理进程相关资源
     */
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
