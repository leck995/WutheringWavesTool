package cn.tealc.wutheringwavestool;

import ch.qos.logback.classic.Level;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.dao.JdbcUtils;
import cn.tealc.wutheringwavestool.jna.GameAppListener;
import cn.tealc.wutheringwavestool.jna.GlobalKeyListener;
import cn.tealc.wutheringwavestool.ui.tray.NewFxTrayIcon;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinNT;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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


    public MainApplication() {
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("LcdFontSmoothing", "true");
        System.setProperty("prism.text", "t2k");

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.toLevel(Config.setting.getLogLevel()));

        Platform.setImplicitExit(false);
    }

    @Override
    public void start(Stage stage) throws IOException {
        JdbcUtils.init();
        VersionUpdateUtil.update();
        if (!Config.setting.isChangeTitlebar()){
            window = new MainWindow();
            window.show();
        }else {
            window = new MainWindow2();
            window.show();
        }


        if (Config.setting.isTheme()){
            //Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        }else {
            //Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        }

        //Application.setUserAgentStylesheet(FXResourcesLoader.load("css/light.css"));
        appListener = GameAppListener.getInstance();
        gameAppListener = User32.INSTANCE.SetWinEventHook(0x0003, 0x0003, null, appListener, 0, 0, 0);
        createTrayIcon();
        initExceptionHandler();

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
        Config.save();
        window.close();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch();
    }

    private void createTrayIcon() {
        if (SystemTray.isSupported()){
            Button show = new Button("显示",new FontIcon(Material2OutlinedMZ.REMOVE_FROM_QUEUE));
            show.setOnAction(event -> {
                window.setIconified(false);
                window.show();
                window.toFront();
            });
            Button exit = new Button("退出",new FontIcon(Material2OutlinedMZ.POWER_SETTINGS_NEW));
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