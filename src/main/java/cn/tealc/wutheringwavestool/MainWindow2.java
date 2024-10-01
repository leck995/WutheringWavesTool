package cn.tealc.wutheringwavestool;

import cn.tealc.teafx.stage.RoundStage;
import cn.tealc.teafx.stage.handler.DragWindowHandler;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.ui.MainView;
import cn.tealc.wutheringwavestool.ui.MainViewModel;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xss.it.nfx.WindowState;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-09-29 20:17
 */
public class MainWindow2 extends RoundStage {
    private static final Logger LOG= LoggerFactory.getLogger(MainWindow2.class);
    private final MainView mainView;
    private final Button closeBtn;
    private final Button minBtn;
    private final Button maxBtn;
    private final HBox titlebar;
    public MainWindow2() {
        Application.setUserAgentStylesheet(FXResourcesLoader.load("css/light.css"));
        ViewTuple<MainView, MainViewModel> viewTuple = FluentViewLoader.fxmlView(MainView.class).load();
        mainView = viewTuple.getCodeBehind();
        closeBtn = mainView.getCloseBtn();
        minBtn = mainView.getMinBtn();
        maxBtn = mainView.getMaxBtn();
        titlebar = mainView.getTitlebar();

        DragWindowHandler handler=new DragWindowHandler(this,true);
        titlebar.setOnMousePressed(handler);
        titlebar.setOnMouseDragged(handler);
        titlebar.setOnMouseReleased(handler);
        titlebar.setOnMouseClicked(handler);

        closeBtn.setOnAction(event -> {
            mainView.showExitDialog();
        });
        maxedProperty().addListener((observable, oldValue, newValue) -> {
            if (maxBtn.getGraphic() instanceof SVGPath path) {
                if (newValue) {
                    path.setContent(REST_SHAPE);
                } else{
                    path.setContent(MAX_SHAPE);
                }
            }
        });
        maxBtn.setOnAction(event -> setMaxed(!isMaxed()));
        minBtn.setOnAction(event -> setIconified(true));
        setContent(viewTuple.getView());
        Scene scene = getScene();
        scene.getStylesheets().add(FXResourcesLoader.load("css/Default.css"));
        getIcons().add(new Image(FXResourcesLoader.load("image/icon.png"),45,45,true,true));
        setTitle("鸣潮助手");
        setWidth(Config.setting.getAppWidth());
        setHeight(Config.setting.getAppHeight());
        setMinHeight(720.0);
        setMinWidth(1280.0);
        //initStyle(StageStyle.UNIFIED);
        Config.setting.appWidthProperty().bind(scene.widthProperty());
        Config.setting.appHeightProperty().bind(scene.heightProperty());
        initFont();
    }


    private void initFont(){
        boolean contains = Font.getFamilies().contains("Microsoft YaHei");
        if (!contains){
            LOG.info("默认字体不存在，加载内置字体");
            try {
                FileInputStream fileInputStream=new FileInputStream("assets/font/HarmonyOS_Sans_SC_Regular.ttf");
                Font.loadFonts(fileInputStream,12);
                FileInputStream fileInputStream2=new FileInputStream("assets/font/HarmonyOS_Sans_SC_Bold.ttf");
                Font.loadFonts(fileInputStream2,12);
                getScene().getRoot().setStyle("-fx-font-family: \"HarmonyOS Sans SC\"");
            } catch (FileNotFoundException e) {
                LOG.error("加载自定义字体出现错误",e);
            }
        }else {
            getScene().getRoot().setStyle("-fx-font-family: \"Microsoft YaHei\"");
        }
    }


    public static final String MAX_SHAPE = "M2.5 2 A 0.50005 0.50005 0 0 0 2 2.5L2 13.5 A 0.50005 0.50005 0 0 0 2.5 14L13.5 14 A 0.50005 0.50005 0 0 0 14 13.5L14 2.5 A 0.50005 0.50005 0 0 0 13.5 2L2.5 2 z M 3 3L13 3L13 13L3 13L3 3 z";
    public static final String REST_SHAPE = "M4.5 2 A 0.50005 0.50005 0 0 0 4 2.5L4 4L2.5 4 A 0.50005 0.50005 0 0 0 2 4.5L2 13.5 A 0.50005 0.50005 0 0 0 2.5 14L11.5 14 A 0.50005 0.50005 0 0 0 12 13.5L12 12L13.5 12 A 0.50005 0.50005 0 0 0 14 11.5L14 2.5 A 0.50005 0.50005 0 0 0 13.5 2L4.5 2 z M 5 3L13 3L13 11L12 11L12 4.5 A 0.50005 0.50005 0 0 0 11.5 4L5 4L5 3 z M 3 5L11 5L11 13L3 13L3 5 z";

}