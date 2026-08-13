package cn.tealc.wutheringwavestool.ui.kujiequ.tower;

import atlantafx.base.controls.Popover;
import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import com.kuro.kujiequ.model.newTowerData.NewTowerBuff;
import com.kuro.kujiequ.model.newTowerData.NewTowerModeDetail;
import com.kuro.kujiequ.model.newTowerData.NewTowerRole;
import com.kuro.kujiequ.model.newTowerData.NewTowerTeam;
import com.kuro.kujiequ.model.roleData.Role;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Pair;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class NewTowerView implements FxmlView<NewTowerViewModel>, Initializable {
    private static final double TWO_COLUMN_BREAKPOINT = 720;
    private static final double CARD_GAP = 16;

    @InjectViewModel
    private NewTowerViewModel viewModel;
    @FXML
    private TilePane areaTilePane;
    @FXML
    private ListView<NewTowerModeDetail> difficuityListview;
    @FXML
    private Label seasonEndTimeLabel;
    @FXML
    private HBox endTimeBox;
    @FXML
    private Label title;
    @FXML
    private ListView<Pair<Long, Pair<String, String>>> towerHistoryListview;
    @FXML
    private Label totalScoreLabel;
    @FXML
    private Label progressLabel;
    @FXML
    private Label rankLabel;
    @FXML
    private ScrollPane contentScroll;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        title.textProperty().bind(viewModel.titleProperty());
        seasonEndTimeLabel.textProperty().bind(viewModel.endTimeProperty());
        seasonEndTimeLabel.managedProperty().bind(seasonEndTimeLabel.textProperty().isNotEmpty());
        endTimeBox.visibleProperty().bind(seasonEndTimeLabel.textProperty().isNotEmpty());
        endTimeBox.managedProperty().bind(seasonEndTimeLabel.textProperty().isNotEmpty());
        totalScoreLabel.textProperty().bind(viewModel.totalScoreProperty());
        progressLabel.textProperty().bind(viewModel.progressInfoProperty());
        rankLabel.textProperty().bind(viewModel.rankTextProperty());
        viewModel.rankTextProperty().addListener((obs, oldRank, rank) -> updateRankStyle(rank));
        updateRankStyle(viewModel.rankTextProperty().get());

        difficuityListview.setItems(viewModel.getDifficulties());
        difficuityListview.setCellFactory(c -> new DifficultyCell());
        viewModel.getDifficulties().addListener((ListChangeListener<NewTowerModeDetail>) change -> {
            if (!change.getList().isEmpty()) {
                difficuityListview.getSelectionModel().selectFirst();
            }
        });
        difficuityListview.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, difficulty) -> {
            if (difficulty != null) {
                viewModel.changeDifficulty(difficulty);
                towerHistoryListview.getSelectionModel().clearSelection();
            }
        });

        viewModel.getTeams().addListener((ListChangeListener<? super NewTowerTeam>) change -> refreshTeams());
        refreshTeams();

        towerHistoryListview.setItems(viewModel.getHistoryList());
        towerHistoryListview.setCellFactory(list -> new HistoryCell());
        towerHistoryListview.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, history) -> {
            if (history != null) {
                viewModel.changHistory(history.getKey());
                difficuityListview.getSelectionModel().clearSelection();
            }
        });
        Label historyPlaceholder = new Label("暂无往期记录");
        historyPlaceholder.getStyleClass().add(Styles.TEXT_MUTED);
        towerHistoryListview.setPlaceholder(historyPlaceholder);

        contentScroll.viewportBoundsProperty().addListener((obs, oldBounds, bounds) -> resizeTeamGrid(bounds.getWidth()));
    }

    private void updateRankStyle(String rank) {
        rankLabel.getStyleClass().removeAll("s", "a", "b", "c");
        if (rank != null && !rank.isBlank()) {
            rankLabel.getStyleClass().add(rank.toLowerCase());
        }
    }

    private void refreshTeams() {
        areaTilePane.getChildren().clear();
        for (int i = 0; i < viewModel.getTeams().size(); i++) {
            areaTilePane.getChildren().add(new AreaCell(viewModel.getTeams().get(i), i + 1));
        }
        resizeTeamGrid(contentScroll.getViewportBounds().getWidth());
    }

    private void resizeTeamGrid(double viewportWidth) {
        if (viewportWidth <= 0) {
            return;
        }
        double contentWidth = Math.max(320, viewportWidth - 2);
        boolean useTwoColumns = contentWidth >= TWO_COLUMN_BREAKPOINT;
        areaTilePane.setPrefColumns(useTwoColumns ? 2 : 1);
        areaTilePane.setPrefTileWidth(useTwoColumns ? (contentWidth - CARD_GAP) / 2 : contentWidth);
        areaTilePane.setPrefWidth(contentWidth);
    }

    static class DifficultyCell extends ListCell<NewTowerModeDetail> {
        private final HBox content = new HBox(8);
        private final Label name = new Label();

        DifficultyCell() {
            content.setAlignment(Pos.CENTER_LEFT);
            name.getStyleClass().add("tower-name");
            HBox.setHgrow(name, Priority.ALWAYS);

            FontIcon areaIcon = new FontIcon(Material2AL.ADJUST);
            areaIcon.getStyleClass().add("tower-area-icon");

            content.getChildren().addAll(areaIcon, name, new Spacer());
            content.getStyleClass().add("tower-cell");
        }

        @Override
        protected void updateItem(NewTowerModeDetail difficulty, boolean empty) {
            super.updateItem(difficulty, empty);
            if (empty || difficulty == null) {
                setDisable(true);
                setVisible(false);
                setGraphic(null);
                return;
            }
            setDisable(false);
            setVisible(true);
            name.setText(difficulty.getModeId() == 0 ? "稳态协议" : "奇点扩张");
            setGraphic(content);
        }
    }

    static class HistoryCell extends ListCell<Pair<Long, Pair<String, String>>> {
        private final HBox child = new HBox(8);
        private final VBox dateBox = new VBox(1);
        private final Label startDate = new Label();
        private final Label endDate = new Label();
        private final Label marker = new Label();

        HistoryCell() {
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
        protected void updateItem(Pair<Long, Pair<String, String>> history, boolean empty) {
            super.updateItem(history, empty);
            if (empty || history == null) {
                setDisable(true);
                setGraphic(null);
                return;
            }
            setDisable(false);
            startDate.setText(history.getValue().getKey());
            endDate.setText(history.getValue().getValue());
            setGraphic(child);
        }
    }

    class AreaCell extends VBox {
        private final NewTowerTeam towerTeam;

        AreaCell(NewTowerTeam towerTeam, int index) {
            this.towerTeam = towerTeam;
            setMinWidth(0);
            setMaxWidth(Double.MAX_VALUE);
            getStyleClass().addAll("matrix-team-card", "new-tower-card", teamStyle(index));

            getChildren().addAll(createHeader(index), createDivider(), createTeamDetails());
        }

        private HBox createHeader(int index) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("team-card-header");

            Label number = new Label(String.format("%02d", index));
            number.getStyleClass().add("team-index");

            VBox titleBox = new VBox(0);
            Label eyebrow = new Label("TEAM");
            eyebrow.getStyleClass().add("team-eyebrow");
            Label teamName = new Label(String.format("第 %d 队", index));
            teamName.getStyleClass().add("team-title");
            titleBox.getChildren().addAll(eyebrow, teamName);

            HBox scoreBox = new HBox(7);
            scoreBox.setAlignment(Pos.CENTER_RIGHT);
            scoreBox.getStyleClass().add("team-score-box");
            Label scoreCaption = new Label("积分");
            scoreCaption.getStyleClass().add("team-score-caption");
            Label score = new Label(String.valueOf(towerTeam.getScore()));
            score.getStyleClass().add("team-score");
            scoreBox.getChildren().addAll(scoreCaption, score);

            row.getChildren().addAll(number, titleBox, new Spacer(), scoreBox);
            return row;
        }

        private Region createDivider() {
            Region divider = new Region();
            divider.getStyleClass().add("team-divider");
            return divider;
        }

        private HBox createTeamDetails() {
            HBox row = new HBox(14);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("team-card-body");

            VBox stageBox = new VBox(2);
            stageBox.setMinWidth(58);
            stageBox.getStyleClass().add("team-stage");
            Label round = new Label(String.format("第%d轮", towerTeam.getRound()));
            round.getStyleClass().add("round-info");
            Label pass = new Label(String.format("%d/%d", towerTeam.getPassBoss(), towerTeam.getBossCount()));
            pass.getStyleClass().add("pass-info");
            stageBox.getChildren().addAll(round, pass);

            HBox roles = createRoleRow();
            HBox.setHgrow(roles, Priority.ALWAYS);

            row.getChildren().addAll(stageBox, roles, createBuffView());
            return row;
        }

        private HBox createRoleRow() {
            HBox roleRow = new HBox(8);
            roleRow.setAlignment(Pos.CENTER_LEFT);
            roleRow.getStyleClass().add("role-row");

            List<NewTowerRole> roleList = towerTeam.getRoleList();
            if (roleList == null || roleList.isEmpty()) {
                Label empty = new Label("暂无阵容记录");
                empty.getStyleClass().add(Styles.TEXT_MUTED);
                roleRow.getChildren().add(empty);
                return roleRow;
            }

            for (NewTowerRole role : roleList) {
                roleRow.getChildren().add(createRoleAvatar(role));
            }
            return roleRow;
        }

        private StackPane createRoleAvatar(NewTowerRole role) {
            StackPane frame = new StackPane();
            frame.setMinSize(48, 48);
            frame.setPrefSize(48, 48);
            frame.setMaxSize(48, 48);
            frame.getStyleClass().add("role-avatar-frame");

            ImageView avatar = new ImageView();
            Image image = LocalResourcesManager.header(role.getRoleId(), 44, 44);
            avatar.setImage(image);
            avatar.setFitWidth(44);
            avatar.setFitHeight(44);
            avatar.setPreserveRatio(false);
            avatar.setClip(new Circle(22, 22, 22));
            frame.getChildren().add(avatar);

            Role roleDetail = viewModel.getRoleMap().get(role.getRoleId());
            if (roleDetail != null) {
                Label chain = new Label(String.valueOf(roleDetail.getChainUnlockNum()));
                chain.getStyleClass().add("chain-unlock-num");
                StackPane.setAlignment(chain, Pos.TOP_RIGHT);
                frame.getChildren().add(chain);
            }
            return frame;
        }

        private StackPane createBuffView() {
            StackPane frame = new StackPane();
            frame.setMinSize(48, 48);
            frame.setPrefSize(48, 48);
            frame.setMaxSize(48, 48);
            frame.getStyleClass().add("buff-frame");

            List<NewTowerBuff> buffs = towerTeam.getBuffs();
            if (buffs == null || buffs.isEmpty()) {
                FontIcon emptyIcon = new FontIcon(Material2MZ.STAR_OUTLINE);
                frame.getChildren().add(emptyIcon);
                frame.getStyleClass().add("empty");
                return frame;
            }

            NewTowerBuff buff = buffs.getFirst();
            ImageView buffIcon = new ImageView(LocalResourcesManager.imageBuffer(buff.getBuffIcon(), 34, 34, true, true));
            buffIcon.setFitWidth(34);
            buffIcon.setFitHeight(34);
            buffIcon.setPreserveRatio(true);
            frame.getChildren().add(buffIcon);

            Label buffDescription = new Label(buff.getDesc());
            buffDescription.setWrapText(true);
            buffDescription.setPrefWidth(260);
            Popover popover = new Popover(buffDescription);
            popover.setTitle(buff.getBuffName());
            popover.setDetachable(false);
            popover.setArrowLocation(Popover.ArrowLocation.TOP_RIGHT);
            frame.setOnMouseClicked(event -> popover.show(frame));
            return frame;
        }

        private String teamStyle(int index) {
            return switch ((index - 1) % 4) {
                case 1 -> "team-violet";
                case 2 -> "team-green";
                case 3 -> "team-orange";
                default -> "team-blue";
            };
        }
    }
}
