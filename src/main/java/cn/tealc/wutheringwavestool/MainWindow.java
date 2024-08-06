/*
 * Copyright © 2024. XTREME SOFTWARE SOLUTIONS
 *
 * All rights reserved. Unauthorized use, reproduction, or distribution
 * of this software or any portion of it is strictly prohibited and may
 * result in severe civil and criminal penalties. This code is the sole
 * proprietary of XTREME SOFTWARE SOLUTIONS.
 *
 * Commercialization, redistribution, and use without explicit permission
 * from XTREME SOFTWARE SOLUTIONS, are expressly forbidden.
 */

package cn.tealc.wutheringwavestool;

import cn.tealc.teafx.stage.RoundStage;
import cn.tealc.teafx.theme.PrimerDark;
import cn.tealc.teafx.theme.PrimerLight;
import cn.tealc.wutheringwavestool.dao.JdbcUtils;
import cn.tealc.wutheringwavestool.jna.GameAppListener;
import cn.tealc.wutheringwavestool.jna.GlobalKeyListener;
import cn.tealc.wutheringwavestool.ui.MainView;
import cn.tealc.wutheringwavestool.ui.MainViewModel;
import com.dustinredmond.fxtrayicon.FXTrayIcon;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinNT;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xss.it.nfx.AbstractNfxUndecoratedWindow;
import xss.it.nfx.HitSpot;
import xss.it.nfx.WindowState;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author XDSSWAR
 * Created on 04/17/2024
 */
public final class MainWindow extends AbstractNfxUndecoratedWindow{
    public static MainWindow window;
    private static final Logger LOG= LoggerFactory.getLogger(MainWindow.class);
    private static final int TITLE_BAR_HEIGHT = 50;
    private final MainView mainView;
    private final Button closeBtn;
    private final Button minBtn;
    private final Button maxBtn;

    /**
     * Constructs a new instance of UndecoratedExample.
     */
    public MainWindow() throws UnsupportedEncodingException {
        super();

        window=this;
        JdbcUtils.init();
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.text", "t2k");
        VersionUpdateUtil.update();
        if (Config.setting.isTheme()){
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        }else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        }


        ViewTuple<MainView, MainViewModel> viewTuple = FluentViewLoader.fxmlView(MainView.class).load();
        mainView = viewTuple.getCodeBehind();
        closeBtn = mainView.getCloseBtn();
        minBtn = mainView.getMinBtn();
        maxBtn = mainView.getMaxBtn();

        closeBtn.setOnAction(event -> {
            mainView.showExitDialog();
        });

        maxBtn.setOnAction(event -> {
            if (getWindowState().equals(WindowState.MAXIMIZED)){
                setWindowState(WindowState.NORMAL);
            }
            else {
                setWindowState(WindowState.MAXIMIZED);
            }
        });

        minBtn.setOnAction(event -> setWindowState(WindowState.MINIMIZED));

