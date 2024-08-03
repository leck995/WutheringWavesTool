package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.model.roleData.Phantom;
import cn.tealc.wutheringwavestool.model.roleData.Role;
import cn.tealc.wutheringwavestool.ui.component.OwnRoleDetailCell;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.Initialize;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import javafx.util.Pair;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-30 17:04
 */
public class OwnRoleDetailView implements FxmlView<OwnRoleDetailViewModel>, Initializable {
    @InjectViewModel
    private OwnRoleDetailViewModel viewModel;
    @FXML
    private ImageView chain01;

    @FXML
    private ImageView chain02;

    @FXML
    private ImageView chain03;

    @FXML
    private ImageView chain04;

    @FXML
    private ImageView chain05;

    @FXML
    private ImageView chain06;

    @FXML
    private ImageView roleImageView;
    @FXML
    private ImageView roleAttrImageView;
    @FXML
    private Label roleLevel;

    @FXML
    private ListView<Pair<Role, Image>> roleListview;

    @FXML
    private Label roleName;

    @FXML
    private Label skill01;

    @FXML
    private Label skill02;

    @FXML
    private Label skill03;

    @FXML
    private Label skill04;

    @FXML
    private Label skill05;

    @FXML
    private ImageView skillImg01;

    @FXML
    private ImageView skillImg02;

    @FXML
    private ImageView skillImg03;

    @FXML
    private ImageView skillImg04;

    @FXML
    private ImageView skillImg05;

    @FXML
    private ImageView weaponImageView;

    @FXML
    private Label weaponLevel;
    @FXML
    private Label weaponName;

    @FXML
    private Label weaponResonLevel;
    @FXML
    private Pane roleBgPane;
    @FXML
    private StackPane weaponBgPane;

    @FXML
    private Label phantomCost;

    @FXML
    private ListView<Pair<Phantom, Image>> phantomListView;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        roleImageView.setSmooth(true);
        roleImageView.imageProperty().bind(viewModel.roleImageProperty());
        roleLevel.textProperty().bind(viewModel.roleLevelProperty());
        roleName.textProperty().bind(viewModel.roleNameProperty());
        roleAttrImageView.imageProperty().bind(viewModel.roleAttrImageProperty());

        weaponName.textProperty().bind(viewModel.weaponNameProperty());
        weaponLevel.textProperty().bind(viewModel.weaponLevelProperty());
        weaponResonLevel.textProperty().bind(viewModel.weaponResonLevelProperty());
        weaponImageView.imageProperty().bind(viewModel.weaponImageProperty());
        weaponBgPane.backgroundProperty().bind(viewModel.weaponBgProperty());
        weaponBgPane.setEffect(new DropShadow(BlurType.THREE_PASS_BOX, Color.web("#2b2b2b75"),1,0.5,2,2));
        skill01.textProperty().bind(viewModel.skill01Property());
        skill02.textProperty().bind(viewModel.skill02Property());
        skill03.textProperty().bind(viewModel.skill03Property());
        skill04.textProperty().bind(viewModel.skill04Property());
        skill05.textProperty().bind(viewModel.skill05Property());
        skillImg01.imageProperty().bind(viewModel.skillImg01Property());
        skillImg02.imageProperty().bind(viewModel.skillImg02Property());
        skillImg03.imageProperty().bind(viewModel.skillImg03Property());
        skillImg04.imageProperty().bind(viewModel.skillImg04Property());
        skillImg05.imageProperty().bind(viewModel.skillImg05Property());

        chain01.imageProperty().bind(viewModel.chainImg01Property());
        chain02.imageProperty().bind(viewModel.chainImg02Property());
        chain03.imageProperty().bind(viewModel.chainImg03Property());
        chain04.imageProperty().bind(viewModel.chainImg04Property());
        chain05.imageProperty().bind(viewModel.chainImg05Property());
        chain06.imageProperty().bind(viewModel.chainImg06Property());

        ColorAdjust colorAdjust1 = new ColorAdjust();

        colorAdjust1.setHue(-0.05);
        colorAdjust1.setBrightness(0.1);
        colorAdjust1.setSaturation(0.9);


        ColorAdjust colorAdjust2 = new ColorAdjust();
        //colorAdjust2.setHue(-0.5);
        colorAdjust2.setBrightness(-0.8);


