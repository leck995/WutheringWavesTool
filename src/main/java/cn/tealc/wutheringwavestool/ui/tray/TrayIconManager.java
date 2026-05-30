package cn.tealc.wutheringwavestool.ui.tray;

import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.WwtApp;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.google.inject.Singleton;
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

@Singleton
public class TrayIconManager {
    private static final Logger LOG = LoggerFactory.getLogger(TrayIconManager.class);

    public void install(Stage stage) {
        if (!SystemTray.isSupported()) {
            LOG.info("SystemTray is not supported");
            return;
        }

        Button show = new Button(
                LanguageManager.getString("ui.tray.show"),
                new FontIcon(Material2OutlinedMZ.REMOVE_FROM_QUEUE));
        show.setOnAction(event -> {
            stage.setIconified(false);
            stage.show();
            stage.toFront();
        });

        Button exit = new Button(
                LanguageManager.getString("ui.tray.exit"),
                new FontIcon(Material2OutlinedMZ.POWER_SETTINGS_NEW));
        exit.setOnAction(event -> Platform.runLater(WwtApp::exit));

        VBox vbox = new VBox(show, exit);
        vbox.getStyleClass().add("tray");
        vbox.getStylesheets().add(FXResourcesLoader.load("css/TrayIcon.css"));
        vbox.setPrefWidth(80);
        vbox.setPrefHeight(60);

        NewFxTrayIcon trayIcon = new NewFxTrayIcon(
                SwingFXUtils.fromFXImage(stage.getIcons().getFirst(), null),
                Config.appTitle, vbox);
        trayIcon.addActionListener(e -> Platform.runLater(() -> {
            stage.setIconified(false);
            stage.show();
            stage.toFront();
        }));

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            LOG.error("Tray Error", e);
        }
    }

    public void remove() {
        SystemTray systemTray = SystemTray.getSystemTray();
        for (TrayIcon trayIcon : systemTray.getTrayIcons()) {
            if (trayIcon instanceof NewFxTrayIcon tray) {
                systemTray.remove(tray);
            }
        }
    }
}
