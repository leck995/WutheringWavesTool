package cn.tealc.wutheringwavestool.ui.kujiequ.tower;

import atlantafx.base.controls.Popover;
import atlantafx.base.controls.Spacer;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
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
import org.kordamp.ikonli.material2.Material2MZ;


public class SlashView implements FxmlView<SlashViewModel> {

    @InjectViewModel
    private SlashViewModel viewModel;

    @FXML
    private FlowPane areaFlowPane;

    @FXML
    private ListView<SlashDifficulty> difficuityListview;

    @FXML
    private Label seasonEndTimeLabel;

    @FXML
    private Label title;

    @FXML
    private ListView<Challenge> towerHistoryListview;

    public void initialize() {
        title.textProperty().bind(viewModel.titleProperty());
        seasonEndTimeLabel.textProperty().bind(viewModel.endTimeProperty());
        seasonEndTimeLabel.visibleProperty().bind(viewModel.endTimeVisibleProperty());


        difficuityListview.setItems(viewModel.getDifficulties());
        difficuityListview.setCellFactory(c -> new DifficultyCell());

        difficuityListview.getSelectionModel().selectedIndexProperty().addListener((observableValue, number, t1) -> viewModel.changeDifficulty(t1.intValue()));

        viewModel.getChallenges().addListener((ListChangeListener<? super Challenge>) change -> {
            areaFlowPane.getChildren().clear();
            for (Challenge challenge : change.getList()) {
                areaFlowPane.getChildren().add(new AreaCell(challenge));
            }
        });



    }

    static class DifficultyCell extends ListCell<SlashDifficulty> {
        private final StackPane child;
        private final Label title=new Label();
        private final Label star=new Label();
        public DifficultyCell() {
            title.getStyleClass().add("tower-name");
            star.getStyleClass().add("tower-star");

            FontIcon fontIcon = new FontIcon(Material2MZ.STAR_OUTLINE);
            star.setGraphic(fontIcon);
            star.setContentDisplay(ContentDisplay.RIGHT);
            child= new StackPane(title,star);

            StackPane.setAlignment(title, Pos.CENTER_LEFT);
            StackPane.setAlignment(star, Pos.CENTER_RIGHT);
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
/*                int sum = difficulty.getTowerAreaList().stream().mapToInt(TowerArea::getStar).sum();
                int max = difficulty.getTowerAreaList().stream().mapToInt(TowerArea::getMaxStar).sum();
                title.setText(difficulty.getDifficultyName());
                star.setText(String.format("%2d",sum));
                if (sum == max){
                    star.getStyleClass().add("full-star");
                }else {
                    star.getStyleClass().remove("full-star");
                }*/
                setGraphic(child);
            }else {
                title.setText(null);
                star.setText(null);
                setDisable(true);
                setVisible(false);
                setGraphic(null);
            }
        }
    }

    static class HistoryCell extends ListCell<Pair<Long, Pair<String,String>>> {
        private final HBox child = new HBox();
        private final Label title=new Label();
        public HistoryCell() {
            child.getChildren().add(title);
            title.getStyleClass().add("tower-name");
            child.getStyleClass().add("tower-cell");
        }

        @Override
        protected void updateItem(Pair<Long, Pair<String, String>> pair, boolean b) {
            super.updateItem(pair, b);
            if (!b){
                setDisable(false);
                title.setText(pair.getValue().getKey() +"--"+pair.getValue().getValue());
                setGraphic(child);
            }else {
                title.setText(null);
                setGraphic(null);
                setDisable(true);
            }
        }
    }

    static class AreaCell extends VBox {
        private static final Image STAR_IMAGE = new Image(FXResourcesLoader.load("image/kujiequ/star01.png"),30,30,true,true,true);
        private final Label title;
        private final Label level;
        private final Label score;
        private Challenge challenge;


        public AreaCell(Challenge challenge) {
            this.challenge = challenge;
            setPrefWidth(400.0);
            setPrefHeight(160.0);
            VBox headBar = new VBox();
            HBox titleVBox = new HBox();

            title = new Label();
            level = new Label();
            score = new Label();

            titleVBox.getChildren().addAll(title,new Spacer(),level,score);
            titleVBox.setSpacing(10);

            Separator separator = new Separator(Orientation.HORIZONTAL);
            headBar.getChildren().addAll(titleVBox, separator);
            headBar.setSpacing(5.0);
            headBar.setAlignment(Pos.CENTER_LEFT);

            getChildren().add(headBar);
            getStyleClass().add("area");
            title.setText(String.format("%d %s",challenge.getChallengeId(),challenge.getChallengeName()));
            title.getStyleClass().add("area-title");


            level.setText(String.valueOf(challenge.getRank()));
            score.setText(String.format("积分：%s",challenge.getScore()));
            //ImageView icon = new ImageView(challenge.get);

            for (int i = 0; i < challenge.getHalfList().size(); i++) {
                Half half = challenge.getHalfList().get(i);
                HBox floorHBox = new HBox();
                Label floorName = new Label();
                HBox starHBox = new HBox();
                HBox roleHbox = new HBox();

                //floorName.
                floorName.getStyleClass().add("floor-title");
                starHBox.setSpacing(5.0);
                starHBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(starHBox, Priority.ALWAYS);

                roleHbox.setSpacing(10.0);
                roleHbox.setAlignment(Pos.CENTER_LEFT);

                Label scoreLabel = new Label();

                floorHBox.getChildren().addAll(floorName, scoreLabel, new Spacer(),roleHbox,starHBox);
                floorHBox.setSpacing(20.0);
                //floorHBox.setMinHeight(40.0);
                floorHBox.setAlignment(Pos.CENTER_LEFT);
                floorName.setText(String.format("第%d队",i+1));
                scoreLabel.setText(String.valueOf(half.getScore()));

                ImageView buffIcon = new ImageView(LocalResourcesManager.imageBuffer(half.getBuffIcon(),50,50,true,true));

                starHBox.getChildren().add(buffIcon);

                Label buffDesc = new Label(String.format("%s：%s",half.getBuffName(),half.getBuffDescription()));
                buffDesc.setWrapText(true);
                buffDesc.setPrefWidth(200);
                Popover popover = new Popover(buffDesc);
                popover.setTitle(half.getBuffName());
                popover.setDetachable(false);
                popover.setArrowLocation(Popover.ArrowLocation.TOP_LEFT);
                buffIcon.setOnMouseClicked(event -> {
                    popover.show(buffIcon);
                });




                if (half.getRoleList() != null && !half.getRoleList().isEmpty()) {
                    for (SimpleRole role : half.getRoleList()) {
                        ImageView roleIv = new ImageView();
                        Image image = LocalResourcesManager.header(role.getRoleId(),50,50);
                        roleIv.setImage(image);
                        Circle circle = new Circle(25,25,25);
                        roleIv.setClip(circle);
                        roleHbox.getChildren().add(roleIv);
                    }
                }else {
                    Label label = new Label("暂无数据");
                    roleHbox.getChildren().add(label);
                }

                getChildren().add(floorHBox);
            }


            for (Half half : challenge.getHalfList()) {

            }

        }



    }


}