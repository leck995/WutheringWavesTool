package cn.tealc.wutheringwavestool.ui;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.controls.ToggleSwitchSkin;
import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.beans.binding.Bindings;
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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.Pair;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 20:20
 */
public class SettingView implements FxmlView<SettingViewModel>, Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(SettingView.class);
    @InjectViewModel
    private SettingViewModel viewModel;
    @FXML
    private AnchorPane root;
    @FXML
    private Label appAuthor;
    @FXML
    private ImageView appIconIv;
    @FXML
    private Label appName;
    @FXML
    private Label appVersion;
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
    private ToggleSwitch versionCheckSwitch;
    @FXML
    private ToggleSwitch noKuJieQuSwitch;
    @FXML
    private ToggleGroup closeEventToggleGroup;
    @FXML
    private ComboBox<Pair<String, Locale>> languageBox;
    @FXML
    private ToggleGroup fileSourceType;
    @FXML
    private ToggleGroup homeBgType;
    @FXML
    private TextField diyBgDirField;
    @FXML
    private StackPane diyBgDirInputGroup;
    @FXML
    private Spinner<Integer> uiScaleSpinner;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(75, 125, Config.setting.getUiScale(), 5);
        uiScaleSpinner.setValueFactory(valueFactory);
        uiScaleSpinner.valueProperty().addListener((observableValue, integer, t1) -> {
            if (t1 != null) {
                Config.setting.setUiScale(t1);
            }
        });

        startWithAnalysisView.selectedProperty().bindBidirectional(viewModel.startWithAnalysisProperty());

        hideWhenGameStart.setSkin(new ToggleSwitchSkin(hideWhenGameStart));
        hideWhenGameStart.selectedProperty().bindBidirectional(viewModel.hideWhenGameStartProperty());
        exitWhenGameOver.selectedProperty().bindBidirectional(viewModel.exitWhenGameOverProperty());

        diyBgField.textProperty().bindBidirectional(viewModel.diyHomeBgNameProperty());
        diyBgInputGroup.managedProperty().bind(Bindings.equal(1, viewModel.homeBgTypeProperty()));
        diyBgInputGroup.visibleProperty().bind(Bindings.equal(1, viewModel.homeBgTypeProperty()));
        diyBgDirField.textProperty().bindBidirectional(viewModel.homeBgDirProperty());
        diyBgDirInputGroup.managedProperty().bind(Bindings.equal(2, viewModel.homeBgTypeProperty()));
        diyBgDirInputGroup.visibleProperty().bind(Bindings.equal(2, viewModel.homeBgTypeProperty()));


        homeBgType.selectToggle(homeBgType.getToggles().get(viewModel.getHomeBgType()));

        homeBgType.selectedToggleProperty().addListener((observableValue, toggle, t1) -> {
            int index = homeBgType.getToggles().indexOf(t1);
            if (index == 0) {
                viewModel.setHomeBgType(0);
                viewModel.changeBackground();
            } else if (index == 1) {
                viewModel.setHomeBgType(1);
                if (viewModel.getDiyHomeBgName() != null) {
                    viewModel.changeBackground();
                }
            } else if (index == 2) {
                viewModel.setHomeBgType(2);
                if (viewModel.getHomeBgDir() != null) {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.CHANGE_BG);
                }
            }
        });
        noKuJieQuSwitch.selectedProperty().bindBidirectional(Config.setting.noKuJieQuProperty());
        noKuJieQuSwitch.selectedProperty().addListener(observableValue -> {
            NotificationManager.publish(NotificationKey.CHANGE_NAV);
        });


        appName.setText(Config.appTitle);
        appVersion.setText(Config.version);
        appAuthor.setText(Config.appAuthor);


        appIconIv.setFitWidth(80);
        appIconIv.setFitHeight(80);
        appIconIv.setImage(new Image(FXResourcesLoader.load("image/icon.png"), 80, 80, true, true, true));

        versionCheckSwitch.selectedProperty().bindBidirectional(viewModel.checkNewVersionProperty());


        if (Config.setting.getCloseEvent() >= 0 && Config.setting.getCloseEvent() <= 2)
            closeEventToggleGroup.selectToggle(closeEventToggleGroup.getToggles().get(Config.setting.getCloseEvent()));


        languageBox.setItems(viewModel.getLanguages());
        for (Pair<String, Locale> language : languageBox.getItems()) {
            if (language.getValue().getLanguage().equals(Config.setting.getLanguage().getLanguage())) {
                languageBox.getSelectionModel().select(language);
            }
        }
        languageBox.getSelectionModel().selectedItemProperty().addListener((observableValue, stringLocalePair, t1) -> {
            if (t1 != null) {
                viewModel.setLanguages(t1.getValue());
            }
        });
        languageBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Pair<String, Locale> stringLocalePair) {
                return stringLocalePair != null ? stringLocalePair.getKey() : null;
            }

            @Override
            public Pair<String, Locale> fromString(String s) {
                return null;
            }
        });


        fileSourceType.getToggles().get(Config.setting.getResourceSource()).setSelected(true);
    }


    @FXML
    void checkVersion(ActionEvent event) {
        viewModel.checkVersion();
    }


    @FXML
    void toWeb(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI("https://github.com/leck995/WutheringWavesTool"));
        } catch (IOException | URISyntaxException e) {
            LOG.warn(e.getMessage());
        }
    }

    @FXML
    void setBgFile(ActionEvent event) {
        int index = homeBgType.getToggles().indexOf(homeBgType.getSelectedToggle());
        if (index == 1) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(LanguageManager.getString("ui.setting.file.background.title"));
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("jpg,jpeg,png,bmp", "*.png", "*.jpg", "*.jpeg", "*.bmp"));
            File file = fileChooser.showOpenDialog(root.getScene().getWindow());
            if (file != null) {
                viewModel.setBgFile(file);
            }
        } else if (index == 2) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(LanguageManager.getString("ui.setting.file.background.title"));
            File file = chooser.showDialog(root.getScene().getWindow());
            if (file != null) {
                File[] files = file.listFiles();
                if (files != null && files.length > 0) {
                    viewModel.setHomeBgDir(file.getAbsolutePath());
                } else {
                    NotificationManager.message(MessageInfo.warning("选中文件夹为空，无法设置为背景文件夹"));
                }
            }
        }
    }

    @FXML
    void toIssues(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI("https://github.com/leck995/WutheringWavesTool/issues"));
        } catch (IOException | URISyntaxException e) {
            LOG.warn(e.getMessage());
        }
    }

    @FXML
    void toQQGroup(ActionEvent event) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(LanguageManager.getString("ui.setting.communication.QQ"));
        clipboard.setContent(content);
        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, new MessageInfo(MessageType.SUCCESS, LanguageManager.getString("ui.setting.communication.QQ.tip")));
    }

    @FXML
    void toSupport(ActionEvent event) {
        Label title = new Label(LanguageManager.getString("ui.setting.sponsor.dialog.title"));
        title.getStyleClass().add(Styles.TITLE_3);
        Label tip1 = new Label(LanguageManager.getString("ui.setting.sponsor.dialog.tip01"));
        tip1.setWrapText(true);
        tip1.setPrefWidth(450);
        tip1.setPrefHeight(80);
        Image image = new Image(FXResourcesLoader.load("image/support.png"), 350, 320, true, true, true);
        ImageView iv = new ImageView(image);

        Label tip2 = new Label(LanguageManager.getString("ui.setting.sponsor.dialog.tip02"));
        Label tip3 = new Label(LanguageManager.getString("ui.setting.sponsor.dialog.tip03"));
        VBox center = new VBox(5.0, tip1, iv, tip2, tip3);

        Hyperlink browserBtn = new Hyperlink(LanguageManager.getString("ui.setting.sponsor.dialog.browser"));
        browserBtn.setOnAction(actionEvent -> {
            try {
                Desktop.getDesktop().browse(new URI(Config.URL_SUPPORT_LIST));
            } catch (IOException | URISyntaxException e) {
                LOG.error("打开赞助名单失败{}", e.getMessage());
            }
        });

        Button cancelBtn = new Button(LanguageManager.getString("ui.common.cancel"));
        cancelBtn.setCancelButton(true);
        JFXDialogLayout dialogLayout = new JFXDialogLayout();
        dialogLayout.setHeading(title);
        dialogLayout.setBody(center);
        dialogLayout.setActions(browserBtn, cancelBtn);
        dialogLayout.setPrefSize(500, 500);
        MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG, dialogLayout);
    }


    @FXML
    void toWiki(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI("https://wave.tealc.fun/#/"));
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

    @FXML
    void setFileSource(ActionEvent event) {
        Object source = event.getSource();
        if (source instanceof RadioButton button) {
            switch (button.getAccessibleText()) {
                case "0" -> {
                    Config.setting.setResourceSource(0);
                }
                case "1" -> {
                    Config.setting.setResourceSource(1);
                }
            }
        }
    }
}