        handelState(getWindowState());
        windowStateProperty().addListener((obs, o, state) -> handelState(state));
        Scene scene = new Scene(viewTuple.getView());
        scene.getStylesheets().add(FXResourcesLoader.load("css/Default.css"));
        setScene(scene);
        getIcons().add(new Image(FXResourcesLoader.load("image/icon.png"),45,45,true,true));
        setTitle("鸣潮助手");
        setWidth(Config.setting.getAppWidth());
        setHeight(Config.setting.getAppHeight());
        setMinHeight(720.0);
        setMinWidth(1280.0);
        Config.setting.appWidthProperty().bind(scene.widthProperty());
        Config.setting.appHeightProperty().bind(scene.heightProperty());
        initFont();
    }



    private void initFont(){
        boolean contains = Font.getFamilies().contains("Microsoft YaHei");
        if (!contains){
            LOG.info("默认字体不存在，加载内置字体");
            Font.loadFont(
                    MainApplication.class.getResourceAsStream("/cn/tealc/wutheringwavestool/font/HarmonyOS_Sans_SC_Regular.ttf"),
                    12);
            Font.loadFont(
                    MainApplication.class.getResourceAsStream("/cn/tealc/wutheringwavestool/font/HarmonyOS_Sans_SC_Bold.ttf"),
                    12);
            window.getScene().getRoot().setStyle("-fx-font-family: \"HarmonyOS Sans SC\"");
        }else {
            window.getScene().getRoot().setStyle("-fx-font-family: \"Microsoft YaHei\"");
            LOG.info("默认字体存在");
        }
        LOG.info("系统默认字体:{}",Font.getDefault().getFamily());
    }


    /**
     * Handles the state of the window.
     *
     * @param state The state of the window (e.g., MAXIMIZED, NORMAL).
     */
    private void handelState(WindowState state){
        if (maxBtn.getGraphic() instanceof SVGPath path) {
            if (state.equals(WindowState.MAXIMIZED)) {
                path.setContent(REST_SHAPE);
            } else if (state.equals(WindowState.NORMAL)) {
                path.setContent(MAX_SHAPE);
            }
        }
    }


    /**
     * Retrieves the list of hit spots in the window.
     *
     * @return The list of hit spots.
     */
    @Override
    public List<HitSpot> getHitSpots() {
        HitSpot minimizeHitSpot = HitSpot.builder()
                .control(minBtn)
                .minimize(true)
                .build();

        minimizeHitSpot.hoveredProperty().addListener((obs, o, hovered) -> {
            if (hovered){
                minimizeHitSpot.getControl().getStyleClass().add("hit-hovered");
            }
            else {
                minimizeHitSpot.getControl().getStyleClass().remove("hit-hovered");
            }
        });

        HitSpot maximizeHitSpot = HitSpot.builder()
                .control(maxBtn)
                .maximize(true)
                .build();

        maximizeHitSpot.hoveredProperty().addListener((obs, o, hovered) -> {
            if (hovered){
                maximizeHitSpot.getControl().getStyleClass().add("hit-hovered");
            }
            else {
                maximizeHitSpot.getControl().getStyleClass().remove("hit-hovered");
            }
        });

        HitSpot closeHitSpot = HitSpot.builder()
                .control(closeBtn)
                .close(true)
                .build();

        closeHitSpot.hoveredProperty().addListener((obs, o, hovered) -> {
            if (hovered){
                closeHitSpot.getControl().getStyleClass().add("hit-close-btn");
                closeBtn.getGraphic().getStyleClass().add("shape-close-hovered");
            }
            else {
                closeHitSpot.getControl().getStyleClass().remove("hit-close-btn");
                closeBtn.getGraphic().getStyleClass().remove("shape-close-hovered");
            }
        });

        return List.of(minimizeHitSpot, maximizeHitSpot, closeHitSpot);
    }

    /**
     * Retrieves the height of the title bar.
     *
     * @return The height of the title bar.
     */
    @Override
    public double getTitleBarHeight() {
        return TITLE_BAR_HEIGHT;
    }


    /**
     * SVG path data for the "Minimize" button icon.
     */
    public static final String MIN_SHAPE = "M1 7L1 8L14 8L14 7Z";

    /**
     * SVG path data for the "Maximize" button icon.
     */
    public static final String MAX_SHAPE = "M2.5 2 A 0.50005 0.50005 0 0 0 2 2.5L2 13.5 A 0.50005 0.50005 0 0 0 2.5 14L13.5 14 A 0.50005 0.50005 0 0 0 14 13.5L14 2.5 A 0.50005 0.50005 0 0 0 13.5 2L2.5 2 z M 3 3L13 3L13 13L3 13L3 3 z";

    /**
     * SVG path data for the "Restore" button icon (used when window is maximized).
     */
    public static final String REST_SHAPE = "M4.5 2 A 0.50005 0.50005 0 0 0 4 2.5L4 4L2.5 4 A 0.50005 0.50005 0 0 0 2 4.5L2 13.5 A 0.50005 0.50005 0 0 0 2.5 14L11.5 14 A 0.50005 0.50005 0 0 0 12 13.5L12 12L13.5 12 A 0.50005 0.50005 0 0 0 14 11.5L14 2.5 A 0.50005 0.50005 0 0 0 13.5 2L4.5 2 z M 5 3L13 3L13 11L12 11L12 4.5 A 0.50005 0.50005 0 0 0 11.5 4L5 4L5 3 z M 3 5L11 5L11 13L3 13L3 5 z";

    /**
     * SVG path data for the "Close" button icon.
     */
    public static final String CLOSE_SHAPE = "M3.726563 3.023438L3.023438 3.726563L7.292969 8L3.023438 12.269531L3.726563 12.980469L8 8.707031L12.269531 12.980469L12.980469 12.269531L8.707031 8L12.980469 3.726563L12.269531 3.023438L8 7.292969Z";


}
