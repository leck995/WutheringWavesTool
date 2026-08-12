package cn.tealc.wutheringwavestool.ui.kujiequ.tower;

import atlantafx.base.controls.Popover;
import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import com.kuro.kujiequ.model.newTowerData.NewTowerBuff;
import com.kuro.kujiequ.model.newTowerData.NewTowerModeDetail;
import com.kuro.kujiequ.model.newTowerData.NewTowerRole;
import com.kuro.kujiequ.model.newTowerData.NewTowerTeam;
import com.kuro.kujiequ.model.roleData.Role;
import com.kuro.kujiequ.model.slash.Challenge;
import com.kuro.kujiequ.model.slash.Half;
import com.kuro.kujiequ.model.towerData.SimpleRole;
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

public class NewTowerView implements FxmlView<NewTowerViewModel>, Initializable {
    @InjectViewModel
    private NewTowerViewModel viewModel;
    @FXML
    private FlowPane areaFlowPane;
    @FXML
    private ListView<NewTowerModeDetail> difficuityListview;
    @FXML
    private Label seasonEndTimeLabel;
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
    private StackPane lockPane;
    @FXML
    private VBox contentPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //lockPane.visibleProperty().bind(viewModel.isUnLockProperty().not());
        lockPane.setVisible(false);
        //contentPane.visibleProperty().bind(viewModel.isUnLockProperty());
        title.textProperty().bind(viewModel.titleProperty());
        seasonEndTimeLabel.textProperty().bind(viewModel.endTimeProperty());
        totalScoreLabel.textProperty().bind(viewModel.totalScoreProperty());
        progressLabel.textProperty().bind(viewModel.progressInfoProperty());
        rankLabel.textProperty().bind(viewModel.rankTextProperty());
        viewModel.rankTextProperty().addListener((obs, old, rank) -> {
            rankLabel.getStyleClass().removeAll("s", "a", "b", "c");
            if (rank != null && !rank.isEmpty()) {
                rankLabel.getStyleClass().add(rank.toLowerCase());
            }
        });


        difficuityListview.setItems(viewModel.getDifficulties());
        difficuityListview.setCellFactory(c -> new DifficultyCell());
        viewModel.getDifficulties().addListener((ListChangeListener<NewTowerModeDetail>) change -> {
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

        viewModel.getTeams().addListener((ListChangeListener<? super NewTowerTeam>) change -> {
            areaFlowPane.getChildren().clear();
            for (int i = 0; i < change.getList().size(); i++) {
                areaFlowPane.getChildren().add(new AreaCell(change.getList().get(i),i+1));
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

    static class DifficultyCell extends ListCell<NewTowerModeDetail> {
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
        protected void updateItem(NewTowerModeDetail difficulty, boolean b) {
            super.updateItem(difficulty, b);
            if (!b) {
                setDisable(false);
                setVisible(true);
                title.setText(difficulty.getModeId() == 0 ? "稳态协议" : "奇点扩张");
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
            child.getStyleClass().add("tower-cell");
        }

        @Override
        protected void updateItem(Pair<Long, Pair<String, String>> pair, boolean b) {
            super.updateItem(pair, b);
            if (!b) {
                setDisable(false);
                title.setText(pair.getValue().getKey() + " " + pair.getValue().getValue());
                setGraphic(child);
            } else {
                title.setText(null);
                setGraphic(null);
                setDisable(true);
            }
        }
    }


    class AreaCell extends VBox {
        private final Label title;
        private final Label score;
        private NewTowerTeam towerTeam;

        public AreaCell(NewTowerTeam towerTeam, int index) {
            this.towerTeam = towerTeam;
            setPrefWidth(350.0);
            setPrefHeight(140.0);
            setSpacing(6);
            getStyleClass().add("area");

            // --- 标题行 ---
            HBox titleRow = new HBox(10);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            title = new Label(String.format("第 %d 队", index));
            title.getStyleClass().add("area-title");

            score = new Label(String.format("%s", this.towerTeam.getScore()));
            score.getStyleClass().add("area-score");
            Label scoreTip = new Label("积分:");
            scoreTip.getStyleClass().add("area-label");
            score.setGraphic(scoreTip);

            titleRow.getChildren().addAll(title, new Spacer(), score);

            Separator separator = new Separator(Orientation.HORIZONTAL);

            getChildren().addAll(titleRow, separator);

            // --- 无记录 ---
            if (this.towerTeam.getRoleList().isEmpty()) {
                Label l = new Label("无记录");
                l.getStyleClass().add(Styles.TEXT_MUTED);
                getChildren().add(l);
                return;
            }

            // --- 队伍详情行 ---
            HBox teamRow = new HBox(15);
            teamRow.getStyleClass().add("team-row");
            teamRow.setAlignment(Pos.CENTER_LEFT);

            // 左侧：轮次 + 过关信息
            VBox descBox = new VBox(8);
            descBox.getStyleClass().add("team-desc");
            descBox.setAlignment(Pos.CENTER_LEFT);

            String passBoss = String.format("%d/%d", towerTeam.getPassBoss(), towerTeam.getBossCount());
            Label passBossLabel = new Label(passBoss);
            passBossLabel.getStyleClass().add("pass-info");

            String roundBoss = String.format("第%d轮", towerTeam.getRound());
            Label roundLabel = new Label(roundBoss);
            roundLabel.getStyleClass().add("round-info");

            descBox.getChildren().addAll(roundLabel, passBossLabel);

            // 中间：角色列表
            HBox roleRow = new HBox(8);
            roleRow.getStyleClass().add("role-row");
            roleRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(roleRow, Priority.ALWAYS);

            for (NewTowerRole role : this.towerTeam.getRoleList()) {
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

                roleRow.getChildren().add(roleItem);
            }

            // 右侧：BUFF
            HBox buffBox = new HBox();
            buffBox.getStyleClass().add("buff-section");

            List<NewTowerBuff> buffs = towerTeam.getBuffs();
            if (buffs != null && !buffs.isEmpty()) {
                NewTowerBuff buff = buffs.getFirst();
                ImageView buffIcon = new ImageView(LocalResourcesManager.imageBuffer(buff.getBuffIcon(), 40, 40, true, true));
                buffIcon.setFitWidth(40);
                buffIcon.setFitHeight(40);
                buffBox.getChildren().add(buffIcon);

                Label buffDesc = new Label(String.format("%s：%s", buff.getBuffName(), buff.getDesc()));
                buffDesc.setWrapText(true);
                buffDesc.setPrefWidth(220);
                Popover popover = new Popover(buffDesc);
                popover.setTitle(buff.getBuffName());
                popover.setDetachable(false);
                popover.setArrowLocation(Popover.ArrowLocation.TOP_LEFT);
                buffBox.setOnMouseClicked(event -> popover.show(buffBox));
            }

            teamRow.getChildren().addAll(descBox, roleRow, buffBox);
            getChildren().add(teamRow);
        }
    }
}
