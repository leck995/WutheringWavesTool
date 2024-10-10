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
        gameStartAppType.selectedToggleProperty().addListener((observableValue, toggle, t1) -> {
            if (t1 instanceof RadioButton radioButton) {
                if (radioButton.getText().equals("默认")){
                    viewModel.setGameAppStartCustom(false);
                    gameStartAppField.setText("Wuthering Waves.exe");
                } else{
                    viewModel.setGameAppStartCustom(true);
                    gameStartAppField.setText("");
                }
            }
        });






        appName.setText(Config.appTitle);
        appVersion.setText(Config.version);
        appAuthor.setText(Config.appAuthor);


        appIconIv.setFitWidth(80);
        appIconIv.setFitHeight(80);
        appIconIv.setImage(new Image(FXResourcesLoader.load("image/icon.png"),80,80,true,true,true));

        titlebarSwitch.selectedProperty().bindBidirectional(viewModel.changeTitlebarProperty());
        versionCheckSwitch.selectedProperty().bindBidirectional(viewModel.checkNewVersionProperty());

    }
    @FXML
    void setGameDir(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择鸣潮安装根目录");
        File file = directoryChooser.showDialog(gameDirField.getScene().getWindow());
        if (file != null) {
            gameDirField.setText(file.getAbsolutePath());
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
        Label tip1 =new Label("助手的开发耗费了开发者大量的时间与精力，如果助手对您有所帮助且您财力有余，可以支持一下开发者，让开发者有更多激情去完成助手的后续开发，感谢。");
        tip1.setWrapText(true);
        tip1.setPrefWidth(450);
        tip1.setMinHeight(80);
        Image image =new Image(FXResourcesLoader.load("image/support.png"),400,350,true,true,true);
        ImageView iv = new ImageView(image);

        Label tip2 =new Label("您可以按照如下格式附上留言:");
        Label tip3 =new Label("[称呼]:[消息]");
        VBox center = new VBox(5.0,tip1,iv,tip2,tip3);


        Button cancelBtn = new Button("取消");
        cancelBtn.setCancelButton(true);
        JFXDialogLayout dialogLayout=new JFXDialogLayout();
        dialogLayout.setHeading(title);
        dialogLayout.setBody(center);
        dialogLayout.setActions(cancelBtn);
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

}