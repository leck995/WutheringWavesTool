package cn.tealc.wutheringwavestool.ui.game.manage;

import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.ui.cardpool.CardDetailAnalysisView;
import cn.tealc.wutheringwavestool.ui.cardpool.CardDetailAnalysisViewModel;
import cn.tealc.wutheringwavestool.util.ButtonBuilder;
import cn.tealc.wutheringwavestool.util.DialogBuilder;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class GameBaseSettingView implements FxmlView<GameBaseSettingViewModel>, Initializable {
    @InjectViewModel
    private GameBaseSettingViewModel viewModel;
    @FXML
    private TextField gameDirField;
    @FXML
    private ToggleGroup gameSourceTypeToggleGroup;
    @FXML
    private TextField gameStartAppField;
    @FXML
    private StackPane gameStartAppGroup;
    @FXML
    private RadioButton gameStartAppRadioDefault;
    @FXML
    private ToggleGroup gameStartAppType;
    @FXML
    private RadioButton sourceTypeBtn01;
    @FXML
    private RadioButton sourceTypeBtn02;
    @FXML
    private RadioButton sourceTypeBtn03;
    @FXML
    private RadioButton sourceTypeBtn04;
    @FXML
    private ListView<String> paramListView;
    @FXML
    private TextField paramField;
    @FXML
    private ToggleGroup gameDxToggleGroup;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        viewModel.init();
        initGameAsset();
        initStartParam();
    }


    private void initGameAsset(){
        gameDirField.setEditable(false);
        gameDirField.textProperty().bindBidirectional(viewModel.gameDirProperty());

        sourceTypeBtn01.disableProperty().bind(viewModel.sourceTypeDisabled01Property());
        sourceTypeBtn02.disableProperty().bind(viewModel.sourceTypeDisabled02Property());
        sourceTypeBtn03.disableProperty().bind(viewModel.sourceTypeDisabled03Property());
        sourceTypeBtn04.disableProperty().bind(viewModel.sourceTypeDisabled04Property());


        viewModel.gameSourceTypeProperty().addListener((observable, oldValue, newValue) -> {
            updateSelectedSourceType(newValue);
        });
        updateSelectedSourceType(viewModel.getGameSourceType());

/*        gameSourceTypeToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == gameSourceTypeToggleGroup.getToggles().get(0)) {
                //viewModel.setLocalSourceType(SourceType.DEFAULT);
                boolean success = viewModel.changeServer(SourceType.DEFAULT);
                if (success) {
                    gameStartAppField.setDisable(false);
                }


            }else if(newValue == gameSourceTypeToggleGroup.getToggles().get(1)) {
                //viewModel.setLocalSourceType(SourceType.BILIBILI);
            }else if(newValue == gameSourceTypeToggleGroup.getToggles().get(2)) {
                //viewModel.setLocalSourceType(SourceType.WE_GAME);
            }else if(newValue == gameSourceTypeToggleGroup.getToggles().get(3)) {
                //viewModel.setLocalSourceType(SourceType.GLOBAL);
            }
        });*/
        gameStartAppField.textProperty().bindBidirectional(viewModel.gameAppStartPathProperty());
        gameStartAppGroup.disableProperty().bind(gameStartAppType.selectedToggleProperty().isEqualTo(gameStartAppRadioDefault));

        if (!viewModel.isGameAppStartCustom()){
            gameStartAppType.selectToggle(gameStartAppType.getToggles().getFirst());
        }else {
            gameStartAppType.selectToggle(gameStartAppType.getToggles().get(1));
        }
    }

    private void initStartParam(){
        paramField.setOnAction(event -> {
            String param = paramField.getText();
            if (!param.trim().isEmpty()){
                viewModel.addParam(paramField.getText());
                paramField.clear();
            }
        });
        paramListView.setItems(viewModel.getStartUpParams());
        paramListView.setCellFactory(stringListView -> new ParamListCell());

        if (viewModel.isDx11()){
            gameDxToggleGroup.selectToggle(gameDxToggleGroup.getToggles().getFirst());
        }else if (viewModel.isDx12()){
            gameDxToggleGroup.selectToggle(gameDxToggleGroup.getToggles().getLast());
        }
    }





    private void updateSelectedSourceType(SourceType sourceType) {
        if (sourceType  == SourceType.DEFAULT) {
            gameSourceTypeToggleGroup.selectToggle(gameSourceTypeToggleGroup.getToggles().get(0));
        }
        else if (sourceType  == SourceType.BILIBILI) {
            gameSourceTypeToggleGroup.selectToggle(gameSourceTypeToggleGroup.getToggles().get(1));
        }
        else if (sourceType  == SourceType.WE_GAME) {
            gameSourceTypeToggleGroup.selectToggle(gameSourceTypeToggleGroup.getToggles().get(2));
        } else if(sourceType  == SourceType.GLOBAL) {
            gameSourceTypeToggleGroup.selectToggle(gameSourceTypeToggleGroup.getToggles().get(3));
        }
    }


    @FXML
    void setDX(ActionEvent event) {
        Object source = event.getSource();
        if (source instanceof RadioButton button) {
            switch (button.getText()) {
                case "DX11" -> {
                    viewModel.addDx11();
                }
                case "DX12" -> {
                    viewModel.addDx12();
                }
            }
        }
    }

    @FXML
    void setGameDir(ActionEvent event) {
 /*       DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(LanguageManager.getString("ui.setting.file.game_dir.title"));
        File file = directoryChooser.showDialog(gameDirField.getScene().getWindow());
        if (file != null) {
            File startApp = new File(file.getAbsolutePath() + File.separator + "Wuthering Waves.exe");
            if (startApp.exists()) {
                gameDirField.setText(file.getAbsolutePath());
            }else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.WARNING,LanguageManager.getString("ui.setting.message.01")));
            }
        }*/


        NotificationManager.publish(NotificationKey.GAME_MANAGE_TO_CHOOSE);
    }

    @FXML
    void setSelectedGameType(ActionEvent event) {
/*        Object source = event.getSource();
        if (source instanceof RadioButton button) {
            switch (button.getAccessibleText()) {
                case "default"-> {
                    viewModel.setGameRootDirSource(SourceType.DEFAULT);
                }
                case "wegame" -> {
                    viewModel.setGameRootDirSource(SourceType.WE_GAME);
                    NotificationManager.message(new MessageInfo(MessageType.WARNING,"WeGame暂时无法直接启动，需要替换文件，具体请前往设置加群获取教程", Duration.seconds(5)));
                }
                case "global" -> {
                    viewModel.setGameRootDirSource(SourceType.GLOBAL);
                }
            }
        }*/
    }

    @FXML
    void setAppPathModel(ActionEvent event) {
        Object source = event.getSource();
        if (source instanceof RadioButton button) {
            switch (button.getAccessibleText()) {
                case "default" -> {
                    Config.setting().setGameStartAppCustom(false);
                    File gameExeClient = GameResourcesManager.getGameExeBase();
                    if (gameExeClient != null) {
                        gameStartAppField.setText(gameExeClient.getAbsolutePath());
                    }else {
                        gameStartAppField.setText("Wuthering Waves.exe");
                    }

                    gameStartAppField.positionCaret(gameStartAppField.getText().length());
                }
                case "custom" -> {
                    Config.setting().setGameStartAppCustom(true);
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
    void setGameApp(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(LanguageManager.getString("ui.setting.file.app.title"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("exe","*.exe","*.*"));
        File file = fileChooser.showOpenDialog(gameDirField.getScene().getWindow());
        if (file != null) {
            gameStartAppField.setText(file.getAbsolutePath());
        }
    }


/*    void setSelectedGameType(ActionEvent event) {
        Button cancelBtn = ButtonBuilder.create().title("取消")
                .cancel().build();
        Button okBtn = ButtonBuilder.create().title("确定")
                .styleClass(Styles.DANGER)
                .ok()
                .action(actionEvent -> {
                    Object source = event.getSource();
                    if (source instanceof RadioButton button) {

                    }
                    cancelBtn.fire();
                }).build();
        JFXDialogLayout layout = DialogBuilder.create()
                .title("提示")
                .message("请确认选取的区服正确，错误的区服将会影响游戏的正确更新")
                .buttons(okBtn, cancelBtn)
                .build();
        NotificationManager.dialog(layout);
    }*/

    class ParamListCell extends ListCell<String>{
        private final Button btn;
        private final StackPane child;
        private final Label row;
        public ParamListCell() {
            child = new StackPane();
            row = new Label();
            btn = new Button(null,new FontIcon(Material2AL.DELETE_OUTLINE));
            btn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, "delete-btn");
            btn.setOnAction(event -> viewModel.deleteParam(getIndex()));
            child.getChildren().addAll(row,btn);
            StackPane.setAlignment(row, Pos.CENTER_LEFT);
            StackPane.setAlignment(btn, Pos.CENTER_RIGHT);


        }



        @Override
        protected void updateItem(String string, boolean b) {
            super.updateItem(string, b);
            if (!b){
                setGraphic(child);
                row.setText(string);
            }else {
                setGraphic(null);
                row.setText(null);
            }
        }
    }
}
