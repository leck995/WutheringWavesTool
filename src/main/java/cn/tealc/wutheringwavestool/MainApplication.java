package cn.tealc.wutheringwavestool;

import ch.qos.logback.classic.Level;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.dao.JdbcUtils;
import cn.tealc.wutheringwavestool.jna.GameAppListener;
import cn.tealc.wutheringwavestool.jna.GlobalKeyListener;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.theme.Light;
import cn.tealc.wutheringwavestool.theme.ThemeManager;
import cn.tealc.wutheringwavestool.thread.system.ClearLogFileTask;
import cn.tealc.wutheringwavestool.ui.MainView;
import cn.tealc.wutheringwavestool.ui.MainViewModel;
import cn.tealc.wutheringwavestool.util.AppLocked;
import cn.tealc.wutheringwavestool.thread.system.ResourcesSyncTask;
import cn.tealc.wutheringwavestool.ui.tray.NewFxTrayIcon;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinNT;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;

public class MainApplication extends Application {
    private static final Logger LOG=LoggerFactory.getLogger(MainApplication.class);
    public static Stage window;
    private static WinNT.HANDLE gameAppListener;
    public GameAppListener appListener;
    private NewFxTrayIcon newFxTrayIcon;

    private static AppLocked appLocked;

    public MainApplication() {
        MvvmFX.setGlobalResourceBundle(Config.language);
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.toLevel(Config.setting.getLogLevel()));
        Platform.setImplicitExit(false);
        appLocked = new AppLocked();
    }

    @Override
    public void start(Stage stage) throws IOException {
        JdbcUtils.init();
        VersionUpdateUtil.update();

        window = stage;

        Application.setUserAgentStylesheet(FXResourcesLoader.load("css/light.css"));
        ViewTuple<MainView, MainViewModel> viewTuple = FluentViewLoader.fxmlView(MainView.class).load();

        Scene scene = new Scene(viewTuple.getView());
        scene.getStylesheets().add(FXResourcesLoader.load("css/Default.css"));
        stage.setScene(scene);
        stage.getIcons().add(new Image(FXResourcesLoader.load("image/icon.png"),45,45,true,true));
        stage.setTitle(LanguageManager.getString("app.title"));
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.setWidth(Config.setting.getAppWidth()  < 1200 ? 1200 : Config.setting.getAppWidth());
        stage.setHeight(Config.setting.getAppHeight() < 700 ? 700 : Config.setting.getAppHeight());
        Config.setting.appWidthProperty().bind(scene.widthProperty());
        Config.setting.appHeightProperty().bind(scene.heightProperty());

        stage.initStyle(StageStyle.EXTENDED);

        initFont();
        stage.show();




/*        ThemeManager.getInstance().setScene(window.getScene());
        ThemeManager.getInstance().setTheme(new Light());*/


        if (Config.setting.isTheme()){
            //Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        }else {
            //Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        }

        appListener = GameAppListener.getInstance();
        gameAppListener = User32.INSTANCE.SetWinEventHook(0x0003, 0x0003, null, appListener, 0, 0, 0);
        createTrayIcon();
        onStart();
    }




    private void initFont(){
        boolean contains = javafx.scene.text.Font.getFamilies().contains("Microsoft YaHei");
        if (!contains){
            LOG.info("默认字体不存在，加载内置字体");
            javafx.scene.text.Font.loadFonts(FXResourcesLoader.loadStream("font/HarmonyOS_Sans_SC_Bold.ttf"),12);
            Font.loadFonts(FXResourcesLoader.loadStream("font/HarmonyOS_Sans_SC_Bold.ttf"),12);
            window.getScene().getRoot().setStyle("-fx-font-family: \"HarmonyOS Sans SC\"");
        }else {
            window.getScene().getRoot().setStyle("-fx-font-family: \"Microsoft YaHei\"");
        }
    }



    private void onStart(){
        Thread.startVirtualThread(new ClearLogFileTask());
        initExceptionHandler();
        syncRemoteResources();
    }



    /**
     * @description: 更同步远程仓库的资源
     * @param:
     * @return  void
     * @date:   2024/10/6
     */
    private void syncRemoteResources() {
        ResourcesSyncTask task = new ResourcesSyncTask();
        task.messageProperty().addListener((observableValue, s, t1) -> {
            if (t1 != null){
                switch (t1){
                    case "success" -> MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.SUCCESS, LanguageManager.getString("ui.main.sync.message.success")));
                    case "error" -> MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.ERROR, LanguageManager.getString("ui.main.sync.message.error")));
                    case "start" -> MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.INFO, LanguageManager.getString("ui.main.sync.message.start")));
                }
            }
        });
        Thread.startVirtualThread(task);
    }


    private void initExceptionHandler(){
        // 捕捉未处理的异常
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                // 抛出栈信息
                LOG.error("线程：{}，出现异常：{}",t.getName(),e.getMessage(),e);
            }
        });
    }


    public void initKeyHook(){
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            throw new RuntimeException(e);
        }
        GlobalScreen.addNativeKeyListener(new GlobalKeyListener());
    }

    public static void exit(){
        if (gameAppListener != null) {
            User32.INSTANCE.UnhookWinEvent(gameAppListener);
        }
        SystemTray systemTray = SystemTray.getSystemTray();
        for (TrayIcon trayIcon : systemTray.getTrayIcons()) {
            if (trayIcon instanceof NewFxTrayIcon tray) {
                systemTray.remove(tray);
            }
        }
        Platform.setImplicitExit(true);
        JdbcUtils.exit();

        window.setX(-10000);
        window.setMaximized(false);
        window.close();;
        Config.save();
        appLocked.release();
        System.exit(0);
    }

    private void createTrayIcon() {
        if (SystemTray.isSupported()){
            Button show = new Button(LanguageManager.getString("ui.tray.show"),new FontIcon(Material2OutlinedMZ.REMOVE_FROM_QUEUE));
            show.setOnAction(event -> {
                window.setIconified(false);
                window.show();
                window.toFront();
            });
            Button exit = new Button(LanguageManager.getString("ui.tray.exit"),new FontIcon(Material2OutlinedMZ.POWER_SETTINGS_NEW));
            exit.setOnAction(event -> Platform.runLater(MainApplication::exit));
            VBox vbox = new VBox(show, exit);
            vbox.getStyleClass().add("tray");
            vbox.getStylesheets().add(FXResourcesLoader.load("css/TrayIcon.css"));
            vbox.setPrefWidth(80);
            vbox.setPrefHeight(60);
            newFxTrayIcon = new NewFxTrayIcon(SwingFXUtils.fromFXImage(window.getIcons().getFirst(),null),Config.appTitle,vbox);
            newFxTrayIcon.addActionListener(e -> {
                Platform.runLater(() -> {
                    window.setIconified(false);
                    window.show();
                    window.toFront();
                });
            });




            SystemTray systemTray = SystemTray.getSystemTray();
            try {
                systemTray.add(newFxTrayIcon);
            } catch (AWTException e) {
                LOG.error("Tray Error",e);
            }
        }else {
            LOG.info("SystemTray is not supported");
        }
    }

}