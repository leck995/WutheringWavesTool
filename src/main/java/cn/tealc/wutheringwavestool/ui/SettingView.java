package cn.tealc.wutheringwavestool.ui;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.controls.ToggleSwitchSkin;
import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 20:20
 */
public class SettingView implements Initializable, FxmlView<SettingViewModel> {
    private static final Logger LOG= LoggerFactory.getLogger(SettingView.class);
    @InjectViewModel
    private SettingViewModel viewModel;


    @FXML
    private Label appAuthor;

    @FXML
    private ImageView appIconIv;

    @FXML
    private Label appName;

    @FXML
    private Label appVersion;
    @FXML
    private TextField gameDirField;

    @FXML
    private CheckBox startWithAnalysisView;
    @FXML
    private ToggleSwitch exitWhenGameOver;
    @FXML
    private ToggleSwitch hideWhenGameStart;

    @FXML
    private TextField diyBgField;

    @FXML
    private StackPane diyBgInputGroup;

    @FXML
    private ToggleSwitch diyBgSwitch;
    @FXML
    private ToggleSwitch titlebarSwitch;
    @FXML
    private ToggleSwitch versionCheckSwitch;
    @FXML
    private ToggleSwitch noKuJieQuSwitch;
    @FXML
    private TextField gameStartAppField;

    @FXML
    private StackPane gameStartAppGroup;

    @FXML
    private ToggleGroup gameStartAppType;
    @FXML
    private ToggleGroup gameSourceType;
    @FXML
    private RadioButton gameStartAppRadioDefault;