     /*   skillImg01.setEffect(colorAdjust2);
        skillImg02.setEffect(colorAdjust2);
        skillImg03.setEffect(colorAdjust2);
        skillImg04.setEffect(colorAdjust2);
        skillImg05.setEffect(colorAdjust2);

        chain01.setEffect(colorAdjust1);
        chain02.setEffect(colorAdjust1);
        chain03.setEffect(colorAdjust1);
        chain04.setEffect(colorAdjust1);
        chain05.setEffect(colorAdjust1);
        chain06.setEffect(colorAdjust1);*/
        chain01.setEffect(colorAdjust1);
        chain02.setEffect(colorAdjust1);
        chain03.setEffect(colorAdjust1);
        chain04.setEffect(colorAdjust1);
        chain05.setEffect(colorAdjust1);
        chain06.setEffect(colorAdjust1);

 /*       chain01.effectProperty().bind(Bindings.when(viewModel.chainImgVisible01Property()).then(colorAdjust1).otherwise(colorAdjust2));
        chain02.effectProperty().bind(Bindings.when(viewModel.chainImgVisible02Property()).then(colorAdjust1).otherwise(colorAdjust2));
        chain03.effectProperty().bind(Bindings.when(viewModel.chainImgVisible03Property()).then(colorAdjust1).otherwise(colorAdjust2));
        chain04.effectProperty().bind(Bindings.when(viewModel.chainImgVisible04Property()).then(colorAdjust1).otherwise(colorAdjust2));
        chain05.effectProperty().bind(Bindings.when(viewModel.chainImgVisible05Property()).then(colorAdjust1).otherwise(colorAdjust2));
        chain06.effectProperty().bind(Bindings.when(viewModel.chainImgVisible06Property()).then(colorAdjust1).otherwise(colorAdjust2));*/

        chain01.visibleProperty().bind(viewModel.chainImgVisible01Property());
        chain02.visibleProperty().bind(viewModel.chainImgVisible02Property());
        chain03.visibleProperty().bind(viewModel.chainImgVisible03Property());
        chain04.visibleProperty().bind(viewModel.chainImgVisible04Property());
        chain05.visibleProperty().bind(viewModel.chainImgVisible05Property());
        chain06.visibleProperty().bind(viewModel.chainImgVisible06Property());

        roleBgPane.backgroundProperty().bind(viewModel.roleBgProperty());

        roleListview.setItems(viewModel.getRolePairList());
        roleListview.setCellFactory(pairListView -> new OwnRoleDetailCell());
        roleListview.getSelectionModel().selectedIndexProperty().addListener((observableValue, number, t1) -> {
            if (t1 != null) {
                viewModel.select(t1.intValue());
            }
        });


        phantomCost.textProperty().bind(viewModel.phantomCostProperty());
        phantomListView.setItems(viewModel.getPhantomList());
        phantomListView.setCellFactory(phantomListView1 -> new PhantomCell());

    }



    class PhantomCell extends ListCell<Pair<Phantom, Image>>{
        private ImageView iv=new ImageView();
        private Label cost=new Label();
        private StackPane root=new StackPane();
        private ImageView attrIV=new ImageView();

        public PhantomCell() {
            iv.setFitHeight(65);
            iv.setFitWidth(65);
            attrIV.setFitHeight(20);
            attrIV.setFitWidth(20);
            cost.getStyleClass().add("cost");
            root.getChildren().addAll(iv,attrIV,cost);
            StackPane.setAlignment(attrIV, Pos.TOP_RIGHT);
            StackPane.setAlignment(cost, Pos.BOTTOM_LEFT);
            root.setVisible(false);
            root.getStyleClass().add("phantom-detail-cell");
            setGraphic(root);
        }

        @Override
        protected void updateItem(Pair<Phantom, Image> pair, boolean b) {
            super.updateItem(pair, b);
            if (!b){
                iv.setImage(pair.getValue());
                cost.setText(String.valueOf(pair.getKey().getCost()));
                attrIV.setImage(LocalResourcesManager.imageBuffer(pair.getKey().getFetterDetail().getIconUrl()));


                switch (pair.getKey().getQuality()){
                    case 5 -> root.getStyleClass().add("ssr");
                    case 4 -> root.getStyleClass().add("sr");
                    default -> root.getStyleClass().add("r");
                }

                root.setVisible(true);


            }else {
                root.setVisible(false);
            }
        }
    }
}