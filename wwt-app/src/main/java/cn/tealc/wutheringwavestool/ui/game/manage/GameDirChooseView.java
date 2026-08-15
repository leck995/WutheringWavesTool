package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class GameDirChooseView implements FxmlView<GameDirChooseViewModel>, Initializable {
    @InjectViewModel
    private GameDirChooseViewModel viewModel;
    @FXML
    private StackPane content;
    @FXML
    private StackPane localDirChild;

    @FXML
    private StackPane selectedChild;
    @FXML
    private TextField localDirField;

    @FXML
    private ToggleGroup serverTypeToggleGroup;
    @FXML
    private Button finishLocalBtn;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initLocal();


    }

    private void initLocal(){
        localDirField.textProperty().bind(viewModel.localDirProperty());
        finishLocalBtn.disableProperty().bind(viewModel.finishEnabledProperty().not());
        viewModel.localSourceTypeProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue  == SourceType.DEFAULT) {
                serverTypeToggleGroup.selectToggle(serverTypeToggleGroup.getToggles().get(0));
            }
            else if (newValue  == SourceType.BILIBILI) {
                serverTypeToggleGroup.selectToggle(serverTypeToggleGroup.getToggles().get(1));
            }
            else if (newValue  == SourceType.WE_GAME) {
                serverTypeToggleGroup.selectToggle(serverTypeToggleGroup.getToggles().get(2));
            } else if(newValue  == SourceType.GLOBAL) {
                serverTypeToggleGroup.selectToggle(serverTypeToggleGroup.getToggles().get(3));
            }
        });

        serverTypeToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == serverTypeToggleGroup.getToggles().get(0)) {
                viewModel.setLocalSourceType(SourceType.DEFAULT);
            }else if(newValue == serverTypeToggleGroup.getToggles().get(1)) {
                viewModel.setLocalSourceType(SourceType.BILIBILI);
            }else if(newValue == serverTypeToggleGroup.getToggles().get(2)) {
                viewModel.setLocalSourceType(SourceType.WE_GAME);
            }else if(newValue == serverTypeToggleGroup.getToggles().get(3)) {
                viewModel.setLocalSourceType(SourceType.GLOBAL);
            }
        });
    }

    @FXML
    void backToSelectedChild(ActionEvent event) {
        content.getChildren().forEach(node -> node.setVisible(false));
        selectedChild.setVisible(true);
    }
    @FXML
    void toAssetManagement(ActionEvent event) {
        NotificationManager.publish(NotificationKey.GAME_MANAGER_TO_ASSET);
    }

    @FXML
    void toLocalChild(ActionEvent event) {
        content.getChildren().forEach(node -> node.setVisible(false));
        localDirChild.setVisible(true);
    }


    @FXML
    void setLocalDir(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(LanguageManager.getString("ui.setting.file.game_dir.title"));
        File file = directoryChooser.showDialog(content.getScene().getWindow());
        if (file != null) {
            viewModel.setLocalDir(file);
        }
    }

    @FXML
    void finishLocalDir(ActionEvent event) {
        boolean finished = viewModel.finishLocal();
        if (finished) { //前往游戏设置界面
            NotificationManager.publish(NotificationKey.GAME_MANAGER_TO_BASE);
        }else {
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.game_manager.message01")));
        }
    }




}
