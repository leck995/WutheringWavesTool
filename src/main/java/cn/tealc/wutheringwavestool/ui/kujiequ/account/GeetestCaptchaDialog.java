package cn.tealc.wutheringwavestool.ui.kujiequ.account;

import cn.tealc.wutheringwavestool.FXResourcesLoader;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import netscape.javascript.JSObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GeetestCaptchaDialog {
    private static final String HTML_PATH = "/cn/tealc/wutheringwavestool/ui/geetest.html";

    private final Stage stage;
    private final Consumer<String> onSolved;

    public GeetestCaptchaDialog(Window owner, String gt, String challenge, Consumer<String> onSolved) {
        this.onSolved = onSolved;

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();

        // Set up JS bridge after page loads
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("bridge", new CaptchaBridge());
            }
        });

        // Load HTML with injected parameters
        String html = loadHtml(gt, challenge);
        engine.loadContent(html);

        // Stage setup
        StackPane root = new StackPane(webView);
        Scene scene = new Scene(root, 400, 500);

        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("人机验证");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> onSolved.accept(""));
    }

    public void show() {
        stage.showAndWait();
    }

    private String loadHtml(String gt, String challenge) {
        try (InputStream is = FXResourcesLoader.loadStream(HTML_PATH);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String content = br.lines().collect(Collectors.joining("\n"));
            return content.replace("${gt}", gt != null ? gt : "")
                          .replace("${challenge}", challenge != null ? challenge : "");
        } catch (Exception e) {
            return "<html><body><h1>Failed to load captcha</h1></body></html>";
        }
    }

    // JS bridge - must be public for JSObject access
    public class CaptchaBridge {
        public void onCaptchaSolved(String json) {
            Platform.runLater(() -> {
                onSolved.accept(json);
                stage.close();
            });
        }
    }
}
