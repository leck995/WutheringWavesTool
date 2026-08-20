package cn.tealc.wutheringwavestool.ui.game;

import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.game.GameTime;
import cn.tealc.wutheringwavestool.ui.component.EmptyTipPane;
import cn.tealc.wutheringwavestool.util.AlterBuilder;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableCell;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.io.File;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class GameTimeView implements FxmlView<GameTimeViewModel>, Initializable {
    @InjectViewModel
    private GameTimeViewModel viewModel;
    @FXML
    private ComboBox<String> accountComboBox;
    @FXML
    private Label allTotalTimeLabel;
    @FXML
    private Label currentDayLabel;
    @FXML
    private ProgressBar currentProgress;
    @FXML
    private Label currentTimeLabel;
    @FXML
    private Label currentTotalTimeLabel;
    @FXML
    private Label currentUserName;
    @FXML
    private ImageView headImageView;
    @FXML
    private ProgressBar totalProgress;
    @FXML
    private LineChart<String, Double> lineChart;

    @FXML
    private ToggleButton tableToggleBtn;
    @FXML
    private VBox chartLayout;
    @FXML
    private VBox tableLayout;
    @FXML
    private TableView<GameTime> recordTable;
    @FXML
    private TableColumn<GameTime, String> colDate;
    @FXML
    private TableColumn<GameTime, String> colStartTime;
    @FXML
    private TableColumn<GameTime, String> colEndTime;
    @FXML
    private TableColumn<GameTime, String> colDuration;
    @FXML
    private TableColumn<GameTime, Void> colActions;
    @FXML
    private StackPane root;
    @FXML
    private AnchorPane content;

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Circle circle = new Circle(40, 40, 40);
        headImageView.setClip(circle);
        headImageView.setFitWidth(80);
        headImageView.setFitHeight(80);
        if (Config.setting().getHomeViewIcon() != null) {
            File roleIVFile = LocalResourcesManager.homeIcon();
            if (roleIVFile.exists()) {
                headImageView.setImage(new Image(roleIVFile.toURI().toString(), 80, 80, true, true, true));
            } else {
                headImageView.setImage(new Image(FXResourcesLoader.load("image/icon.png"), 80, 80, true, true, true));
            }
        } else {
            headImageView.setImage(new Image(FXResourcesLoader.load("image/icon.png"), 80, 80, true, true, true));
        }

        lineChart.setData(viewModel.getChartData());
        currentDayLabel.textProperty().bind(viewModel.currentDayTextProperty());
        currentTimeLabel.textProperty().bind(viewModel.currentTimeTextProperty());
        currentTotalTimeLabel.textProperty().bind(viewModel.currentTotalTimeTextProperty());
        currentUserName.textProperty().bind(viewModel.currentUserNameProperty());
        currentProgress.progressProperty().bind(viewModel.currentProgressValueProperty());
        allTotalTimeLabel.textProperty().bind(viewModel.allTotalTimeTextProperty());
        totalProgress.progressProperty().bind(viewModel.totalProgressValueProperty());
        accountComboBox.setItems(viewModel.getUserInfoList());
        accountComboBox.getSelectionModel().select(viewModel.getUserIndex());
        accountComboBox.getSelectionModel().selectedIndexProperty().addListener(
                (observableValue, number, t1) -> {
                    viewModel.updateIndex(t1.intValue());
                    if (tableLayout.isVisible()) {
                        viewModel.refreshTableData();
                    }
                });

        setupTable();
        setupToggle();

        MvvmFX.getNotificationCenter().subscribe(NotificationKey.HOME_GAME_TIME_UPDATE,
                (s, objects) -> {
                    viewModel.freshWithAccount();
                    if (tableLayout.isVisible()) {
                        viewModel.refreshTableData();
                    }
                });


        EmptyTipPane tipPane = new EmptyTipPane("无记录","请使用助手启动游戏游玩一次", Material2MZ.SENTIMENT_DISSATISFIED);
        tipPane.visibleProperty().bind(viewModel.emptyProperty());
        content.visibleProperty().bind(viewModel.emptyProperty().not());
        root.getChildren().addFirst(tipPane);


    }

    private void setupToggle() {
        tableToggleBtn.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            chartLayout.setVisible(!isSelected);
            chartLayout.setManaged(!isSelected);
            tableLayout.setVisible(isSelected);
            tableLayout.setManaged(isSelected);
            if (isSelected) {
                viewModel.refreshTableData();
            }
        });
    }

    private void setupTable() {
        recordTable.setItems(viewModel.getTableData());
        recordTable.setFixedCellSize(40);

        colDate.setCellValueFactory(data -> {
            String date = data.getValue().getGameDate();
            return new SimpleStringProperty(date != null ? date : "");
        });

        colStartTime.setCellValueFactory(data -> {
            Long ts = data.getValue().getStartTime();
            return new SimpleStringProperty(ts != null ? formatTimestamp(ts) : "");
        });

        colEndTime.setCellValueFactory(data -> {
            Long ts = data.getValue().getEndTime();
            return new SimpleStringProperty(ts != null ? formatTimestamp(ts) : "");
        });

        colDuration.setCellValueFactory(data -> {
            Long dur = data.getValue().getDuration();
            double minutes = dur != null ? dur / 60000.0 : 0;
            return new SimpleStringProperty(String.format("%.0f", minutes));
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("删除");
            {
                deleteBtn.getStyleClass().addAll(Styles.DANGER, Styles.SMALL,Styles.FLAT);
                deleteBtn.setGraphic(new FontIcon(Material2AL.DELETE_OUTLINE));
                deleteBtn.setOnAction(e -> {
                    JFXDialogLayout dialogLayout = AlterBuilder.create()
                            .danger()
                            .title("警告")
                            .message("确认删除吗？")
                            .ok("删除",event -> {
                                GameTime item = getTableView().getItems().get(getIndex());
                                viewModel.deleteRecord(item);
                            })
                            .cancel()
                            .build();
                    NotificationManager.alert(dialogLayout);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    @FXML
    private void addRecord() {
        String currentRoleId = viewModel.getUserInfoList().isEmpty() ? ""
                : viewModel.getUserInfoList().get(viewModel.getUserIndex());
        GameTimeAddView dialog = new GameTimeAddView(currentRoleId);
        MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG, dialog);
    }

    private static String formatTimestamp(Long ts) {
        if (ts == null || ts == 0) return "";
        return TIMESTAMP_FMT.format(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()));
    }

}