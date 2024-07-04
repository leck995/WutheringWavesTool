package cn.tealc.wutheringwavestool.ui;

import atlantafx.base.controls.Message;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import cn.tealc.teafx.controls.TitleBar;
import cn.tealc.wutheringwavestool.Config;
import cn.tealc.wutheringwavestool.NotificationKey;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedAL;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 18:59
 */
public class MainView implements Initializable,FxmlView<MainViewModel> {
    @FXML
    private StackPane child;

    @FXML
    private ToggleGroup navToggleGroup;

    @FXML
    private AnchorPane root;

    @FXML
    private VBox messagePane;
    @FXML
    private ToggleButton analysisBtn;
    @FXML
    private ToggleButton homeBtn;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {


        if (Config.setting.isFirstViewWithPoolAnalysis()){
            ViewTuple<AnalysisPoolView, AnalysisPoolViewModel> viewTuple = FluentViewLoader.fxmlView(AnalysisPoolView.class).load();
            child.getChildren().setAll(viewTuple.getView());
            navToggleGroup.selectToggle(analysisBtn);
        }else {
            ViewTuple<HomeView, HomeViewModel> viewTuple = FluentViewLoader.fxmlView(HomeView.class).load();
            child.getChildren().setAll(viewTuple.getView());
        }

        MvvmFX.getNotificationCenter().subscribe(NotificationKey.MESSAGE,((s, objects) -> {
            if (messagePane.getChildren().size() > 7){
                messagePane.getChildren().remove(0);
            }
            MessageInfo info= (MessageInfo) objects[0];
            Message message = createMessage(info);
            message.setOnClose(e -> {
                var out = Animations.slideOutRight(message, Duration.millis(250));
                out.setOnFinished(f -> messagePane.getChildren().remove(message));
                out.playFromStart();
            });
            Platform.runLater(() -> {
                messagePane.getChildren().add(message);
                message.setTranslateX(300);
                Timeline timeline=new Timeline(new KeyFrame(Duration.millis(250),new KeyValue(message.translateXProperty(),0)));
                timeline.play();
            });
            if (info.getAutoClose()){
                Timeline fiveSecondsWonder = new Timeline(new KeyFrame(info.getShowTime(), new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        var out = Animations.slideOutRight(message, Duration.millis(250));
                        out.setOnFinished(f -> messagePane.getChildren().remove(message));
                        out.playFromStart();
                    }
                }));
                fiveSecondsWonder.play();
            }
        }));
    }
    @FXML
    void toAnalysis(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            ViewTuple<AnalysisPoolView, AnalysisPoolViewModel> viewTuple = FluentViewLoader.fxmlView(AnalysisPoolView.class).load();
            child.getChildren().setAll(viewTuple.getView());
        }else {
            toggleButton.setSelected(true);
        }

    }

    @FXML
    void toMain(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            ViewTuple<HomeView, HomeViewModel> viewTuple = FluentViewLoader.fxmlView(HomeView.class).load();
            child.getChildren().setAll(viewTuple.getView());
        }else {
            toggleButton.setSelected(true);
        }
    }

    @FXML
    void toSetting(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            ViewTuple<SettingView, SettingViewModel> viewTuple = FluentViewLoader.fxmlView(SettingView.class).load();
            child.getChildren().setAll(viewTuple.getView());
        }else {
            toggleButton.setSelected(true);
        }
    }



    private Message createMessage(MessageInfo messageInfo){
        Message message = null;
        switch (messageInfo.getType()){
            case SUCCESS -> {
                message = new Message(
                        "Success",
                        messageInfo.getMessage(),
                        new FontIcon(Material2OutlinedAL.CHECK_CIRCLE_OUTLINE)
                );
                message.getStyleClass().add(Styles.SUCCESS);
            }
            case WARNING -> {
                message = new Message(
                        "Warning",
                        messageInfo.getMessage(),
                        new FontIcon(Material2OutlinedMZ.OUTLINED_FLAG)
                );
                message.getStyleClass().add(Styles.WARNING);
            }
            case INFO -> {
                message = new Message(
                        "Info",
                        messageInfo.getMessage(),
                        new FontIcon(Material2OutlinedAL.HELP_OUTLINE)
                );
                message.getStyleClass().add(Styles.ACCENT);
            }
            case ERROR -> {
                message = new Message(
                        "ERROR",
                        messageInfo.getMessage(),
                        new FontIcon(Material2OutlinedAL.ERROR_OUTLINE)
                );
                message.getStyleClass().add(Styles.DANGER);
            }
        }

        message.setPrefSize(300.0,60.0);
        message.setMaxSize(300.0,80.0);
        return message;
    }
}