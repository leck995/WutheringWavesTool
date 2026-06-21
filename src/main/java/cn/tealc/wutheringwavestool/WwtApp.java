package cn.tealc.wutheringwavestool;

import ch.qos.logback.classic.Level;
import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.dao.JdbcUtils;
import cn.tealc.wutheringwavestool.jna.GlobalKeyListener;
import cn.tealc.wutheringwavestool.service.GameWindowMonitorService;
import cn.tealc.wutheringwavestool.service.TokenRefreshService;
import cn.tealc.wutheringwavestool.thread.system.ClearLogFileTask;
import cn.tealc.wutheringwavestool.ui.system.MainView;
import cn.tealc.wutheringwavestool.ui.system.MainViewModel;
import cn.tealc.wutheringwavestool.ui.system.tray.TrayIconManager;
import cn.tealc.wutheringwavestool.util.AppLocked;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WwtApp extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(WwtApp.class);
    private static Stage window;
    private static AppLocked appLocked;
    private static boolean autoStarted = false;

    public WwtApp() {
        MvvmFX.setGlobalResourceBundle(Config.language);
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.toLevel(Config.setting().getLogLevel()));
        Platform.setImplicitExit(false);
        appLocked = new AppLocked();
    }

    @Override
    public void start(Stage stage) throws IOException {
        autoStarted = getParameters().getRaw().contains("--auto-start");
        JdbcUtils.init();
        AppInjector.getInjector();
        VersionUpdateUtil.update();
        window = stage;
        stage.initStyle(StageStyle.EXTENDED);
        setupStage(stage);
        initFont();
        if (Config.setting().isSilentStart()) {
            stage.hide();
            LOG.info("静默启动，窗口已隐藏");
        }else {
            Platform.runLater(stage::show);
        }

        AppInjector.getInstance(GameWindowMonitorService.class).start();
        AppInjector.getInstance(TokenRefreshService.class).start();
        AppInjector.getInstance(TrayIconManager.class).install(stage);
        Thread.startVirtualThread(new ClearLogFileTask());
        Thread.setDefaultUncaughtExceptionHandler((t, e) ->
                LOG.error("线程：{}，出现异常：{}", t.getName(), e.getMessage(), e));

        if (!autoStarted && Config.setting().isAutoStartGame()) {
            PauseTransition delay = new PauseTransition(javafx.util.Duration.seconds(2));
            delay.setOnFinished(e -> MvvmFX.getNotificationCenter().publish(NotificationKey.HOME_AUTO_START_GAME));
            delay.play();
        }
    }

    private void setupStage(Stage stage) {
        Application.setUserAgentStylesheet(FXResourcesLoader.load("css/light.css"));
        ViewTuple<MainView, MainViewModel> viewTuple = FluentViewLoader.fxmlView(MainView.class).load();

        Scene scene = new Scene(viewTuple.getView());
        scene.getStylesheets().add(FXResourcesLoader.load("css/Default.css"));
        stage.setScene(scene);
        stage.getIcons().add(new Image(FXResourcesLoader.load("image/icon.png"), 45, 45, true, true));
        stage.setTitle(LanguageManager.getString("app.title"));
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.setWidth(Config.setting().getAppWidth() < 1200 ? 1200 : Config.setting().getAppWidth());
        stage.setHeight(Config.setting().getAppHeight() < 700 ? 700 : Config.setting().getAppHeight());
        Config.setting().appWidthProperty().bind(scene.widthProperty());
        Config.setting().appHeightProperty().bind(scene.heightProperty());
    }

    private void initFont() {
        boolean contains = Font.getFamilies().contains("Microsoft YaHei");
        if (!contains) {
            LOG.info("默认字体不存在，加载内置字体");
            Font.loadFonts(FXResourcesLoader.loadStream("font/HarmonyOS_Sans_SC_Bold.ttf"), 12);
            window.getScene().getRoot().setStyle("-fx-font-family: \"HarmonyOS Sans SC\"");
        } else {
            window.getScene().getRoot().setStyle("-fx-font-family: \"Microsoft YaHei\"");
        }
    }

    public void initKeyHook() {
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            throw new RuntimeException(e);
        }
        GlobalScreen.addNativeKeyListener(new GlobalKeyListener());
    }

    public static boolean isAutoStarted() {
        return autoStarted;
    }

    public static Stage getWindow() {
        return window;
    }

    public static void exit() {
        AppInjector.getInstance(GameWindowMonitorService.class).stop();
        AppInjector.getInstance(TrayIconManager.class).remove();
        Platform.setImplicitExit(true);
        JdbcUtils.exit();
        if (window != null) {
            window.setX(-10000);
            window.setMaximized(false);
            window.close();
        }
        Config.setting().save();
        appLocked.release();
        System.exit(0);
    }
}
