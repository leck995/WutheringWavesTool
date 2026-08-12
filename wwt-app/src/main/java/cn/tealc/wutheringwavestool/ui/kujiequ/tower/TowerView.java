package cn.tealc.wutheringwavestool.ui.kujiequ.tower;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import com.kuro.kujiequ.model.roleData.Role;
import com.kuro.kujiequ.model.towerData.Difficulty;
import com.kuro.kujiequ.model.towerData.Floor;
import com.kuro.kujiequ.model.towerData.SimpleRole;
import com.kuro.kujiequ.model.towerData.TowerArea;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Pair;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-15 23:40
 */
public class TowerView implements FxmlView<TowerViewModel>, Initializable {
    private static final int FLOOR_MAX_STAR = 3;

    @InjectViewModel
    private TowerViewModel viewModel;
    @FXML
    private ListView<Difficulty> difficuityListview;
    @FXML
    private GridPane areaGridPane;
    @FXML
    private Label seasonEndTimeLabel;
    @FXML
    private Label title;
    @FXML
    private ListView<Pair<Long, Pair<String, String>>> towerHistoryListview;
    @FXML
    private VBox historySection;
    @FXML
    private Label totalStarLabel;
    @FXML
    private HBox summaryBox;
    @FXML
    private ProgressBar starProgressBar;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        title.textProperty().bind(viewModel.titleProperty());
        seasonEndTimeLabel.textProperty().bind(viewModel.seasonEndTimeProperty());

        difficuityListview.setItems(viewModel.getDifficultyList());
        difficuityListview.setCellFactory(difficultyListView -> new DifficultyCell());
        viewModel.getDifficultyList().addListener((ListChangeListener<Difficulty>) change -> {
            if (!change.getList().isEmpty()) {
                difficuityListview.getSelectionModel().selectFirst();
            }
        });

