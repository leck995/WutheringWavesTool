package cn.tealc.wutheringwavestool.ui.gacha;

import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.CloudFileItem;
import cn.tealc.wutheringwavestool.model.CloudUploadItem;
import cn.tealc.wutheringwavestool.ui.component.BaseDialog;
import cn.tealc.wutheringwavestool.util.DialogBuilder;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.URL;
import java.util.ResourceBundle;

public class CloudBackupView extends BaseDialog implements FxmlView<CloudBackupViewModel>, Initializable {

    @InjectViewModel
    private CloudBackupViewModel viewModel;

    @FXML
    private TableView<CloudFileItem> downloadTable;

    @FXML
    private TableColumn<CloudFileItem, String> fileNameCol;

    @FXML
    private TableColumn<CloudFileItem, String> dateCol;

    @FXML
    private TableColumn<CloudFileItem, String> downloadActionCol;

    @FXML
    private Button refreshBtn;

    @FXML
    private Label loadingLabel;

    @FXML
    private VBox errorBox;

    @FXML
    private Button errorRetryBtn;

    @FXML
    private TableView<CloudUploadItem> uploadTable;

    @FXML
    private TableColumn<CloudUploadItem, String> playerIdTCol;

    @FXML
    private TableColumn<CloudUploadItem, String> funcTCol;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        downloadTable.setItems(viewModel.getFileList());
        uploadTable.setItems(viewModel.getUploadList());

        loadingLabel.visibleProperty().bind(viewModel.loadingProperty());
        errorBox.visibleProperty().bind(viewModel.failedProperty());
        downloadTable.visibleProperty().bind(viewModel.loadedProperty());

        refreshBtn.setOnAction(e -> viewModel.refresh());
        errorRetryBtn.setOnAction(e -> viewModel.refresh());

        playerIdTCol.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getPlayerId()));

        funcTCol.setCellFactory(param -> new TableCell<>() {
            private final Button uploadBtn = new Button(null, new FontIcon(Material2AL.CLOUD_UPLOAD));

            {
                uploadBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.ACCENT);
                uploadBtn.setOnAction(e -> {
                    CloudUploadItem item = getTableRow().getItem();
                    if (item != null) {
                        showUploadDialog(item);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : uploadBtn);
                setText(null);
            }
        });

        fileNameCol.setCellValueFactory(param -> {
            String name = param.getValue().getOriginalName();
            int idx = name.indexOf('-');
            return new SimpleStringProperty(idx > 0 ? name.substring(0, idx) : name);
        });

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        dateCol.setCellValueFactory(param -> {
            String createdAt = param.getValue().getCreatedAt();
            if (createdAt != null && !createdAt.isEmpty()) {
                try {
                    LocalDateTime dt = LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    return new SimpleStringProperty(dt.format(dateFormatter));
                } catch (Exception e) {
                    return new SimpleStringProperty(createdAt);
                }
            }
            return new SimpleStringProperty("");
        });

        downloadActionCol.setCellFactory(param -> new TableCell<>() {
            private final Button downloadBtn = new Button(null, new FontIcon(Material2AL.CLOUD_DOWNLOAD));
            private final Button deleteBtn = new Button(null, new FontIcon(Material2AL.DELETE));
            private final HBox box = new HBox(4, downloadBtn, deleteBtn);

            {
                downloadBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.ACCENT);
                downloadBtn.setOnAction(e -> {
                    CloudFileItem item = getTableRow().getItem();
                    if (item != null) {
                        showDownloadDialog(item);
                    }
                });

                deleteBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.DANGER);
                deleteBtn.setOnAction(e -> {
                    CloudFileItem item = getTableRow().getItem();
                    if (item != null) {
                        showDeleteDialog(item);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
                setText(null);
            }
        });
    }


    public void showDownloadDialog(CloudFileItem item){
        String name = item.getOriginalName();
        int idx = name.indexOf('-');
        String player = idx > 0 ? name.substring(0, idx) : name;
        Button button = new Button("下载");
        button.setCancelButton(true);
        button.getStyleClass().add(Styles.ACCENT);
        button.setOnAction(event -> {
            viewModel.downloadJson(item);
        });
        JFXDialogLayout dialogLayout = DialogBuilder.create()
                .title(String.format("确认下载 %s 的备份吗？", player))
                .message("该操作会下载并覆盖本地记录")
                .buttons(button)
                .cancel()
                .build();
        NotificationManager.dialog(dialogLayout);
    }

    public void showUploadDialog(CloudUploadItem item){
        String player = item.getPlayerId();
        Button button = new Button("上传");
        button.setCancelButton(true);
        button.getStyleClass().add(Styles.ACCENT);
        button.setOnAction(event -> {
            viewModel.uploadJson(item);
        });

        JFXDialogLayout dialogLayout = DialogBuilder.create()
                .title(String.format("确认上传 %s 的抽卡数据吗？", player))
                .message("同一个游戏账号的数据，每60分钟只允许上传一次")
                .buttons(button)
                .cancel()
                .build();
        NotificationManager.dialog(dialogLayout);
    }


    public void showDeleteDialog(CloudFileItem item){
        String name = item.getOriginalName();
        int idx = name.indexOf('-');
        String player = idx > 0 ? name.substring(0, idx) : name;

        Button button = new Button("删除");
        button.setCancelButton(true);
        button.getStyleClass().add(Styles.DANGER);
        button.setOnAction(event -> {
            viewModel.deleteJson(item);
        });
        JFXDialogLayout dialogLayout = DialogBuilder.create()
                .title(String.format("确认删除 %s 的备份吗？", player))
                .message("注意该操作不可逆")
                .buttons(button)
                .cancel()
                .build();
        NotificationManager.dialog(dialogLayout);
    }
}
