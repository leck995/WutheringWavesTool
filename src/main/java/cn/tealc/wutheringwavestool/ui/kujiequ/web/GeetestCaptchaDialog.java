package cn.tealc.wutheringwavestool.ui.kujiequ.web;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 模态极验滑块弹窗：完成后通过回调返回 geeTest JSON。
 */
public final class GeetestCaptchaDialog {

    private static final Logger log = LoggerFactory.getLogger(GeetestCaptchaDialog.class);

    private Stage stage;
    private WebView webView;
    private final AtomicBoolean captchaReady = new AtomicBoolean(false);
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicBoolean showRequested = new AtomicBoolean(false);

    private Consumer<String> onSuccess;
    private Consumer<String> onError;
    private Runnable onCancel;

    public void setOnSuccess(Consumer<String> onSuccess) {
        this.onSuccess = onSuccess;
    }

    public void setOnError(Consumer<String> onError) {
        this.onError = onError;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    public void show(Window owner) {
        completed.set(false);
        captchaReady.set(false);
        showRequested.set(false);

        webView = new WebView();
        webView.setPrefSize(420, 320);
        Label tip = new Label("加载极验中…完成后会自动弹出滑块");
        tip.setWrapText(true);

        VBox box = new VBox(8, tip, webView);
        box.setPadding(new Insets(12));
        VBox.setVgrow(webView, Priority.ALWAYS);

        stage = new Stage();
        stage.setTitle("完成滑块验证");
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setScene(new Scene(box, 460, 400));
        stage.setOnCloseRequest(e -> {
            if (!completed.get()) {
                fireCancel();
            }
        });

        initWebView(webView.getEngine(), tip);
        stage.show();
    }

    public void close() {
        if (stage != null) {
            stage.close();
            stage = null;
        }
        webView = null;
        captchaReady.set(false);
        showRequested.set(false);
    }

    public void resetCaptcha() {
        if (webView == null) {
            return;
        }
        try {
            webView.getEngine().executeScript("window.resetCaptcha && window.resetCaptcha()");
        } catch (Exception e) {
            log.debug("resetCaptcha failed", e);
        }
    }

    private void initWebView(WebEngine engine, Label tip) {
        engine.setJavaScriptEnabled(true);
        engine.setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        );

        engine.getLoadWorker().stateProperty().addListener((obs, o, state) -> {
            if (state == Worker.State.FAILED) {
                Throwable ex = engine.getLoadWorker().getException();
                String msg = "页面加载失败" + (ex == null ? "" : "：" + ex.getMessage());
                tip.setText(msg);
                fireError(msg);
                return;
            }
            if (state != Worker.State.SUCCEEDED) {
                return;
            }
            installBridge(engine, tip);
        });

        URL html = Objects.requireNonNull(
                GeetestCaptchaDialog.class.getResource("/web/geetest.html"),
                "缺少 /web/geetest.html"
        );
        engine.load(html.toExternalForm() + "?t=" + System.currentTimeMillis());
    }

    private void installBridge(WebEngine engine, Label tip) {
        try {
            JSObject window = (JSObject) engine.executeScript("window");
            window.setMember("javaBridge", new GeetestBridge(new GeetestBridge.Listener() {
                @Override
                public void onReady() {
                    captchaReady.set(true);
                    tip.setText("极验已就绪，正在弹出滑块…");
                    showCaptchaBox(engine, tip);
                }

                @Override
                public void onSuccess(String geeTestJson) {
                    if (!completed.compareAndSet(false, true)) {
                        return;
                    }
                    tip.setText("验证通过");
                    Consumer<String> cb = onSuccess;
                    close();
                    if (cb != null) {
                        cb.accept(geeTestJson);
                    }
                }

                @Override
                public void onError(String message) {
                    tip.setText("极验失败：" + message);
                    // 不立即关闭，允许用户重试；若尚未完成则通知
                    fireError(message);
                }

                @Override
                public void onClose() {
                    // 用户关掉滑块框（不是关掉整个弹窗）
                    if (!completed.get()) {
                        fireCancel();
                        close();
                    }
                }
            }));

            Object synced = engine.executeScript(
                    "window.syncReadyToJava ? window.syncReadyToJava() : false"
            );
            if (Boolean.TRUE.equals(synced)) {
                captchaReady.set(true);
                tip.setText("极验已就绪，正在弹出滑块…");
                showCaptchaBox(engine, tip);
            } else {
                tip.setText("页面已加载，等待极验初始化…");
                pollAndShow(engine, tip, 0);
            }
        } catch (Exception e) {
            log.error("安装极验桥接失败", e);
            tip.setText("安装桥接失败：" + e.getMessage());
            fireError("安装桥接失败：" + e.getMessage());
        }
    }

    private void pollAndShow(WebEngine engine, Label tip, int attempt) {
        if (attempt > 40 || captchaReady.get() || completed.get()) {
            return;
        }
        Platform.runLater(() -> {
            if (completed.get() || captchaReady.get()) {
                return;
            }
            try {
                Object ready = engine.executeScript(
                        "window.isCaptchaReady ? window.isCaptchaReady() : false"
                );
                if (Boolean.TRUE.equals(ready)) {
                    captchaReady.set(true);
                    tip.setText("极验已就绪，正在弹出滑块…");
                    showCaptchaBox(engine, tip);
                    return;
                }
            } catch (Exception ignored) {
            }
            if (attempt < 40) {
                Thread t = new Thread(() -> {
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    pollAndShow(engine, tip, attempt + 1);
                }, "captcha-poll");
                t.setDaemon(true);
                t.start();
            } else {
                tip.setText("极验初始化超时，请关闭后重试");
                fireError("极验初始化超时");
            }
        });
    }

    private void showCaptchaBox(WebEngine engine, Label tip) {
        if (!showRequested.compareAndSet(false, true)) {
            return;
        }
        try {
            Object ok = engine.executeScript("window.showCaptcha && window.showCaptcha()");
            if (Boolean.FALSE.equals(ok)) {
                tip.setText("无法弹出滑块，请关闭后重试");
                fireError("无法弹出滑块");
            }
        } catch (Exception e) {
            tip.setText("调用滑块失败：" + e.getMessage());
            fireError("调用滑块失败：" + e.getMessage());
        }
    }

    private void fireError(String message) {
        // 仅首次致命错误时通知；滑块失败仍可能 reset 重试，这里只回调 onError 供 UI toast
        Consumer<String> cb = onError;
        if (cb != null) {
            cb.accept(message == null ? "未知错误" : message);
        }
    }

    private void fireCancel() {
        if (!completed.compareAndSet(false, true)) {
            return;
        }
        Runnable cb = onCancel;
        if (cb != null) {
            cb.run();
        }
    }
}
