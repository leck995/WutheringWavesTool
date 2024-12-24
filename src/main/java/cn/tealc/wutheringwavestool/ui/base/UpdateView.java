package cn.tealc.wutheringwavestool.ui.base;

import cn.tealc.teafx.stage.TeaStage;
import cn.tealc.teafx.stage.handler.DragWindowHandler;
import cn.tealc.wutheringwavestool.MainApplication;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-27 23:45
 */
public class UpdateView implements FxmlView<UpdateViewModel>, Initializable {
    @InjectViewModel
    private UpdateViewModel viewModel;

    @FXML
    private StackPane root;
    @FXML
    private MenuButton cancelBtn;

    @FXML
    private Button closeBtn;

    @FXML
    private Label dateTime;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Button maxBtn;

    @FXML
    private Button minBtn;
    @FXML
    private Button downloadBtn;

    @FXML
    private Label packageSize;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label progressLabel;

    @FXML
    private HBox titlebar;

    @FXML
    private Label type;
    @FXML
    private Label forceLabel;
    @FXML
    private Label version;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initTitleBar();
        version.textProperty().bind(viewModel.versionProperty());
        type.textProperty().bind(viewModel.typeProperty());
        packageSize.textProperty().bind(viewModel.packageSizeProperty());
        descriptionArea.textProperty().bind(viewModel.descriptionProperty());
        progressLabel.textProperty().bind(viewModel.progressLabelProperty());
        dateTime.textProperty().bind(viewModel.dateTimeProperty());
        progressBar.progressProperty().bind(viewModel.progressValueProperty());
        cancelBtn.disableProperty().bind(viewModel.downloadingProperty());
        downloadBtn.disableProperty().bind(viewModel.downloadingProperty());
        forceLabel.textProperty().bind(viewModel.forceLabelProperty());
    }

    private void initTitleBar(){
        DragWindowHandler handler=new DragWindowHandler((TeaStage) MainApplication.window,true);
        titlebar.setOnMousePressed(handler);
        titlebar.setOnMouseDragged(handler);
        titlebar.setOnMouseReleased(handler);
        titlebar.setOnMouseClicked(handler);

        closeBtn.setOnAction(event -> MainApplication.exit());
        minBtn.setOnAction(event -> MainApplication.window.setIconified(true));
    }

    @FXML
    void nextRemind(ActionEvent event) {
        if (viewModel.isForce()){
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.update.message01")));
        }else {
            Pane parent = (Pane) root.getParent();
            parent.getChildren().remove(root);
        }

    }

    @FXML
    void skipUpdate(ActionEvent event) {
        if (viewModel.isForce()){
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.update.message01")));
        }else {
            viewModel.setSkipVersion();
            Pane parent = (Pane) root.getParent();
            parent.getChildren().remove(root);
        }

    }

    @FXML
    void startUpdate(ActionEvent event) {
        viewModel.downloadZip();
    }

    @FXML
    void toReleaseWebsite(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI("https://github.com/leck995/WutheringWavesTool/releases"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


}