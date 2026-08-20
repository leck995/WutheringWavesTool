package cn.tealc.wutheringwavestool.ui.game.manage;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.teafx.utils.message.MessageType;
import cn.tealc.wutheringwavestool.util.AlterBuilder;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.fxml.Initializable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-17 22:50
 */
public class GameAdvanceSettingView implements FxmlView<GameAdvanceSettingViewModel>, Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(GameAdvanceSettingView.class);
    @InjectViewModel
    private GameAdvanceSettingViewModel viewModel;
    @FXML
    private ToggleSwitch userAdvanceSettingSwitch;
    @FXML
    private ScrollPane content;
    @FXML
    private RadioButton gameFps120;
    @FXML
    private RadioButton gameFps30;
    @FXML
    private RadioButton gameFps60;
    @FXML
    private ToggleGroup gameFpsToggleGroup;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        userAdvanceSettingSwitch.selectedProperty().bindBidirectional(Config.setting().userAdvanceGameSettingsProperty());
        content.visibleProperty().bind(userAdvanceSettingSwitch.selectedProperty());
        String fps = viewModel.getFps();
        if (fps != null) {
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
            JFXDialogLayout dialogLayout = AlterBuilder.create()
                    .danger()
                    .title(LanguageManager.getString("ui.common.warning"))
                    .message(LanguageManager.getString("ui.game_manager.advance.warning.tip"))
                    .ok(LanguageManager.getString("ui.game_manager.advance.warning.ok"),
                            actionEvent -> userAdvanceSettingSwitch.setSelected(true))
                    .cancel()
                    .build();
            NotificationManager.alert(dialogLayout);
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
    void openEngineIni(ActionEvent event) {
        File gameEngineIni = GameResourcesManager.getGameEngineIni();
        if (gameEngineIni != null) {
            try {
                Desktop.getDesktop().open(gameEngineIni);
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        } else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, MessageInfo.warning(LanguageManager.getString("ui.game_manager.advance.engine.message01")));
        }
    }
}