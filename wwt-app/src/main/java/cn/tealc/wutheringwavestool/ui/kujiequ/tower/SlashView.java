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
import com.kuro.kujiequ.model.towerData.Difficulty;
import com.kuro.kujiequ.model.towerData.Floor;
import com.kuro.kujiequ.model.towerData.SimpleRole;
import com.kuro.kujiequ.model.towerData.TowerArea;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
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
    private ListView<Pair<Long, Pair<String, String>>> towerHistoryListview;

    public void initialize() {
        title.textProperty().bind(viewModel.titleProperty());
        infoPane.visibleProperty().bind(viewModel.endTimeVisibleProperty());
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

        private final Label title;
        private final Label level;
        private final Label score;
        private Challenge challenge;


        public AreaCell(Challenge challenge) {
            this.challenge = challenge;
            setMinWidth(0);
            setMaxWidth(Double.MAX_VALUE);
            setMinHeight(160.0);
            VBox headBar = new VBox();
            HBox titleVBox = new HBox();

            title = new Label();
            level = new Label();
            score = new Label();
            Label scoreTip = new Label("积分:");
            scoreTip.getStyleClass().add("area-label");
            score.setGraphic(scoreTip);


            titleVBox.getChildren().addAll(title, new Spacer(), level, score);
            titleVBox.setAlignment(Pos.CENTER_LEFT);
            titleVBox.setSpacing(10);

            Separator separator = new Separator(Orientation.HORIZONTAL);
            headBar.getChildren().addAll(titleVBox, separator);
            headBar.setSpacing(5.0);
            headBar.setAlignment(Pos.CENTER_LEFT);

            getChildren().add(headBar);
            getStyleClass().add("area");
            title.setText(String.format("%d %s", challenge.getChallengeId(), challenge.getChallengeName()));
            title.getStyleClass().add("area-title");

            ImageView icon = new ImageView();
            if (challenge.getChallengeId() < 7) {
                icon.setImage(SLASH_IMAGE01);
            } else if (challenge.getChallengeId() < 12) {
                icon.setImage(SLASH_IMAGE02);
            } else {
                icon.setImage(SLASH_IMAGE03);
            }
            title.setGraphic(icon);


            if(challenge.getRank() != null){
                level.setText(challenge.getRank());
                level.getStyleClass().addAll("area-level", challenge.getRank().toLowerCase());
            }
            score.setText(String.format("%s", challenge.getScore()));
            score.getStyleClass().add("area-score");
            //ImageView icon = new ImageView(challenge.get);

            if (challenge.getHalfList().isEmpty()){
                Label l = new Label("无记录");
                l.getStyleClass().add(Styles.TEXT_MUTED);
                getChildren().add(l);
                return;
            }


            for (int i = 0; i < challenge.getHalfList().size(); i++) {
                Half half = challenge.getHalfList().get(i);
                HBox floorHBox = new HBox();
                // 左：第N队 + 分数
                HBox leftBox = new HBox();
                // 中：角色头像
                HBox roleHbox = new HBox();
                // 右：buff 图标
                HBox buffBox = new HBox();

                leftBox.setSpacing(8.0);
                leftBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(leftBox, Priority.NEVER);

                roleHbox.setSpacing(5.0);
                roleHbox.setAlignment(Pos.CENTER);
                HBox.setHgrow(roleHbox, Priority.ALWAYS);

                buffBox.setAlignment(Pos.CENTER_RIGHT);
                HBox.setHgrow(buffBox, Priority.NEVER);

                Label floorName = new Label();
                floorName.getStyleClass().add("floor-title");
                Label scoreLabel = new Label();
                scoreLabel.getStyleClass().add("floor-score");
                scoreLabel.setMinWidth(40.0);
                floorName.setText(String.format("%d队", i + 1));
                scoreLabel.setText(String.valueOf(half.getScore()));
                leftBox.getChildren().addAll(floorName, scoreLabel);

                StackPane buffFrame = new StackPane();
                buffFrame.getStyleClass().addAll("role-item", "buff-frame");

                ImageView buffIcon = new ImageView(LocalResourcesManager.imageBuffer(half.getBuffIcon(), 50, 50, true, true));
                buffIcon.setFitWidth(50);
                buffIcon.setFitHeight(50);
                Circle buffCircle = new Circle(25, 25, 25);
                buffIcon.setClip(buffCircle);
                buffFrame.getChildren().add(buffIcon);
                buffBox.getChildren().add(buffFrame);
                buffBox.getStyleClass().add("floor-buff");

                Label buffDesc = new Label(String.format("%s：%s", half.getBuffName(), half.getBuffDescription()));
                buffDesc.setWrapText(true);
                buffDesc.setPrefWidth(200);
                Popover popover = new Popover(buffDesc);
                popover.setTitle(half.getBuffName());
                popover.setDetachable(false);
                popover.setArrowLocation(Popover.ArrowLocation.TOP_LEFT);
                buffFrame.setOnMouseClicked(event -> popover.show(buffFrame));

                if (half.getRoleList() != null && !half.getRoleList().isEmpty()) {
                    for (SimpleRole role : half.getRoleList()) {
                        StackPane roleItem = new StackPane();
                        roleItem.getStyleClass().add("role-item");

                        ImageView roleIv = new ImageView();
                        Image image = LocalResourcesManager.header(role.getRoleId(), 50, 50);
                        roleIv.setImage(image);
                        roleIv.setFitWidth(50);
                        roleIv.setFitHeight(50);
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

                        roleHbox.getChildren().add(roleItem);
                    }
                } else {
                    Label label = new Label("暂无数据");
                    roleHbox.getChildren().add(label);
                }

                floorHBox.getChildren().addAll(leftBox, roleHbox, buffBox);
                floorHBox.setSpacing(10.0);
                floorHBox.setAlignment(Pos.CENTER_LEFT);

                getChildren().add(floorHBox);
            }
        }


    }


}