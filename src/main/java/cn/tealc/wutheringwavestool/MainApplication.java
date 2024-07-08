package cn.tealc.wutheringwavestool;

import atlantafx.base.theme.PrimerLight;
import cn.tealc.teafx.stage.RoundStage;
import cn.tealc.wutheringwavestool.ui.AnalysisPoolView;
import cn.tealc.wutheringwavestool.ui.AnalysisPoolViewModel;
import cn.tealc.wutheringwavestool.ui.MainView;
import cn.tealc.wutheringwavestool.ui.MainViewModel;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    public static RoundStage window;
    @Override
    public void start(Stage stage) throws IOException {
        stage.close();
        RoundStage roundStage=new RoundStage();
        window=roundStage;
        roundStage.setWidth(1300.0);
        roundStage.setHeight(750.0);
        roundStage.setTitle("鸣潮助手");
        roundStage.getIcons().add(new Image(MainApplication.class.getResourceAsStream("image/icon.png")));
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        ViewTuple<MainView, MainViewModel> viewTuple = FluentViewLoader.fxmlView(MainView.class).load();
        roundStage.setContent(viewTuple.getView());


        roundStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        Config.save();
    }

    public static void main(String[] args) {
        launch();
    }
}