    @FXML
    private ToggleGroup closeEventToggleGroup;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {



        gameDirField.textProperty().bindBidirectional(viewModel.gameDirProperty());
        startWithAnalysisView.selectedProperty().bindBidirectional(viewModel.startWithAnalysisProperty());

        hideWhenGameStart.setSkin(new ToggleSwitchSkin(hideWhenGameStart));
        hideWhenGameStart.selectedProperty().bindBidirectional(viewModel.hideWhenGameStartProperty());
        exitWhenGameOver.selectedProperty().bindBidirectional(viewModel.exitWhenGameOverProperty());
        diyBgField.textProperty().bindBidirectional(viewModel.diyHomeBgNameProperty());
        diyBgSwitch.selectedProperty().bindBidirectional(viewModel.diyHomeBgProperty());
        diyBgInputGroup.managedProperty().bind(diyBgSwitch.selectedProperty());
        diyBgInputGroup.visibleProperty().bind(diyBgSwitch.selectedProperty());


        noKuJieQuSwitch.selectedProperty().bindBidirectional(Config.setting.noKuJieQuProperty());


        if (viewModel.getGameRootDirSource()==SourceType.DEFAULT){
            gameSourceType.selectToggle(gameSourceType.getToggles().getFirst());
        }else {
            gameSourceType.selectToggle(gameSourceType.getToggles().get(1));
        }
        gameSourceType.selectedToggleProperty().addListener((observableValue, toggle, t1) -> {
            if (t1 instanceof RadioButton radioButton) {
                if (radioButton.getText().equals("默认")){
                    viewModel.setGameRootDirSource(SourceType.DEFAULT);
                } else{
                    viewModel.setGameRootDirSource(SourceType.WE_GAME);
                }
            }
        });

        gameStartAppField.textProperty().bindBidirectional(viewModel.gameAppStartPathProperty());
        gameStartAppGroup.disableProperty().bind(gameStartAppType.selectedToggleProperty().isEqualTo(gameStartAppRadioDefault));
        if (!viewModel.isGameAppStartCustom()){
            gameStartAppType.selectToggle(gameStartAppType.getToggles().getFirst());
        }else {
            gameStartAppType.selectToggle(gameStartAppType.getToggles().get(1));
        }


        appName.setText(Config.appTitle);
        appVersion.setText(Config.version);
        appAuthor.setText(Config.appAuthor);


        appIconIv.setFitWidth(80);
        appIconIv.setFitHeight(80);
        appIconIv.setImage(new Image(FXResourcesLoader.load("image/icon.png"),80,80,true,true,true));

        titlebarSwitch.selectedProperty().bindBidirectional(viewModel.changeTitlebarProperty());
        versionCheckSwitch.selectedProperty().bindBidirectional(viewModel.checkNewVersionProperty());


        if (Config.setting.getCloseEvent() >= 0 && Config.setting.getCloseEvent() <= 2)
            closeEventToggleGroup.selectToggle(closeEventToggleGroup.getToggles().get(Config.setting.getCloseEvent()));
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
    void setGameDir(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择鸣潮安装根目录");
        File file = directoryChooser.showDialog(gameDirField.getScene().getWindow());
        if (file != null) {
            File startApp = null;
            if (Config.setting.getGameRootDirSource() == SourceType.DEFAULT){
                startApp = new File(file.getAbsolutePath() + File.separator + "launcher.exe");
            }else if (Config.setting.getGameRootDirSource() == SourceType.WE_GAME){
                startApp = new File(file.getAbsolutePath() + File.separator + "Wuthering Waves.exe");
            }
            if (startApp != null && startApp.exists()) {
                gameDirField.setText(file.getAbsolutePath());
            }else {
                gameDirField.setText(file.getAbsolutePath());
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.WARNING,"游戏根目录选择存在错误，请重新选择"));
            }
        }
    }

    @FXML
    void toWeb(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI("https://github.com/leck995/WutheringWavesTool"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void setBgFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择背景图片");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("jpg,jpeg,png,bmp","*.png","*.jpg","*.jpeg","*.bmp"));
        File file = fileChooser.showOpenDialog(gameDirField.getScene().getWindow());
        if (file != null) {
            viewModel.setBgFile(file);

        }
    }

    @FXML
    void toIssues(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI("https://github.com/leck995/WutheringWavesTool/issues"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void toQQGroup(ActionEvent event) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString("234294078");
        clipboard.setContent(content);
        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.SUCCESS,"已复制群号，可到QQ中搜索加入"));
    }

    @FXML
    void toSupport(ActionEvent event) {
        Label title = new Label("感谢支持");
        title.getStyleClass().add(Styles.TITLE_2);
        Label tip1 =new Label("助手的开发耗费了开发者大量的时间与精力，如果助手对您有所帮助且您财力有余，可以赞助一下开发者，您的支持将促进助手的开发与完善。");
        tip1.setWrapText(true);
        tip1.setPrefWidth(450);
        tip1.setMinHeight(80);
        Image image =new Image(FXResourcesLoader.load("image/support.png"),400,350,true,true,true);
        ImageView iv = new ImageView(image);

        Label tip2 =new Label("您可以按照如下格式附上留言:");
        Label tip3 =new Label("鸣潮助手 @[称呼]:[消息]");
        VBox center = new VBox(5.0,tip1,iv,tip2,tip3);

        Hyperlink browserBtn = new Hyperlink("查看赞助名单");
        browserBtn.setOnAction(actionEvent -> {
            try {
                Desktop.getDesktop().browse(new URI(Config.URL_SUPPORT_LIST));
            } catch (IOException | URISyntaxException e) {
                LOG.error("打开赞助名单失败{}",e.getMessage());
            }
        });

        Button cancelBtn = new Button("取消");
        cancelBtn.setCancelButton(true);
        JFXDialogLayout dialogLayout=new JFXDialogLayout();
        dialogLayout.setHeading(title);
        dialogLayout.setBody(center);
        dialogLayout.setActions(browserBtn,cancelBtn);
        dialogLayout.setPrefSize(500,500);
        MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG,dialogLayout);
    }

    @FXML
    void setGameApp(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择鸣潮启动程序");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("exe","*.exe"));
        File file = fileChooser.showOpenDialog(gameDirField.getScene().getWindow());
        if (file != null) {
            gameStartAppField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    void toWiki(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI("https://github.com/leck995/WutheringWavesTool/wiki"));
        } catch (IOException | URISyntaxException e) {
            LOG.info(e.getMessage());
        }
    }

    @FXML
    void setCloseEvent(ActionEvent event) {
        Object source = event.getSource();
        if (source instanceof RadioButton button) {
            switch (button.getAccessibleText()) {
                case "0" -> Config.setting.setCloseEvent(0);
                case "1" -> Config.setting.setCloseEvent(1);
                case "2" -> Config.setting.setCloseEvent(2);
            }
        }
    }

}