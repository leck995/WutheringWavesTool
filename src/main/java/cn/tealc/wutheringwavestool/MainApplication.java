package cn.tealc.wutheringwavestool;

import cn.tealc.teafx.stage.RoundStage;
import cn.tealc.wutheringwavestool.dao.JdbcUtils;
import cn.tealc.wutheringwavestool.jna.GameAppListener;
import cn.tealc.wutheringwavestool.theme.PrimerDark;
import cn.tealc.wutheringwavestool.theme.PrimerLight;
import cn.tealc.wutheringwavestool.ui.AnalysisPoolView;
import cn.tealc.wutheringwavestool.ui.AnalysisPoolViewModel;
import cn.tealc.wutheringwavestool.ui.MainView;
import cn.tealc.wutheringwavestool.ui.MainViewModel;
import com.dustinredmond.fxtrayicon.FXTrayIcon;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MainApplication extends Application {
    private static final Logger LOG=LoggerFactory.getLogger(MainApplication.class);
    public static RoundStage window;
    private static WinNT.HANDLE gameAppListener;
    public GameAppListener appListener;
    private static FXTrayIcon fxTrayIcon;

    @Override
    public void start(Stage stage) throws IOException {
        stage.close();
        JdbcUtils.init();
        System.setProperty("prism.lcdtext", "false");
       // System.setProperty("LcdFontSmoothing", "true");
        System.setProperty("prism.text", "t2k");

        VersionUpdateUtil.update();


        RoundStage roundStage=new RoundStage();
        window=roundStage;
        roundStage.setWidth(1300.0);
        roundStage.setHeight(750.0);
        roundStage.setTitle("鸣潮助手");
        roundStage.getIcons().add(new Image(MainApplication.class.getResourceAsStream("image/icon.png")));
        initFont();
        if (Config.setting.isTheme()){
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        }else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        }



        ViewTuple<MainView, MainViewModel> viewTuple = FluentViewLoader.fxmlView(MainView.class).load();
        roundStage.setContent(viewTuple.getView());

        appListener = GameAppListener.getInstance();
        gameAppListener = User32.INSTANCE.SetWinEventHook(0x0003, 0x0003, null, appListener, 0, 0, 0);

        roundStage.show();
        createTrayIcon();
        initExceptionHandler();
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




    public static void exit(){
        if (gameAppListener != null) {
            User32.INSTANCE.UnhookWinEvent(gameAppListener);
        }
        JdbcUtils.exit();
        Config.save();
        window.close();
        System.exit(0);
    }
    public static void main(String[] args) {
        launch();
    }



    private void createTrayIcon() throws UnsupportedEncodingException {
        MenuItem recovery=new MenuItem("还原");
        recovery.setOnAction(event -> {
            Platform.runLater(()->{
                window.setIconified(false);
                window.show();
                window.toFront();
            });

        });
        fxTrayIcon = new FXTrayIcon(window, window.getIcons().getFirst());

        fxTrayIcon.addExitItem("退出",actionEvent -> {
            Platform.runLater(MainApplication::exit);
        });

        fxTrayIcon.addMenuItems(recovery);
        fxTrayIcon.show();
    }

}