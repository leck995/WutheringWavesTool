package cn.tealc.wutheringwavestool.ui;

import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.Difficulty;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.Floor;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.Role;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.TowerArea;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.awt.geom.Area;
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        difficuityListview.setItems(viewModel.getDifficultyList());
        difficuityListview.setCellFactory(difficultyListView -> new DifficultyCell());



        difficuityListview.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                viewModel.changeDifficulty(newValue);
            }

        });
        viewModel.getTowerAreaList().addListener((ListChangeListener<? super TowerArea>) change -> {
            areaFlowPane.getChildren().clear();
            for (TowerArea towerArea : change.getList()) {
                areaFlowPane.getChildren().add(new AreaCell(towerArea));
            }
        });

    }



    class DifficultyCell extends ListCell<Difficulty> {
        public DifficultyCell() {
        }

        @Override
        protected void updateItem(Difficulty difficulty, boolean b) {
            super.updateItem(difficulty, b);
            if (!b) {
                int sum = difficulty.getTowerAreaList().stream().mapToInt(TowerArea::getStar).sum();
                setText(String.format("%s (%d星)",difficulty.getDifficultyName(),sum));
            }
        }
    }


    class AreaCell extends VBox {
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



            title.setText(towerArea.getAreaName());

            for (Floor floor : towerArea.getFloorList()) {
                HBox floorHBox = new HBox();
                Label floorName = new Label();
                HBox starHBox = new HBox();
                HBox roleHbox = new HBox();

                //floorName.
                floorName.getStyleClass().add(Styles.TITLE_4);
                starHBox.setSpacing(10.0);
                starHBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(starHBox, Priority.ALWAYS);

                roleHbox.setSpacing(10.0);
                roleHbox.setAlignment(Pos.CENTER_LEFT);

                floorHBox.getChildren().addAll(floorName, starHBox, roleHbox);
                floorHBox.setSpacing(20.0);
                floorHBox.setAlignment(Pos.CENTER_LEFT);
                floorName.setText(String.format("第 %d 层",floor.getFloor()));

                if (floor.getRoleList() != null) {
                    for (Role role : floor.getRoleList()) {
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