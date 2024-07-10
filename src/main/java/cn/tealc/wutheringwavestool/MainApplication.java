package cn.tealc.wutheringwavestool;

import atlantafx.base.theme.PrimerLight;
import cn.tealc.teafx.stage.RoundStage;
import cn.tealc.wutheringwavestool.dao.JdbcUtils;
import cn.tealc.wutheringwavestool.jna.GameAppListener;
import cn.tealc.wutheringwavestool.ui.AnalysisPoolView;
import cn.tealc.wutheringwavestool.ui.AnalysisPoolViewModel;
import cn.tealc.wutheringwavestool.ui.MainView;
import cn.tealc.wutheringwavestool.ui.MainViewModel;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    public static RoundStage window;
    private static WinNT.HANDLE gameAppListener;
    private GameAppListener appListener;

    @Override
    public void start(Stage stage) throws IOException {
        stage.close();
        JdbcUtils.init();


        RoundStage roundStage=new RoundStage();
        window=roundStage;
        roundStage.setWidth(1300.0);
        roundStage.setHeight(750.0);
        roundStage.setTitle("鸣潮助手");
        roundStage.getIcons().add(new Image(MainApplication.class.getResourceAsStream("image/icon.png")));
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        ViewTuple<MainView, MainViewModel> viewTuple = FluentViewLoader.fxmlView(MainView.class).load();
        roundStage.setContent(viewTuple.getView());

        appListener = new GameAppListener();
        gameAppListener = User32.INSTANCE.SetWinEventHook(0x0003, 0x0003, null, appListener, 0, 0, 0);
        //addGameAppListener();
        roundStage.show();


    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (gameAppListener != null) {

            User32.INSTANCE.UnhookWinEvent(gameAppListener);
        }

        JdbcUtils.exit();
        Config.save();
    }

    public static void main(String[] args) {
        launch();
    }

    private void addGameAppListener(){
        gameAppListener = User32.INSTANCE.SetWinEventHook(0x0003, 0x0003, null, (handle, dword, hwnd, aLong, aLong1, dword1, dword2) -> {
            char[] buffer = new char[256];
            User32.INSTANCE.GetWindowText(hwnd, buffer, buffer.length);
            String title = Native.toString(buffer);
            System.out.println(title);

        }, 0, 0, 0);
    }
}