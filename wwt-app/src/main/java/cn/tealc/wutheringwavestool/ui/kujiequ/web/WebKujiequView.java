package cn.tealc.wutheringwavestool.ui.kujiequ.web;

import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.WwtApp;
import cn.tealc.wutheringwavestool.model.webkujiequ.PagePreset;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedAL;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 库街区 H5 手机窗（MVVM View）：HeaderBar + 页面切换 + WebView。
 * <p>
 * 生命周期：
 * <ul>
 *   <li>关闭按钮 / 关窗 → hide + {@link KuroWebShell#loadBlank()}（保留引擎，减泄漏）</li>
 *   <li>{@link #dispose()} → 销毁 Stage 与 WebView（应用退出）</li>
 * </ul>
 */
public final class WebKujiequView {
    private static final Logger LOG = LoggerFactory.getLogger(WebKujiequView.class);

    public static final double DEFAULT_WIDTH = 420;
    public static final double DEFAULT_HEIGHT = 820;

    private final KuroWebShell shell;
    private final Map<PagePreset, Button> pageButtons = new EnumMap<>(PagePreset.class);

    private Stage stage;
    private Label titleLabel;
    private BorderPane root;
    private StackPane webHost;
    private PagePreset activePreset;
    private Consumer<PagePreset> pageOpenHandler;
    private boolean disposed;

    public WebKujiequView(KuroWebShell shell) {
        this.shell = shell;
    }

    public KuroWebShell getShell() {
        return shell;
    }

    public Stage getStage() {
        return stage;
    }

    public boolean isShowing() {
        return stage != null && stage.isShowing() && !disposed;
    }

    public boolean isDisposed() {
        return disposed;
    }

    /** 标题栏页面图标点击时回调（由 Manager 打开对应 H5）。 */
    public void setPageOpenHandler(Consumer<PagePreset> pageOpenHandler) {
        this.pageOpenHandler = pageOpenHandler;
    }

    /** 在 FX 线程创建 UI（幂等）。 */
    public void ensureCreated() {
        if (disposed) {
            throw new IllegalStateException("WebKujiequView already disposed");
        }
        if (stage != null) {
            return;
        }
        ensureFxThread();

        titleLabel = new Label("库街区");
        titleLabel.getStyleClass().add("title");

        HBox pageNav = buildPageNav();
        pageNav.getStyleClass().add("webkujiequ-page-nav");

        HBox leadingBox = new HBox(8, titleLabel, pageNav);
        leadingBox.setAlignment(Pos.CENTER_LEFT);
        leadingBox.getStyleClass().add("leading");

        Button refreshBtn = new Button(null, new FontIcon(Material2OutlinedMZ.REFRESH));
        refreshBtn.setTooltip(new Tooltip("刷新"));
        refreshBtn.setFocusTraversable(false);
        refreshBtn.setOnAction(e -> shell.reload());

        Button closeBtn = new Button(null, new FontIcon(Material2OutlinedAL.CLOSE));
        closeBtn.getStyleClass().add("close-btn");
        closeBtn.setFocusTraversable(false);
        closeBtn.setTooltip(new Tooltip("关闭"));
        closeBtn.setOnAction(e -> hideAndReleasePage());

        HBox systemBox = new HBox(closeBtn);
        systemBox.getStyleClass().add("system-func");

        HBox trailingBox = new HBox(refreshBtn, systemBox);
        trailingBox.getStyleClass().add("trailing");

        HeaderBar headerBar = new HeaderBar();
        headerBar.getStyleClass().addAll("headbar", "webkujiequ-header");
        headerBar.setLeft(leadingBox);
        headerBar.setRight(trailingBox);
        HBox.setHgrow(headerBar, Priority.ALWAYS);

        webHost = new StackPane(shell.getWebView());
        webHost.getStyleClass().add("webkujiequ-web-host");

        root = new BorderPane();
        root.setTop(headerBar);
        root.setCenter(webHost);
        root.getStyleClass().add("webkujiequ-root");

        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        try {
            var defaultCss = FXResourcesLoader.loadURL("css/Default.css");
            if (defaultCss != null) {
                scene.getStylesheets().add(defaultCss.toExternalForm());
            }
            var cssUrl = FXResourcesLoader.loadURL("css/kujiequ/WebKujiequ.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) {
            LOG.debug("load stylesheets failed: {}", e.getMessage());
        }
        try {
            Scene mainScene = WwtApp.getWindow() != null ? WwtApp.getWindow().getScene() : null;
            if (mainScene != null && mainScene.getRoot() != null) {
                String font = mainScene.getRoot().getStyle();
                if (font != null && !font.isBlank()) {
                    root.setStyle(font);
                }
            }
        } catch (Exception ignored) {
        }

        // 与主窗口一致：EXTENDED + HeaderBar 接管系统标题区
        stage = new Stage(StageStyle.EXTENDED);
        Window owner = WwtApp.getWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setScene(scene);
        stage.setMinWidth(360);
        stage.setMinHeight(600);
        stage.setWidth(DEFAULT_WIDTH);
        stage.setHeight(DEFAULT_HEIGHT);
        stage.setTitle("库街区");
        stage.setOnCloseRequest(e -> {
            e.consume();
            hideAndReleasePage();
        });

        Platform.runLater(() -> HeaderBar.setPrefButtonHeight(stage, 0));

        LOG.info("WebKujiequView created (HeaderBar + page nav)");
    }

    public void setTitle(String title) {
        String t = title == null || title.isBlank() ? "库街区" : title.trim();
        if (titleLabel != null) {
            titleLabel.setText(t);
        }
        if (stage != null) {
            stage.setTitle(t);
        }
    }

    /** 同步标题栏当前页高亮。 */
    public void setActivePreset(PagePreset preset) {
        activePreset = preset;
        for (Map.Entry<PagePreset, Button> e : pageButtons.entrySet()) {
            boolean selected = preset != null && e.getKey() == preset;
            e.getValue().pseudoClassStateChanged(
                    javafx.css.PseudoClass.getPseudoClass("selected"), selected);
            if (selected) {
                if (!e.getValue().getStyleClass().contains("selected")) {
                    e.getValue().getStyleClass().add("selected");
                }
            } else {
                e.getValue().getStyleClass().remove("selected");
            }
        }
        if (preset != null) {
            setTitle(preset.title());
        }
    }

    public void showAndFocus() {
        ensureCreated();
        if (!stage.isShowing()) {
            stage.show();
        }
        stage.setIconified(false);
        stage.toFront();
        stage.requestFocus();
    }

    /** 关窗：隐藏 + 卸页面，不销毁 WebView。 */
    public void hideAndReleasePage() {
        if (disposed) {
            return;
        }
        Runnable action = () -> {
            try {
                shell.loadBlank();
            } catch (Exception e) {
                LOG.debug("loadBlank on hide: {}", e.getMessage());
            }
            if (stage != null && stage.isShowing()) {
                stage.hide();
            }
            LOG.debug("WebKujiequView hidden, page released");
        };
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    /** 应用退出：销毁 Stage 与 WebView。 */
    public void dispose() {
        if (disposed) {
            return;
        }
        Runnable action = () -> {
            if (disposed) {
                return;
            }
            disposed = true;
            try {
                shell.dispose();
            } catch (Exception e) {
                LOG.warn("shell dispose: {}", e.getMessage());
            }
            if (webHost != null) {
                webHost.getChildren().clear();
            }
            if (stage != null) {
                try {
                    stage.setOnCloseRequest(null);
                    stage.setScene(null);
                    stage.close();
                } catch (Exception e) {
                    LOG.debug("stage close: {}", e.getMessage());
                }
            }
            pageButtons.clear();
            pageOpenHandler = null;
            stage = null;
            root = null;
            webHost = null;
            titleLabel = null;
            activePreset = null;
            LOG.info("WebKujiequView disposed");
        };
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    private HBox buildPageNav() {
        HBox nav = new HBox(2);
        nav.setAlignment(Pos.CENTER_LEFT);
        addPageButton(nav, PagePreset.MC_ROLE_BOX, Material2OutlinedAL.DASHBOARD);
        addPageButton(nav, PagePreset.RESOURCE_BRIEFING, Material2OutlinedAL.ASSESSMENT);
        addPageButton(nav, PagePreset.MC_CALENDAR, Material2OutlinedAL.EVENT);
        addPageButton(nav, PagePreset.GROWTH_CALCULATOR, Material2OutlinedAL.CALCULATE);
        addPageButton(nav, PagePreset.MC_MONTH_SIGN, Material2OutlinedAL.CARD_GIFTCARD);
        return nav;
    }

    private void addPageButton(HBox nav, PagePreset preset, Ikon ikon) {
        Button btn = new Button(null, new FontIcon(ikon));
        btn.getStyleClass().addAll("button-icon", "flat", "webkujiequ-page-btn");
        btn.setTooltip(new Tooltip(preset.title()));
        btn.setFocusTraversable(false);
        btn.setOnAction(e -> {
            if (activePreset == preset) {
                // 已在当前页：刷新
                shell.reload();
                return;
            }
            Consumer<PagePreset> handler = pageOpenHandler;
            if (handler != null) {
                handler.accept(preset);
            }
        });
        pageButtons.put(preset, btn);
        nav.getChildren().add(btn);
    }

    private static void ensureFxThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("must be on JavaFX Application Thread");
        }
    }
}
