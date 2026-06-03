package cn.tealc.wutheringwavestool.ui.gacha;

import de.saxsys.mvvmfx.FxmlView;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.VBox;

public class CloudBackupView implements FxmlView<CloudBackupViewModel>, Initializable {

    @FXML
    private TreeTableView<?> downloadTreeTable;

    @FXML
    private TableColumn<?, ?> funcTCol;

    @FXML
    private TreeTableColumn<?, ?> funcTTCol;

    @FXML
    private TableColumn<?, ?> jsonFIleTCol;

    @FXML
    private TreeTableColumn<?, ?> jsonTTCol;

    @FXML
    private TableColumn<?, ?> playerIdTCol;

    @FXML
    private VBox root;

    @FXML
    private TableView<?> uploadTable;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
