package cn.tealc.wutheringwavestool.ui.kujiequ.web;

import cn.tealc.wutheringwavestool.model.webkujiequ.AuthConfig;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Kuro H5 WebView 壳：UA + 每次加载成功后注入 bridge/auth。
 * <p>
 * 内存注意：调用 {@link #dispose()} 后不可再使用；关窗请至少 {@link #loadBlank()}。
 */
public final class KuroWebShell {
    private static final Logger LOG = LoggerFactory.getLogger(KuroWebShell.class);

    /** Android 库街区 WebView UA（与 headers.rs 一致）。 */
    public static final String USER_AGENT =
            "Mozilla/5.0 (Linux; Android 9; PJH110 Build/PQ3B.190801.04011825; wv) "
                    + "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 "
                    + "Chrome/91.0.4472.114 Mobile Safari/537.36 "
                    + "Kuro/3.0.1 KuroGameBox/3.0.1";

    private WebView webView;
    private WebEngine engine;
    private AuthConfig auth = new AuthConfig();
    private String lastEntry = "";
    private long navId;
    private boolean injectArmed;
    private boolean disposed;
    private BiConsumer<Worker.State, String> loadStateHandler;

    private ChangeListener<Worker.State> stateListener;
    private ChangeListener<Object> documentListener;

    public KuroWebShell() {
        webView = new WebView();
        // 限制缓存，降低长时间打开后的内存占用
        webView.setContextMenuEnabled(false);
        engine = webView.getEngine();
        engine.setUserAgent(USER_AGENT);
        engine.setJavaScriptEnabled(true);

        stateListener = (obs, old, state) -> {
            if (disposed) {
                return;
            }
            String loc = safeLocation();
            LOG.debug("load state={} url={}", state, loc);
            if (loadStateHandler != null) {
                try {
                    loadStateHandler.accept(state, loc);
                } catch (Exception e) {
                    LOG.debug("loadStateHandler error: {}", e.getMessage());
                }
            }
            if (state == Worker.State.SUCCEEDED) {
                if (loc.startsWith("http://") || loc.startsWith("https://")) {
                    injectBridgeAndAuth("page_load");
                }
            } else if (state == Worker.State.FAILED) {
                Throwable ex = engine.getLoadWorker().getException();
                LOG.warn("load FAILED: {}", ex != null ? ex.getMessage() : "unknown");
            }
        };
        engine.getLoadWorker().stateProperty().addListener(stateListener);

        documentListener = (obs, oldDoc, doc) -> {
            if (disposed || doc == null || !injectArmed) {
                return;
            }
            String loc = safeLocation();
            if (loc.startsWith("http://") || loc.startsWith("https://")) {
                Platform.runLater(() -> {
                    if (!disposed) {
                        injectBridgeAndAuth("document");
                    }
                });
            }
        };
        engine.documentProperty().addListener(documentListener);
    }

    public WebView getWebView() {
        ensureAlive();
        return webView;
    }

    public WebEngine getEngine() {
        ensureAlive();
        return engine;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public AuthConfig getAuth() {
        return auth.copy();
    }

    public String getLastEntry() {
        return lastEntry;
    }

    public long getNavId() {
        return navId;
    }

    public String getLocation() {
        return safeLocation();
    }

    public Worker.State getLoadState() {
        if (disposed || engine == null) {
            return Worker.State.READY;
        }
        return engine.getLoadWorker().getState();
    }

    public void setLoadStateHandler(BiConsumer<Worker.State, String> loadStateHandler) {
        this.loadStateHandler = loadStateHandler;
    }

    public void setAuth(AuthConfig next) {
        this.auth = next == null ? new AuthConfig() : next.copy().normalize();
    }

    /**
     * 打开页面。
     *
     * @param forceReload 经 about:blank 跳转，避免 SPA/同 URL 粘用户
     */
    public long open(String entryUrl, AuthConfig nextAuth, boolean forceReload) {
        ensureAlive();
        if (nextAuth != null) {
            setAuth(nextAuth);
        }
        String entry = entryUrl == null || entryUrl.isBlank() ? "about:blank" : entryUrl.trim();
        lastEntry = entry.equals("about:blank") ? "" : entry;
        navId++;
        injectArmed = true;

        LOG.info("OPEN nav_id={} url={} forceReload={}", navId, entry, forceReload);

        tryEval(buildApplyAuthScript(auth), "pre-nav apply");

        Runnable loadTarget = () -> {
            if (disposed) {
                return;
            }
            injectArmed = true;
            engine.load(entry);
        };

        if (forceReload) {
            engine.load("about:blank");
            Platform.runLater(loadTarget);
        } else {
            loadTarget.run();
        }
        return navId;
    }

    public long open(String entryUrl, AuthConfig nextAuth) {
        return open(entryUrl, nextAuth, true);
    }

    public long applyAuthAndReload(AuthConfig nextAuth) {
        ensureAlive();
        setAuth(nextAuth);
        tryEval(buildApplyAuthScript(auth), "auth apply");
        if (lastEntry != null && !lastEntry.isBlank()) {
            LOG.info("AUTH reload url={}", lastEntry);
            return open(lastEntry, null, true);
        }
        navId++;
        LOG.info("AUTH applied (no page open) nav_id={}", navId);
        return navId;
    }

    public void reload() {
        ensureAlive();
        if (lastEntry != null && !lastEntry.isBlank()) {
            open(lastEntry, null, true);
        } else if (engine != null) {
            engine.reload();
        }
    }

    /** 释放页面 DOM/JS 堆，保留 WebView 节点（关窗时用）。 */
    public void loadBlank() {
        if (disposed || engine == null) {
            return;
        }
        injectArmed = false;
        lastEntry = "";
        try {
            // 尽量清掉页面侧缓存，减轻串号与内存
            tryEval("""
                    (function(){
                      try {
                        if (typeof window.__kjqClearUserCaches === 'function') {
                          window.__kjqClearUserCaches();
                        }
                        localStorage.clear();
                        sessionStorage.clear();
                      } catch (e) {}
                    })();
                    """, "clear-storage");
        } catch (Exception ignored) {
            // blank 前页面可能已不可用
        }
        try {
            engine.getLoadWorker().cancel();
        } catch (Exception ignored) {
        }
        engine.load("about:blank");
        LOG.debug("cleared to about:blank");
    }

    /**
     * 完整释放 WebView，避免泄漏。调用后本实例不可再用。
     * 必须在 FX 线程调用。
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        injectArmed = false;
        loadStateHandler = null;
        lastEntry = "";

        if (engine != null) {
            try {
                if (stateListener != null) {
                    engine.getLoadWorker().stateProperty().removeListener(stateListener);
                }
                if (documentListener != null) {
                    engine.documentProperty().removeListener(documentListener);
                }
            } catch (Exception e) {
                LOG.debug("remove listeners: {}", e.getMessage());
            }
            try {
                engine.getLoadWorker().cancel();
            } catch (Exception ignored) {
            }
            try {
                engine.load("about:blank");
            } catch (Exception ignored) {
            }
            try {
                // 切断文档，帮助 GC
                engine.loadContent("");
            } catch (Exception ignored) {
            }
            try {
                engine.setOnAlert(null);
                engine.setConfirmHandler(null);
                engine.setPromptHandler(null);
                engine.setCreatePopupHandler(null);
            } catch (Exception ignored) {
            }
        }

        if (webView != null) {
            try {
                if (webView.getParent() instanceof javafx.scene.layout.Pane pane) {
                    pane.getChildren().remove(webView);
                }
            } catch (Exception ignored) {
            }
            try {
                webView.setOnKeyPressed(null);
                webView.setOnKeyReleased(null);
                webView.setOnMouseClicked(null);
                // 缩减历史，避免持有页面快照；entries 列表通常不可 clear
                if (engine != null) {
                    engine.getHistory().setMaxSize(1);
                }
            } catch (Exception ignored) {
            }
        }

        stateListener = null;
        documentListener = null;
        engine = null;
        webView = null;
        LOG.info("KuroWebShell disposed");
    }


    // -------------------------------------------------------------------------
    // Bridge / scrollbar scripts (merged from former BridgeScripts)
    // -------------------------------------------------------------------------

    private static final String BRIDGE_RESOURCE =
            "/web/bridge-mock.js";
    private static final String SCROLLBAR_RESOURCE =
            "/web/scrollbar.js";

    private static final String BRIDGE_TEMPLATE = readResource(BRIDGE_RESOURCE);
    private static final String SCROLLBAR_SCRIPT = readResource(SCROLLBAR_RESOURCE);

    private static String buildInitScript(AuthConfig auth) {
        String bridge = BRIDGE_TEMPLATE.replace("__KJQ_AUTH_JSON__", auth.toAuthJson());
        return SCROLLBAR_SCRIPT + "\n" + bridge;
    }

    private static String buildApplyAuthScript(AuthConfig auth) {
        String payload = auth.toAuthJson();
        return """
                (function(){
                  var next = %s;
                  window.__kjqAuth = Object.assign({}, next);
                  if (typeof window.__kjqApplyAuth === "function") {
                    window.__kjqApplyAuth(next, { clearFirst: true });
                  } else {
                    try {
                      if (next.token) localStorage.setItem("token", next.token);
                      if (next.userId) localStorage.setItem("userId", next.userId);
                      if (next.roleId) {
                        localStorage.setItem("roleId", next.roleId);
                        localStorage.setItem("mc_roleId", next.roleId);
                      }
                      if (next.serverId) {
                        localStorage.setItem("serverId", next.serverId);
                        localStorage.setItem("mc_serverId", next.serverId);
                      }
                      localStorage.setItem("initUserInfo", JSON.stringify(next));
                      localStorage.setItem("userInfo", JSON.stringify(next));
                    } catch (e) {}
                  }
                  console.log("[webkujiequ-fx] auth applied", {
                    hasToken: !!next.token,
                    userId: next.userId || null,
                    roleId: next.roleId || null
                  });
                })();
                """.formatted(payload);
    }

    private static String readResource(String path) {
        try (InputStream in = KuroWebShell.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("missing resource: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("read resource failed: " + path, e);
        }
    }

    private void injectBridgeAndAuth(String reason) {
        if (disposed) {
            return;
        }
        String init = buildInitScript(auth);
        if (tryEval(init, "init/" + reason)) {
            LOG.debug("bridge+auth injected ({}) nav_id={}", reason, navId);
        }
    }

    private boolean tryEval(String script, String label) {
        if (disposed || engine == null) {
            return false;
        }
        try {
            engine.executeScript(script);
            return true;
        } catch (Exception e) {
            LOG.debug("eval failed ({}): {}", label, e.getMessage());
            return false;
        }
    }

    private String safeLocation() {
        if (disposed || engine == null) {
            return "";
        }
        try {
            String loc = engine.getLocation();
            return loc == null ? "" : loc;
        } catch (Exception e) {
            return "";
        }
    }

    private void ensureAlive() {
        if (disposed) {
            throw new IllegalStateException("KuroWebShell already disposed");
        }
    }

    @Override
    public String toString() {
        return "KuroWebShell{navId=" + navId
                + ", lastEntry=" + lastEntry
                + ", disposed=" + disposed
                + ", auth=" + Objects.toString(auth)
                + "}";
    }
}
