package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.Difficulty;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.Floor;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.SimpleRole;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.TowerArea;
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
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-15 23:40
 */
public class TowerView implements FxmlView<TowerViewModel>, Initializable {
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
    private ListView<Pair<Long, Pair<String,String>>> towerHistoryListview;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        title.textProperty().bind(viewModel.titleProperty());
        difficuityListview.setItems(viewModel.getDifficultyList());
        difficuityListview.setCellFactory(difficultyListView -> new DifficultyCell());



        difficuityListview.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                viewModel.changeDifficulty(newValue);
                boolean show = newValue.getDifficulty() == 3;
                seasonEndTimeLabel.setVisible(show);
                towerHistoryListview.getParent().setVisible(show);
            }
        });
        viewModel.getTowerAreaList().addListener((ListChangeListener<? super TowerArea>) change -> {
            areaFlowPane.getChildren().clear();
            for (TowerArea towerArea : change.getList()) {
                areaFlowPane.getChildren().add(new AreaCell(towerArea));
            }
        });

        seasonEndTimeLabel.textProperty().bind(viewModel.seasonEndTimeProperty());

        towerHistoryListview.setItems(viewModel.getTowerHistoryList());
        towerHistoryListview.setCellFactory(difficultyListView -> new HistoryCell());
        towerHistoryListview.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                viewModel.changeHistory(newValue.getKey());
            }
        });
    }



    static class DifficultyCell extends ListCell<Difficulty> {
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
        protected void updateItem(Difficulty difficulty, boolean b) {
            super.updateItem(difficulty, b);
            if (!b) {
                setDisable(false);
                setVisible(true);
                int sum = difficulty.getTowerAreaList().stream().mapToInt(TowerArea::getStar).sum();
                int max = difficulty.getTowerAreaList().stream().mapToInt(TowerArea::getMaxStar).sum();
                title.setText(difficulty.getDifficultyName());
                star.setText(String.format("%2d",sum));
                if (sum == max){
                    star.getStyleClass().add("full-star");
                }else {
                    star.getStyleClass().remove("full-star");
                }
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

    static class HistoryCell extends ListCell<Pair<Long,Pair<String,String>>> {
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
        private static final Image STAR_IMAGE = new Image(FXResourcesLoader.load("image/star01.png"),30,30,true,true,true);
        private final Label title;
        private TowerArea towerArea;


        public AreaCell(TowerArea towerArea) {
            this.towerArea = towerArea;

            setPrefWidth(400.0);
            setPrefHeight(160.0);
            VBox titleVBox = new VBox();
            title = new Label();
            Separator separator = new Separator(Orientation.HORIZONTAL);
            titleVBox.getChildren().addAll(title, separator);
            titleVBox.setSpacing(5.0);
            titleVBox.setAlignment(Pos.CENTER_LEFT);




            getChildren().add(titleVBox);
            getStyleClass().add("area");


            title.setText(towerArea.getAreaName());
            title.getStyleClass().add("area-title");
            for (Floor floor : towerArea.getFloorList()) {
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

                floorHBox.getChildren().addAll(floorName, starHBox, roleHbox);
                floorHBox.setSpacing(20.0);
                //floorHBox.setMinHeight(40.0);
                floorHBox.setAlignment(Pos.CENTER_LEFT);
                floorName.setText(String.format("第%d层",floor.getFloor()));

                for (int i = 0; i < floor.getStar(); i++) {
                    ImageView star = new ImageView(STAR_IMAGE);
                    starHBox.getChildren().add(star);

                }

                if (floor.getRoleList() != null && !floor.getRoleList().isEmpty()) {
                    for (SimpleRole role : floor.getRoleList()) {
                        ImageView roleIv = new ImageView();
                        Image image = LocalResourcesManager.imageBuffer(role.getIconUrl(),50,50,true,true);
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

        }



    }



}