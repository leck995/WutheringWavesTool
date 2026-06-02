package cn.tealc.wutheringwavestool.ui.system.redemptionCode;

import cn.tealc.wutheringwavestool.model.RedemptionCodeItem;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.net.URL;
import java.util.ResourceBundle;

public class RedemptionCodeView implements FxmlView<RedemptionCodeViewModel>, Initializable {
    @InjectViewModel
    private RedemptionCodeViewModel viewModel;

    @FXML
    private ListView<RedemptionCodeItem> cnCodeListView;
    @FXML
    private ListView<RedemptionCodeItem> globalCodeListView;
    @FXML
    private Button refreshBtn;

    private FontIcon refreshIcon;
    private RotateTransition refreshRotate;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cnCodeListView.setItems(viewModel.getCnCodeList());
        cnCodeListView.setCellFactory((ListView<RedemptionCodeItem> listView) -> new RedemptionCodeCell());

        globalCodeListView.setItems(viewModel.getGlobalCodeList());
        globalCodeListView.setCellFactory((ListView<RedemptionCodeItem> listView) -> new RedemptionCodeCell());

        refreshIcon = (FontIcon) refreshBtn.getGraphic();
        refreshRotate = new RotateTransition(Duration.seconds(0.8), refreshIcon);
        refreshRotate.setByAngle(360);
        refreshRotate.setCycleCount(1);

        viewModel.loadingProperty().addListener((observable, oldValue, loading) -> {
            if (!loading) {
                PauseTransition cooldown = new PauseTransition(Duration.seconds(5));
                cooldown.setOnFinished(e -> refreshBtn.setDisable(false));
                cooldown.play();
            }
        });
    }

    @FXML
    void refresh(ActionEvent event) {
        refreshBtn.setDisable(true);
        refreshIcon.setRotate(0);
        refreshRotate.playFromStart();
        viewModel.loadRedemptionCodes();
    }

    static class RedemptionCodeCell extends ListCell<RedemptionCodeItem> {
        private final Label codeLabel = new Label();
        private final Label rewardLabel = new Label();
        private final Label timeLabel = new Label();
        private final Label descriptionLabel = new Label();
        private final Label contributorsLabel = new Label();
        private final Label validLabel = new Label();
        private final FontIcon copyIcon = new FontIcon(Material2AL.CONTENT_COPY);
        private final Button copyBtn = new Button(null, copyIcon);
        private final HBox hbox;
        private PauseTransition copyRevert;

        public RedemptionCodeCell() {
            codeLabel.getStyleClass().add("code-label");
            rewardLabel.getStyleClass().add("reward-label");
            timeLabel.getStyleClass().add("time-label");
            descriptionLabel.getStyleClass().add("description-label");
            contributorsLabel.getStyleClass().add("contributors-label");
            validLabel.getStyleClass().add("valid-label");
            copyBtn.getStyleClass().add("copy-btn");

            copyBtn.setOnAction(event -> {
                RedemptionCodeItem item = getItem();
                if (item != null) {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(item.getKey());
                    Clipboard.getSystemClipboard().setContent(content);
                    if (copyRevert != null) {
                        copyRevert.stop();
                    }
                    copyIcon.setIconCode(Material2AL.DONE);
                    copyRevert = new PauseTransition(Duration.seconds(1.5));
                    copyRevert.setOnFinished(e -> copyIcon.setIconCode(Material2AL.CONTENT_COPY));
                    copyRevert.play();
                }
            });

            VBox infoBox = new VBox(3.0, codeLabel, descriptionLabel, rewardLabel, timeLabel, contributorsLabel);
            infoBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            hbox = new HBox(10.0, infoBox, validLabel, copyBtn);
            hbox.getStyleClass().add("code-item");
            hbox.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(RedemptionCodeItem item, boolean empty) {
            super.updateItem(item, empty);
            if (!empty && item != null) {
                copyIcon.setIconCode(Material2AL.CONTENT_COPY);
                if (copyRevert != null) {
                    copyRevert.stop();
                }
                codeLabel.setText(item.getKey());
                String description = item.getDescription();
                if (description != null && !description.isEmpty()) {
                    descriptionLabel.setText(description);
                    descriptionLabel.setVisible(true);
                    descriptionLabel.setManaged(true);
                } else {
                    descriptionLabel.setVisible(false);
                    descriptionLabel.setManaged(false);
                }
                rewardLabel.setText(item.getReward());
                String timeRange = item.getStartTime() + " ~ " + item.getEndTime();
                timeLabel.setText(timeRange);
                String contributors = item.getContributors();
                if (contributors != null && !contributors.isEmpty()) {
                    contributorsLabel.setText("感谢提供者: " + contributors);
                    contributorsLabel.setVisible(true);
                    contributorsLabel.setManaged(true);
                } else {
                    contributorsLabel.setVisible(false);
                    contributorsLabel.setManaged(false);
                }
                if (item.isValid()) {
                    validLabel.setText("有效");
                    validLabel.setTextFill(Color.GREEN);
                    hbox.getStyleClass().remove("invalid");
                } else {
                    validLabel.setText("无效");
                    validLabel.setTextFill(Color.RED);
                    if (!hbox.getStyleClass().contains("invalid")) {
                        hbox.getStyleClass().add("invalid");
                    }
                }
                copyBtn.setVisible(true);
                setGraphic(hbox);
            } else {
                codeLabel.setText(null);
                descriptionLabel.setText(null);
                descriptionLabel.setVisible(false);
                descriptionLabel.setManaged(false);
                rewardLabel.setText(null);
                timeLabel.setText(null);
                contributorsLabel.setText(null);
                contributorsLabel.setVisible(false);
                contributorsLabel.setManaged(false);
                validLabel.setText(null);
                copyBtn.setVisible(false);
                setGraphic(null);
            }
        }
    }
}
