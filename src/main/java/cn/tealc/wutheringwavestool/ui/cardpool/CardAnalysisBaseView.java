package cn.tealc.wutheringwavestool.ui.cardpool;

import atlantafx.base.theme.Styles;
import cn.tealc.fxplugin.FxPlugin;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.plugin.FxPluginManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class CardAnalysisBaseView implements FxmlView<CardAnalysisBaseViewModel>, Initializable {
    @InjectViewModel
    private CardAnalysisBaseViewModel viewModel;
    @FXML
    private StackPane content;
    @FXML
    private ComboBox<String> playerComboBox;

    private Parent commonChild;
    private Parent detailChild;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        playerComboBox.setItems(viewModel.getPlayerList());
        if (!viewModel.getPlayerList().isEmpty()) {
            playerComboBox.getSelectionModel().select(viewModel.getPlayerList().indexOf(viewModel.getPlayer()));
        }
        playerComboBox.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            if (t1 != null) {
                viewModel.changePlayer(t1);
            }
        });


        viewModel.subscribe("update-player", (s, objects) -> {
            playerComboBox.getSelectionModel().select(viewModel.getPlayerList().indexOf(viewModel.getPlayer()));
        });


       toCommonChild();


    }


    @FXML
    void load(ActionEvent event) {
        viewModel.loadFromNet();
    }

    @FXML
    void fresh(ActionEvent event) {
        viewModel.refreshFromNet();
    }

    @FXML
    void export(ActionEvent event) {
        FxPluginManager instance = FxPluginManager.getInstance();
        Optional<FxPlugin> plugin = instance.loadPlugins(1001);
        if (plugin.isPresent()) { //插件存在
            FxPlugin fxPlugin = plugin.get();
            Map<String, Object> params = new HashMap<>();
            fxPlugin.setOnFinished(o -> {
                viewModel.loadFile((String) o);
            });
            Optional<Object> result = fxPlugin.run(params);
            result.ifPresent(o -> {
                MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG, o);
            });
        } else { //不存在
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.analysis.message.type07")));
        }
    }

    @FXML
    void delete(ActionEvent event) {
        if (!playerComboBox.getItems().isEmpty()) {
            JFXDialogLayout layout = new JFXDialogLayout();
            Label title = new Label(LanguageManager.getString("ui.common.warning"));
            title.setStyle(Styles.TITLE_2);
            layout.setHeading(title);
            Label tip = new Label(String.format(LanguageManager.getString("ui.analysis.delete.tip.content"), viewModel.getPlayer()));
            layout.setBody(tip);
            Button okBtn = new Button(LanguageManager.getString("ui.common.ok"));
            Button cancelBtn = new Button(LanguageManager.getString("ui.common.cancel"));
            okBtn.setOnAction(event1 -> {
                viewModel.delete();
                cancelBtn.fireEvent(event1);
            });
            cancelBtn.setCancelButton(true);
            layout.setActions(okBtn, cancelBtn);
            MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG, layout);
        }
    }


    @FXML
    void toCommonChild(ActionEvent event) {
        if (event.getSource() instanceof ToggleButton toggleButton){
            if (toggleButton.isSelected()) {
               toCommonChild();
                if (viewModel.getPoolData() != null) {
                    NotificationManager.publish(NotificationKey.CARD_POOL_USER_UPDATE, viewModel.getPoolData());
                }
            } else {
                toggleButton.setSelected(true);
            }
        }
    }


    private void toCommonChild(){
        if (commonChild == null) {
            ViewTuple<CardCommonAnalysisView, CardCommonAnalysisViewModel> viewTuple = FluentViewLoader.fxmlView(CardCommonAnalysisView.class).load();
            commonChild = viewTuple.getView();
        }
        content.getChildren().setAll(commonChild);
    }

    @FXML
    void toDetailChild(ActionEvent event) {
        if (event.getSource() instanceof ToggleButton toggleButton){
            if (toggleButton.isSelected()) {
                if (detailChild == null) {
                    ViewTuple<CardDetailAnalysisView, CardDetailAnalysisViewModel> viewTuple = FluentViewLoader.fxmlView(CardDetailAnalysisView.class).load();
                    detailChild = viewTuple.getView();
                }
                content.getChildren().setAll(detailChild);
                if (viewModel.getPoolData() != null) {
                    NotificationManager.publish(NotificationKey.CARD_POOL_USER_UPDATE, viewModel.getPoolData());
                }
            } else {
                toggleButton.setSelected(true);
            }
        }
    }

    @FXML
    void snapshot(ActionEvent event) {
        WritableImage image = content.getScene().snapshot(null);
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putImage(image);
        clipboard.setContent(clipboardContent);
        NotificationManager.message(MessageInfo.success(LanguageManager.getString("ui.analysis.message.snapshot.success")));
    }
}