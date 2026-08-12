package cn.tealc.wutheringwavestool.ui.kujiequ.tower;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
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
    private FlowPane areaFlowPane;
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
    private Label areaProgressLabel;
    @FXML
    private HBox summaryBox;

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
            areaFlowPane.getChildren().clear();
            List<? extends TowerArea> areas = change.getList();
            for (TowerArea towerArea : areas) {
                areaFlowPane.getChildren().add(new AreaCell(towerArea));
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
        int clearedAreas = 0;
        for (TowerArea area : areas) {
            star += area.getStar();
            maxStar += area.getMaxStar();
            if (area.getStar() > 0) {
                clearedAreas++;
            }
        }
        totalStarLabel.setText(String.format("%d / %d", star, maxStar));
        areaProgressLabel.setText(String.format("区域 %d / %d", clearedAreas, areas.size()));
        summaryBox.setVisible(!areas.isEmpty());
        summaryBox.setManaged(!areas.isEmpty());
    }


    static class DifficultyCell extends ListCell<Difficulty> {
        private final StackPane child;
        private final Label title = new Label();
        private final Label star = new Label();

        public DifficultyCell() {
            title.getStyleClass().add("tower-name");
            star.getStyleClass().add("tower-star");

            FontIcon fontIcon = new FontIcon(Material2MZ.STAR_OUTLINE);
            star.setGraphic(fontIcon);
            star.setContentDisplay(ContentDisplay.RIGHT);
            child = new StackPane(title, star);

            StackPane.setAlignment(title, Pos.CENTER_LEFT);
            StackPane.setAlignment(star, Pos.CENTER_RIGHT);
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
        private final HBox child = new HBox();
        private final Label title = new Label();

        public HistoryCell() {
            child.getChildren().add(title);
            title.getStyleClass().add("tower-name");
            title.setMaxWidth(Double.MAX_VALUE);
            title.setEllipsisString("…");
            HBox.setHgrow(title, Priority.ALWAYS);
            child.setMaxWidth(Double.MAX_VALUE);
            child.getStyleClass().add("tower-cell");
            setMaxWidth(Double.MAX_VALUE);
        }

        @Override
        protected void updateItem(Pair<Long, Pair<String, String>> pair, boolean empty) {
            super.updateItem(pair, empty);
            if (!empty) {
                setDisable(false);
                title.setText(pair.getValue().getKey() + " — " + pair.getValue().getValue());
                setGraphic(child);
            } else {
                title.setText(null);
                setGraphic(null);
                setDisable(true);
            }
        }
    }

    class AreaCell extends VBox {
        // 与改版前一致：使用 star01.png，按 30px 加载
        private static final Image STAR_IMAGE = new Image(
                FXResourcesLoader.load("image/kujiequ/star01.png"), 30, 30, true, true, true);

        public AreaCell(TowerArea towerArea) {
            setPrefWidth(400.0);
            setMinWidth(360.0);
            setSpacing(6);
            getStyleClass().add("area");

            // --- 标题行：区域名 + 星数 ---
            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            header.getStyleClass().add("area-header");

            Label areaTitle = new Label(towerArea.getAreaName());
            areaTitle.getStyleClass().add("area-title");

            Label areaScore = new Label(String.format("%d / %d", towerArea.getStar(), towerArea.getMaxStar()));
            areaScore.getStyleClass().add("area-score");
            FontIcon starIcon = new FontIcon(Material2MZ.STAR_OUTLINE);
            areaScore.setGraphic(starIcon);
            areaScore.setContentDisplay(ContentDisplay.RIGHT);
            areaScore.getStyleClass().add("full-star");

            header.getChildren().addAll(areaTitle, new Spacer(), areaScore);

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

        private HBox buildFloorRow(Floor floor) {
            HBox row = new HBox();
            row.getStyleClass().add("floor-row");
            row.setAlignment(Pos.CENTER_LEFT);

            Label floorName = new Label(String.format("第%d层", floor.getFloor()));
            floorName.getStyleClass().add("floor-title");

            HBox starBox = new HBox();
            starBox.getStyleClass().add("star-box");
            starBox.setAlignment(Pos.CENTER_LEFT);
            int earned = Math.max(0, Math.min(floor.getStar(), FLOOR_MAX_STAR));
            for (int i = 0; i < FLOOR_MAX_STAR; i++) {
                ImageView star = new ImageView(STAR_IMAGE);
                // 已获得的星保持原图亮度；仅未获得的星降低透明度占位
                if (i >= earned) {
                    star.setOpacity(0.35);
                } else {
                    star.setOpacity(1.0);
                }
                starBox.getChildren().add(star);
            }

            HBox roleRow = new HBox(8);
            roleRow.getStyleClass().add("role-row");
            roleRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(roleRow, Priority.ALWAYS);

            if (floor.getRoleList() != null && !floor.getRoleList().isEmpty()) {
                for (SimpleRole role : floor.getRoleList()) {
                    roleRow.getChildren().add(buildRoleItem(role));
                }
            } else {
                Label empty = new Label("暂无数据");
                empty.getStyleClass().add(Styles.TEXT_MUTED);
                roleRow.getChildren().add(empty);
            }

            row.getChildren().addAll(floorName, starBox, roleRow);
            return row;
        }

        private StackPane buildRoleItem(SimpleRole role) {
            StackPane roleItem = new StackPane();
            roleItem.getStyleClass().add("role-item");

            ImageView roleIv = new ImageView();
            Image image = LocalResourcesManager.header(role.getRoleId(), 50, 50);
            roleIv.setImage(image);
            Circle circle = new Circle(25, 25, 25);
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
