package cn.tealc.wutheringwavestool.ui.kujiequ.tower;

import atlantafx.base.controls.Popover;
import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import com.kuro.kujiequ.model.roleData.Role;
import com.kuro.kujiequ.model.slash.Challenge;
import com.kuro.kujiequ.model.slash.Half;
import com.kuro.kujiequ.model.slash.SlashDifficulty;
import com.kuro.kujiequ.model.towerData.SimpleRole;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Pair;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;


public class SlashView implements FxmlView<SlashViewModel> {

    @InjectViewModel
    private SlashViewModel viewModel;

    @FXML
    private GridPane areaGridPane;

    @FXML
    private ListView<SlashDifficulty> difficuityListview;

    @FXML
    private Label seasonEndTimeLabel;

    @FXML
    private Label title;
    @FXML
    private Label scoreLabel01, scoreLabel02;
    @FXML
    private HBox infoPane;

    @FXML
    private HBox endTimeBox;

    @FXML
    private ListView<Pair<Long, Pair<String, String>>> towerHistoryListview;

    public void initialize() {
        title.textProperty().bind(viewModel.titleProperty());
        infoPane.visibleProperty().bind(viewModel.endTimeVisibleProperty());
        endTimeBox.visibleProperty().bind(viewModel.endTimeVisibleProperty());
        endTimeBox.managedProperty().bind(viewModel.endTimeVisibleProperty());
        seasonEndTimeLabel.textProperty().bind(viewModel.endTimeProperty());
        scoreLabel01.textProperty().bind(viewModel.score01Property());
        scoreLabel02.textProperty().bind(viewModel.score02Property());


        difficuityListview.setItems(viewModel.getDifficulties());
        difficuityListview.setCellFactory(c -> new DifficultyCell());
        viewModel.getDifficulties().addListener((ListChangeListener<SlashDifficulty>) change -> {
            if (!change.getList().isEmpty())
                difficuityListview.getSelectionModel().selectFirst();
        });
        difficuityListview.getSelectionModel().selectedItemProperty().addListener((observableValue, number, t1) ->
        {
            if (t1 != null) {
                viewModel.changeDifficulty(t1);
                towerHistoryListview.getSelectionModel().clearSelection();
            }
        });

        viewModel.getChallenges().addListener((ListChangeListener<? super Challenge>) change -> {
            areaGridPane.getChildren().clear();
            java.util.List<? extends Challenge> challenges = change.getList();
            for (int i = 0; i < challenges.size(); i++) {
                AreaCell areaCell = new AreaCell(challenges.get(i));
                GridPane.setHgrow(areaCell, Priority.ALWAYS);
                GridPane.setVgrow(areaCell, Priority.NEVER);
                areaGridPane.add(areaCell, i % 2, i / 2);
            }
        });

        towerHistoryListview.setItems(viewModel.getHistoryList());
        towerHistoryListview.setCellFactory(difficultyListView -> new HistoryCell());
        towerHistoryListview.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                viewModel.changHistory(newValue.getKey());
                difficuityListview.getSelectionModel().clearSelection();
            }
        });


        towerHistoryListview.setPlaceholder(new Label("尚无往期记录"));
    }

    static class DifficultyCell extends ListCell<SlashDifficulty> {
        private final HBox child;
        private final Label title = new Label();

        public DifficultyCell() {
            title.getStyleClass().add("tower-name");

            FontIcon areaIcon = new FontIcon(Material2AL.ADJUST);
            areaIcon.getStyleClass().add("tower-area-icon");

            child = new HBox(8, areaIcon, title, new Spacer());
            child.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(title, Priority.ALWAYS);
            setGraphic(child);
            child.getStyleClass().add("tower-cell");
        }

        @Override
        protected void updateItem(SlashDifficulty difficulty, boolean b) {
            super.updateItem(difficulty, b);
            if (!b) {
                setDisable(false);
                setVisible(true);
                title.setText(difficulty.getDifficultyName());
                setGraphic(child);
            } else {
                title.setText(null);
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
        protected void updateItem(Pair<Long, Pair<String, String>> pair, boolean b) {
            super.updateItem(pair, b);
            if (!b) {
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
        private static final Image SLASH_IMAGE01 = new Image(FXResourcesLoader.load("image/kujiequ/slash01.png"), 30, 30, true, true, true);
        private static final Image SLASH_IMAGE02 = new Image(FXResourcesLoader.load("image/kujiequ/slash02.png"), 30, 30, true, true, true);
        private static final Image SLASH_IMAGE03 = new Image(FXResourcesLoader.load("image/kujiequ/slash03.png"), 30, 30, true, true, true);

        private final Challenge challenge;


        public AreaCell(Challenge challenge) {
            this.challenge = challenge;
            setMinWidth(0);
            setMaxWidth(Double.MAX_VALUE);
            getStyleClass().addAll("matrix-team-card", "slash-card", teamStyle(challenge.getChallengeId()));

            getChildren().addAll(createHeader(), createDivider());

            if (challenge.getHalfList().isEmpty()) {
                Label empty = new Label("无记录");
                empty.getStyleClass().add(Styles.TEXT_MUTED);
                getChildren().add(empty);
                return;
            }

            for (int i = 0; i < challenge.getHalfList().size(); i++) {
                getChildren().add(createHalfRow(challenge.getHalfList().get(i), i + 1));
            }
        }

        private HBox createHeader() {
            HBox header = new HBox(12);
            header.setAlignment(Pos.CENTER_LEFT);
            header.getStyleClass().add("team-card-header");

            ImageView icon = new ImageView();
            if (challenge.getChallengeId() < 7) {
                icon.setImage(SLASH_IMAGE01);
            } else if (challenge.getChallengeId() < 12) {
                icon.setImage(SLASH_IMAGE02);
            } else {
                icon.setImage(SLASH_IMAGE03);
            }

            VBox titleBox = new VBox(0);
            Label eyebrow = new Label(String.format("FLOOR %d", challenge.getChallengeId()));
            eyebrow.getStyleClass().add("team-eyebrow");
            Label title = new Label(challenge.getChallengeName());
            title.getStyleClass().add("team-title");
            titleBox.getChildren().addAll(eyebrow, title);

            HBox scoreBox = new HBox(7);
            scoreBox.setAlignment(Pos.CENTER_RIGHT);

            if (challenge.getRank() != null) {
                Label rank = new Label(challenge.getRank());
                rank.getStyleClass().addAll("summary-rank", challenge.getRank().toLowerCase());
                scoreBox.getChildren().add(rank);
            }

            Label scoreCaption = new Label("积分");
            scoreCaption.getStyleClass().add("team-score-caption");
            Label score = new Label(String.valueOf(challenge.getScore()));
            score.getStyleClass().add("team-score");
            scoreBox.getChildren().addAll(scoreCaption, score);

            header.getChildren().addAll(icon, titleBox, new Spacer(), scoreBox);
            return header;
        }

        private Region createDivider() {
            Region divider = new Region();
            divider.getStyleClass().add("team-divider");
            return divider;
        }

        private HBox createHalfRow(Half half, int index) {
            HBox row = new HBox(14);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("team-card-body");

            // 左：NewTowerView 风格 team-stage
            VBox stageBox = new VBox(2);
            stageBox.setMinWidth(58);
            stageBox.getStyleClass().add("team-stage");
            Label round = new Label(String.format("%d队", index));
            round.getStyleClass().add("round-info");
            Label pass = new Label(String.valueOf(half.getScore()));
            pass.getStyleClass().add("pass-info");
            stageBox.getChildren().addAll(round, pass);

            // 中：角色头像
            HBox roleRow = createRoleRow(half);
            HBox.setHgrow(roleRow, Priority.ALWAYS);

            // 右：buff
            StackPane buffFrame = createBuffFrame(half);

            row.getChildren().addAll(stageBox, roleRow, buffFrame);
            return row;
        }

        private HBox createRoleRow(Half half) {
            HBox roleRow = new HBox(8);
            roleRow.setAlignment(Pos.CENTER_LEFT);
            roleRow.getStyleClass().add("role-row");

            if (half.getRoleList() == null || half.getRoleList().isEmpty()) {
                Label empty = new Label("暂无阵容记录");
                empty.getStyleClass().add(Styles.TEXT_MUTED);
                roleRow.getChildren().add(empty);
                return roleRow;
            }

            for (SimpleRole role : half.getRoleList()) {
                roleRow.getChildren().add(createRoleAvatar(role));
            }
            return roleRow;
        }

        private StackPane createRoleAvatar(SimpleRole role) {
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

        private StackPane createBuffFrame(Half half) {
            StackPane frame = new StackPane();
            frame.setMinSize(48, 48);
            frame.setPrefSize(48, 48);
            frame.setMaxSize(48, 48);
            frame.getStyleClass().add("buff-frame");

            ImageView buffIcon = new ImageView(LocalResourcesManager.imageBuffer(half.getBuffIcon(), 34, 34, true, true));
            buffIcon.setFitWidth(34);
            buffIcon.setFitHeight(34);
            buffIcon.setPreserveRatio(true);
            frame.getChildren().add(buffIcon);

            Label buffDesc = new Label(String.format("%s：%s", half.getBuffName(), half.getBuffDescription()));
            buffDesc.setWrapText(true);
            buffDesc.setPrefWidth(260);
            Popover popover = new Popover(buffDesc);
            popover.setTitle(half.getBuffName());
            popover.setDetachable(false);
            popover.setArrowLocation(Popover.ArrowLocation.TOP_RIGHT);
            frame.setOnMouseClicked(event -> popover.show(frame));
            return frame;
        }

        private String teamStyle(int id) {
            return switch ((id - 1) % 4) {
                case 1 -> "team-violet";
                case 2 -> "team-green";
                case 3 -> "team-orange";
                default -> "team-blue";
            };
        }
    }


}