        difficuityListview.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                viewModel.changeDifficulty(newValue);
                boolean show = newValue.getDifficulty() == 3;
                seasonEndTimeLabel.setVisible(show);
                seasonEndTimeLabel.setManaged(show);
                historySection.setVisible(show);
                historySection.setManaged(show);
                towerHistoryListview.getSelectionModel().clearSelection();
            }
        });

        viewModel.getTowerAreaList().addListener((ListChangeListener<? super TowerArea>) change -> {
            areaGridPane.getChildren().clear();
            List<? extends TowerArea> areas = change.getList();
            for (int i = 0; i < areas.size(); i++) {
                AreaCell areaCell = new AreaCell(areas.get(i));
                GridPane.setHgrow(areaCell, Priority.ALWAYS);
                GridPane.setVgrow(areaCell, Priority.ALWAYS);
                areaGridPane.add(areaCell, i % 3, i / 3);
            }
            updateSummary(areas);
        });

        towerHistoryListview.setItems(viewModel.getTowerHistoryList());
        towerHistoryListview.setCellFactory(difficultyListView -> new HistoryCell());
        towerHistoryListview.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                viewModel.changeHistory(newValue.getKey());
                difficuityListview.getSelectionModel().clearSelection();
            }
        });
        Label historyPlaceholder = new Label("尚无往期记录");
        historyPlaceholder.getStyleClass().add(Styles.TEXT_MUTED);
        towerHistoryListview.setPlaceholder(historyPlaceholder);

        // 默认隐藏历史区，待选中难度 3 后再显示
        historySection.setVisible(false);
        historySection.setManaged(false);
        seasonEndTimeLabel.setVisible(false);
        seasonEndTimeLabel.setManaged(false);
    }

    private void updateSummary(List<? extends TowerArea> areas) {
        int star = 0;
        int maxStar = 0;
        for (TowerArea area : areas) {
            star += area.getStar();
            maxStar += area.getMaxStar();
        }
        totalStarLabel.setText(String.format("%d / %d", star, maxStar));
        starProgressBar.setProgress(maxStar > 0 ? (double) star / maxStar : 0);
        summaryBox.setVisible(!areas.isEmpty());
        summaryBox.setManaged(!areas.isEmpty());
    }


    static class DifficultyCell extends ListCell<Difficulty> {
        private final HBox child;
        private final Label title = new Label();
        private final Label star = new Label();

        public DifficultyCell() {
            title.getStyleClass().add("tower-name");
            star.getStyleClass().add("tower-star");

            FontIcon areaIcon = new FontIcon(Material2AL.ADJUST);
            areaIcon.getStyleClass().add("tower-area-icon");

            FontIcon fontIcon = new FontIcon(Material2MZ.STAR);
            star.setGraphic(fontIcon);
            star.setContentDisplay(ContentDisplay.RIGHT);
            child = new HBox(8, areaIcon, title, new Spacer(), star);
            child.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(title, Priority.ALWAYS);
            setGraphic(child);
            child.getStyleClass().add("tower-cell");
        }

        @Override
        protected void updateItem(Difficulty difficulty, boolean empty) {
            super.updateItem(difficulty, empty);
            if (!empty) {
                setDisable(false);
                setVisible(true);
                int sum = difficulty.getTowerAreaList().stream().mapToInt(TowerArea::getStar).sum();
                int max = difficulty.getTowerAreaList().stream().mapToInt(TowerArea::getMaxStar).sum();
                title.setText(difficulty.getDifficultyName());
                star.setText(String.format("%2d", sum));
                if (sum == max && max > 0) {
                    if (!star.getStyleClass().contains("full-star")) {
                        star.getStyleClass().add("full-star");
                    }
                } else {
                    star.getStyleClass().remove("full-star");
                }
                setGraphic(child);
            } else {
                title.setText(null);
                star.setText(null);
                setDisable(true);
                setVisible(false);
                setGraphic(null);
            }
        }
    }

    static class HistoryCell extends ListCell<Pair<Long, Pair<String, String>>> {
        private final HBox child = new HBox(8);
        private final VBox dateBox = new VBox(1);
        private final Label startDate = new Label();
        private final Label endDate = new Label();
        private final Label marker = new Label();

        public HistoryCell() {
            child.setAlignment(Pos.CENTER_LEFT);
            marker.getStyleClass().add("history-marker");
            startDate.getStyleClass().add("history-start-date");
            endDate.getStyleClass().add("history-end-date");
            dateBox.getChildren().addAll(startDate, endDate);
            HBox.setHgrow(dateBox, Priority.ALWAYS);
            child.getChildren().addAll(marker, dateBox);
            child.setMaxWidth(Double.MAX_VALUE);
            child.getStyleClass().addAll("tower-cell", "classic-history-cell");
            setMaxWidth(Double.MAX_VALUE);
        }

        @Override
        protected void updateItem(Pair<Long, Pair<String, String>> pair, boolean empty) {
            super.updateItem(pair, empty);
            if (!empty) {
                setDisable(false);
                startDate.setText(pair.getValue().getKey());
                endDate.setText(pair.getValue().getValue());
                setGraphic(child);
            } else {
                startDate.setText(null);
                endDate.setText(null);
                setGraphic(null);
                setDisable(true);
            }
        }
    }

    class AreaCell extends VBox {
        public AreaCell(TowerArea towerArea) {
            setMinWidth(0);
            setMaxWidth(Double.MAX_VALUE);
            setMaxHeight(Double.MAX_VALUE);
            setSpacing(6);
            getStyleClass().addAll("area", "classic-area-card");

            // --- 标题区：塔名称在上，星数在下 ---
            VBox header = new VBox(3);
            header.setAlignment(Pos.CENTER);
            header.getStyleClass().add("area-header");

            Label areaTitle = new Label(towerArea.getAreaName());
            areaTitle.getStyleClass().add("area-title");

            Label areaScore = new Label(String.format("%d / %d", towerArea.getStar(), towerArea.getMaxStar()));
            areaScore.getStyleClass().add("area-score");
            FontIcon starIcon = new FontIcon(Material2MZ.STAR);
            areaScore.setGraphic(starIcon);
            areaScore.setContentDisplay(ContentDisplay.RIGHT);
            areaScore.getStyleClass().add("full-star");

            header.getChildren().addAll(areaTitle, areaScore);

            Separator separator = new Separator(Orientation.HORIZONTAL);
            getChildren().addAll(header, separator);

            // --- 楼层列表 ---
            VBox floorList = new VBox(2);
            floorList.getStyleClass().add("floor-list");

            List<Floor> floors = towerArea.getFloorList();
            if (floors == null || floors.isEmpty()) {
                Label empty = new Label("暂无楼层数据");
                empty.getStyleClass().addAll(Styles.TEXT_MUTED, "empty-hint");
                floorList.getChildren().add(empty);
            } else {
                for (Floor floor : floors) {
                    floorList.getChildren().add(buildFloorRow(floor));
                }
            }
            getChildren().add(floorList);
        }

        private VBox buildFloorRow(Floor floor) {
            VBox row = new VBox(5);
            row.getStyleClass().add("floor-row");
            row.setAlignment(Pos.TOP_LEFT);

            Label floorName = new Label(String.format("第%d层", floor.getFloor()));
            floorName.getStyleClass().add("floor-title");

            HBox starBox = new HBox();
            starBox.getStyleClass().add("star-box");
            starBox.setAlignment(Pos.CENTER_LEFT);
            int earned = Math.max(0, Math.min(floor.getStar(), FLOOR_MAX_STAR));
            for (int i = 0; i < FLOOR_MAX_STAR; i++) {
                boolean unlocked = i < earned;
                FontIcon star = new FontIcon(unlocked ? Material2MZ.STAR : Material2MZ.STAR_BORDER);
                star.getStyleClass().add(unlocked ? "floor-star-earned" : "floor-star-empty");
                starBox.getChildren().add(star);
            }

            HBox floorContent = new HBox(7);
            floorContent.getStyleClass().add("floor-content");
            floorContent.setAlignment(Pos.CENTER_LEFT);

            HBox roleRow = new HBox(5);
            roleRow.getStyleClass().add("role-row");
            roleRow.setAlignment(Pos.CENTER_RIGHT);

            if (floor.getRoleList() != null && !floor.getRoleList().isEmpty()) {
                for (SimpleRole role : floor.getRoleList()) {
                    roleRow.getChildren().add(buildRoleItem(role));
                }
            } else {
                Label empty = new Label("暂无数据");
                empty.getStyleClass().addAll(Styles.TEXT_MUTED, "floor-empty-text");
                roleRow.getChildren().add(empty);
            }

            Pane contentSpacer = new Pane();
            HBox.setHgrow(contentSpacer, Priority.ALWAYS);
            floorContent.getChildren().addAll(starBox, contentSpacer, roleRow);
            row.getChildren().addAll(floorName, floorContent);
            return row;
        }

        private StackPane buildRoleItem(SimpleRole role) {
            StackPane roleItem = new StackPane();
            roleItem.getStyleClass().add("role-item");

            ImageView roleIv = new ImageView();
            Image image = LocalResourcesManager.header(role.getRoleId(), 40, 40);
            roleIv.setImage(image);
            roleIv.setFitWidth(40);
            roleIv.setFitHeight(40);
            Circle circle = new Circle(20, 20, 20);
            roleIv.setClip(circle);
            roleItem.getChildren().add(roleIv);

            Role roleDetail = viewModel.getRoleMap().get(role.getRoleId());
            if (roleDetail != null) {
                Label num = new Label(String.valueOf(roleDetail.getChainUnlockNum()));
                num.getStyleClass().add("chain-unlock-num");
                StackPane.setAlignment(num, Pos.TOP_RIGHT);
                roleItem.getChildren().add(num);
            }
            return roleItem;
        }
    }
}
