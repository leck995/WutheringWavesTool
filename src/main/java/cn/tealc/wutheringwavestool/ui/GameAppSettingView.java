package cn.tealc.wutheringwavestool.ui;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.dao.GameSettingDao;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.fxml.Initializable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-17 22:50
 */
public class GameAppSettingView implements FxmlView<GameAppSettingViewModel>, Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(GameAppSettingView.class);
    @InjectViewModel
    private GameAppSettingViewModel viewModel;
    @FXML
    private ToggleSwitch userAdvanceSettingSwitch;

    @FXML
    private ScrollPane content;
    @FXML
    private ToggleGroup gameDxToggleGroup;

    @FXML
    private RadioButton gameFps120;

    @FXML
    private RadioButton gameFps30;

    @FXML
    private RadioButton gameFps60;


    @FXML
    private ToggleGroup gameFpsToggleGroup;

    @FXML
    private TextField gameStartAppField;

    @FXML
    private StackPane gameStartAppGroup;

    @FXML
    private RadioButton gameStartAppRadioDefault;

    @FXML
    private ToggleGroup gameStartAppToggleGroup;

    @FXML
    private TextField paramField;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        userAdvanceSettingSwitch.selectedProperty().bindBidirectional(Config.setting.userAdvanceGameSettingsProperty());
        content.visibleProperty().bind(userAdvanceSettingSwitch.selectedProperty());

        gameStartAppField.textProperty().bindBidirectional(Config.setting.gameStarAppPathProperty());
        paramField.textProperty().bindBidirectional(Config.setting.appParamsProperty());
        gameStartAppField.positionCaret(gameStartAppField.getText().length());
        if (!Config.setting.isGameStartAppCustom()){
            gameStartAppToggleGroup.selectToggle(gameStartAppToggleGroup.getToggles().getFirst());
        }else {
            gameStartAppToggleGroup.selectToggle(gameStartAppToggleGroup.getToggles().getLast());
        }
        gameStartAppGroup.disableProperty().bind(gameStartAppToggleGroup.selectedToggleProperty().isEqualTo(gameStartAppRadioDefault));

        if (paramField.getText() != null && paramField.getText().contains("dx11")){
            gameDxToggleGroup.selectToggle(gameDxToggleGroup.getToggles().getFirst());
        }else if (paramField.getText() != null && paramField.getText().contains("dx12")){
            gameDxToggleGroup.selectToggle(gameDxToggleGroup.getToggles().getLast());
        }


        String fps = viewModel.getFps();
        if (fps != null){
            switch (fps) {
                case "0" -> gameFpsToggleGroup.selectToggle(gameFpsToggleGroup.getToggles().getFirst());
                case "1" -> gameFpsToggleGroup.selectToggle(gameFpsToggleGroup.getToggles().get(1));
                case "2" -> gameFpsToggleGroup.selectToggle(gameFpsToggleGroup.getToggles().get(2));
                case "3" -> gameFpsToggleGroup.selectToggle(gameFpsToggleGroup.getToggles().get(3));
            }
        }



    }

    @FXML
    void showWarning(MouseEvent event) {
        if (userAdvanceSettingSwitch.isSelected()) {
            userAdvanceSettingSwitch.setSelected(false);
            Label title = new Label("警告");
            title.getStyleClass().add(Styles.TITLE_2);
            Label tip1 =new Label("高级启动部分功能存在修改游戏配置的行为，一旦开启，由于误操作或者其他原因，导致的所有的责任将由用户自己承担，与开发者没有任何关系，请谨慎对待。");
            tip1.setWrapText(true);
            tip1.setPrefWidth(450);
            tip1.setMinHeight(80);
            VBox center = new VBox(5.0,tip1);
            Button okBtn = new Button("继续使用");
            okBtn.getStyleClass().add(Styles.DANGER);

            Button cancelBtn = new Button("取消");
            cancelBtn.setCancelButton(true);

            okBtn.setOnAction(actionEvent -> {
                userAdvanceSettingSwitch.setSelected(true);
                cancelBtn.fireEvent(actionEvent);
            });
            JFXDialogLayout dialogLayout=new JFXDialogLayout();
            dialogLayout.setHeading(title);
            dialogLayout.setBody(center);
            dialogLayout.setActions(okBtn,cancelBtn);
            dialogLayout.setPrefSize(500,500);
            MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG,dialogLayout);
        }else {

        }
    }

    @FXML
    void setAppPathModel(ActionEvent event) {
        Object source = event.getSource();
        if (source instanceof RadioButton button) {
            switch (button.getAccessibleText()) {
                case "default" -> {
                    Config.setting.setGameStartAppCustom(false);
                    File gameExeClient = GameResourcesManager.getGameExeBase();
                    if (gameExeClient != null) {
                        gameStartAppField.setText(gameExeClient.getAbsolutePath());
                    }else {
                        gameStartAppField.setText("Wuthering Waves.exe");
                    }

                    gameStartAppField.positionCaret(gameStartAppField.getText().length());
                }
                case "custom" -> {
                    Config.setting.setGameStartAppCustom(true);
                    File gameExeClient = GameResourcesManager.getGameExeClient();
                    if (gameExeClient != null) {
                        gameStartAppField.setText(gameExeClient.getAbsolutePath());
                    }
                    gameStartAppField.positionCaret(gameStartAppField.getText().length());
                }
            }
        }
    }

    @FXML
    void setDX(ActionEvent event) {
        Object source = event.getSource();
        if (source instanceof RadioButton button) {
            String text = paramField.getText();
            switch (button.getText()) {
                case "DX11" -> {
                    if (text != null && !text.contains("-dx")) {
                        paramField.setText(paramField.getText() + " -dx11");
                    }else if (text != null && text.contains("-dx")){
                        String replace = text.replace("dx12", "dx11");
                        paramField.setText(replace);
                    }else if (text == null){
                        paramField.setText("-dx11");
                    }
                    paramField.positionCaret(gameStartAppField.getText().length());
                }
                case "DX12" -> {
                    if (text != null && !text.contains("-dx")) {
                        paramField.setText(paramField.getText() + " -dx12");
                    }else if (text != null && text.contains("-dx")){
                        String replace = text.replace("dx11", "dx12");
                        paramField.setText(replace);
                    }else if (text == null){
                        paramField.setText("-dx12");
                    }
                    paramField.positionCaret(gameStartAppField.getText().length());
                }

            }
        }
    }

    @FXML
    void setFPS(ActionEvent event) {
        Object source = event.getSource();
        if (source instanceof RadioButton button) {
            switch (button.getText()) {
                case "30" -> viewModel.setFps("0");
                case "45" -> viewModel.setFps("1");
                case "60" -> viewModel.setFps("2");
                case "120" -> viewModel.setFps("3");
            }
        }
    }

    @FXML
    void setGameApp(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择鸣潮启动程序");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("exe","*.exe"));
        File file = fileChooser.showOpenDialog(gameStartAppField.getScene().getWindow());
        if (file != null) {
            gameStartAppField.setText(file.getAbsolutePath());
            gameStartAppField.positionCaret(gameStartAppField.getText().length());
        }
    }


    @FXML
    void openEngineIni(ActionEvent event) {
        File gameEngineIni = GameResourcesManager.getGameEngineIni();
        if (gameEngineIni != null) {
            try {
                Desktop.getDesktop().open(gameEngineIni);
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        }else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.WARNING,"无法找到配置文件Engine。ini"));
        }
    }
}