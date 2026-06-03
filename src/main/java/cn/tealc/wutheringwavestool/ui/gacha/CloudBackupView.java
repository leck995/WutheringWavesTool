package cn.tealc.wutheringwavestool.ui.gacha;

import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.CloudFileItem;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.thread.system.gachaUpload.CloudFileDownloadTask;
import cn.tealc.wutheringwavestool.ui.component.BaseDialog;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        downloadTable.setItems(viewModel.getFileList());

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
            private final Button downloadBtn = new Button(null,new FontIcon(Material2AL.CLOUD_DOWNLOAD));

            {
                downloadBtn.getStyleClass().addAll(Styles.BUTTON_ICON,Styles.FLAT,Styles.ACCENT);
                downloadBtn.setOnAction(e -> {
                    CloudFileItem item = getTableRow().getItem();
                    if (item != null) {
                        viewModel.downloadJson(item);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(downloadBtn);
                }
                setText(null);
            }
        });
    }
}
