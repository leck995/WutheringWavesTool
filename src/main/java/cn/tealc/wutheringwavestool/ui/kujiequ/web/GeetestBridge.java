package cn.tealc.wutheringwavestool.ui.kujiequ.web;

import javafx.application.Platform;

import java.util.Objects;

/**
 * WebView 页面通过 window.javaBridge 回调到 Java。
 */
public final class GeetestBridge {

    public interface Listener {
        void onReady();

        void onSuccess(String geeTestJson);

        void onError(String message);

        void onClose();
    }

    private final Listener listener;

    public GeetestBridge(Listener listener) {
        this.listener = Objects.requireNonNull(listener);
    }

    public void onReady(String ignored) {
        Platform.runLater(listener::onReady);
    }

    public void onSuccess(String geeTestJson) {
        Platform.runLater(() -> listener.onSuccess(geeTestJson));
    }

    public void onError(String message) {
        Platform.runLater(() -> listener.onError(message == null ? "未知错误" : message));
    }

    public void onClose(String ignored) {
        Platform.runLater(listener::onClose);
    }
}
