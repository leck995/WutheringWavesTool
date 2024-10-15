package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.model.kujiequ.roleData.FetterDetail;
import cn.tealc.wutheringwavestool.model.kujiequ.roleData.Phantom;
import cn.tealc.wutheringwavestool.model.kujiequ.roleData.PhoantomMainProps;
import cn.tealc.wutheringwavestool.model.kujiequ.roleData.Role;
import cn.tealc.wutheringwavestool.ui.component.OwnRoleDetailCell;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Pair;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
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
    private VBox phantomListGroup;


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

        viewModel.getPhantomList().addListener((ListChangeListener<? super Pair<Phantom, Image>>) change -> {
            phantomListGroup.getChildren().clear();
            HBox phantomTop = PhantomTop();
            phantomListGroup.getChildren().add(phantomTop);

            FlowPane flowPane =new FlowPane();
            flowPane.setHgap(15.0);
            flowPane.setVgap(15.0);

            phantomListGroup.getChildren().add(flowPane);

            if (!change.getList().isEmpty()) {
                for (Pair<Phantom, Image> phantomImagePair : change.getList()) {
                    flowPane.getChildren().add(new PhantomItem(phantomImagePair));
                }
                flowPane.getChildren().add(phantomGuidance());
            }

        });


    }


    /**
     * @description: 创建声骸头部信息，如套装效果，声骸词条总值
     * @param:
     * @return  javafx.scene.layout.HBox
     * @date:   2024/10/5
     */
    private HBox PhantomTop(){
        HBox hBox = new HBox();
        hBox.setSpacing(15.0);
        //套装效果
        if (!viewModel.getFetterDetails().isEmpty()) {
            VBox vBox = new VBox();
            vBox.setAlignment(Pos.CENTER_LEFT);
            vBox.getStyleClass().add("phantom-desc");
            vBox.setMaxWidth(300);
            vBox.setSpacing(5.0);




            Label tip = new Label("声骸评级:");
            Label level =new Label();
            Separator separator = new Separator(Orientation.HORIZONTAL);
            separator.setPrefWidth(200);
            tip.getStyleClass().add("tip");

            if (viewModel.getPhantomStatus() != null) {
                level.setText(viewModel.getPhantomStatus().getText());
                level.getStyleClass().add("status");
                switch (viewModel.getPhantomStatus().getLevel()){
                    case 1:  level.getStyleClass().add("n");
                    case 2:  level.getStyleClass().add("s");
                    case 3:  level.getStyleClass().add("ss");
                    case 4:  level.getStyleClass().add("sss");
                    case 5:  level.getStyleClass().add("ace");
                }
            }else {
                level.getStyleClass().add("tip");
                level.setText("当前无法评级");
            }



            HBox levelHbox = new HBox(10.0,tip,level);
            levelHbox.setAlignment(Pos.CENTER_LEFT);
            vBox.getChildren().addAll(levelHbox,separator);


            for (FetterDetail fetterDetail : viewModel.getFetterDetails()) {
                if (fetterDetail.getNum() == 5){
                    vBox.getChildren().add(fetterItem(fetterDetail,1));
                    vBox.getChildren().add(fetterItem(fetterDetail,2));
                }else if (fetterDetail.getNum() > 2){
                    vBox.getChildren().add(fetterItem(fetterDetail,1));
                }
            }
            hBox.getChildren().add(vBox);
        }

        //声骸词条总值
        if (!viewModel.getTotalPhantomValueList().isEmpty()){
            FlowPane right=new FlowPane();
            right.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(right,Priority.ALWAYS);
            right.setHgap(15.0);
            right.setVgap(5.0);
            right.getStyleClass().add("phantom-total");

            for (PhoantomMainProps prop : viewModel.getTotalPhantomValueList()) {
                Label label1=new Label(prop.getAttributeName());
                Label label2=new Label(prop.getAttributeValue());

                Rectangle thumb=new Rectangle(4.0,15.0);
                thumb.setArcWidth(4);
                thumb.setArcHeight(4);
                thumb.getStyleClass().add("thumb");
                label1.setGraphic(thumb);

                label1.getStyleClass().add("sub-prop1");
                label2.getStyleClass().add("sub-prop2");

                if (prop.getLevel() == 3){
                    thumb.getStyleClass().add("sss");
                }else if (prop.getLevel() == 2){
                    thumb.getStyleClass().add("ss");
                }else if (prop.getLevel() == 1){
                    thumb.getStyleClass().add("ss");
                }
                HBox hbox=new HBox(label1,label2);
                hbox.setPadding(new Insets(0,10,0,10));
                hbox.setPrefWidth(300);
                hbox.setMaxWidth(300);
                right.getChildren().add(hbox);
            }
            hBox.getChildren().add(right);
        }
        return hBox;
    }



    /**
     * @description: 套装文本显示
     * @param:	null
     * @return
     * @date:   2024/10/5
     */
    private Label fetterItem(FetterDetail detail,int index){
        Label label=new Label();
        ImageView iconIV = new ImageView();
        iconIV.setFitHeight(20);
        iconIV.setFitWidth(20);
        iconIV.setImage(LocalResourcesManager.imageBuffer(detail.getIconUrl(),20,20,true,true));
        if (index == 1){
            label.setText(detail.getFirstDescription());
        }else {
            label.setText(detail.getSecondDescription());
        }
        label.setGraphic(iconIV);
        label.setPrefWidth(280.0);
        label.setMinHeight(Label.USE_PREF_SIZE);
        label.setWrapText(true);
        return label;
    }






    private VBox phantomGuidance(){
        VBox vBox = new VBox();
        VBox top = new VBox();
        top.getStyleClass().add("top");
        Label tip1=new Label("评级说明:");
        tip1.getStyleClass().add("tip");

        Hyperlink button =new Hyperlink("详细说明");
        button.setVisited(true);
        button.setOnAction(actionEvent -> {
            try {
                Desktop.getDesktop().browse(
                        URI.create("https://github.com/leck995/WutheringWavesTool/wiki/%E5%A3%B0%E9%AA%B8%E6%B5%8B%E8%AF%84%E6%9C%BA%E5%88%B6%E8%AF%B4%E6%98%8E"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        StackPane stackPane = new StackPane(tip1,button);
        StackPane.setAlignment(tip1,Pos.CENTER_LEFT);
        StackPane.setAlignment(button,Pos.CENTER_RIGHT);

        Separator separator = new Separator(Orientation.HORIZONTAL);
        separator.setPrefWidth(200);

        top.getChildren().addAll(stackPane,separator);

        String text= """
                共ACE、SSS、SS、SS、N这5个等级。
             
                测评将声骸分为两个部分，分为词条和数值：
                    词条：统计对于角色的重要词条的多少
                    数值：统计当前词条数值与词条最大数
                           值的比值
                请注意当前ACE与SSS的要求比较苛刻，SS即可意味着可以毕业。
                
                该功能处于测试阶段，数据可能存在错误，自定义功能会在该功能完善时推出。
                """;

        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        textLabel.setPrefHeight(Label.USE_COMPUTED_SIZE);



        vBox.getChildren().addAll(top,textLabel);
        vBox.setSpacing(8.0);
        vBox.setPrefWidth(300);
        vBox.setMinWidth(300);
        vBox.getStyleClass().add("phantom");
        return vBox;
    }






    class PhantomItem extends VBox{
        private ImageView iv=new ImageView();
        private Label level=new Label();
        private ImageView attrIV=new ImageView();

        public PhantomItem(Pair<Phantom, Image> pair) {
            setPrefWidth(300);
            setMinWidth(300);
            Phantom phantom = pair.getKey();
            VBox top = new VBox();
            top.getStyleClass().add("top");
            HBox countPane = new HBox();
            countPane.setSpacing(15.0);
            if (phantom.getStatus() != null){
                Label tip1=new Label("词条:");
                Label tip2=new Label("词条数值:");
                tip1.getStyleClass().add("tip");
                tip2.getStyleClass().add("tip");

                Label status1=new Label();
                Label status2=new Label();

                status1.getStyleClass().add("status");
                status2.getStyleClass().add("status");

                status1.setText(phantom.getStatus().getText());
                if (phantom.getStatus() == Phantom.Status.ACE){
                    status1.getStyleClass().add("ace");
                } else if (phantom.getStatus() == Phantom.Status.SSS){
                    status1.getStyleClass().add("sss");
                }else if (phantom.getStatus() == Phantom.Status.SS){
                    status1.getStyleClass().add("ss");
                }else if (phantom.getStatus() == Phantom.Status.S){
                    status1.getStyleClass().add("s");
                }else {
                    status1.getStyleClass().add("n");
                }


                status2.setText(phantom.getPropStatus().getText());
                if (phantom.getPropStatus() == Phantom.Status.ACE){
                    status2.getStyleClass().add("ace");
                } else if (phantom.getPropStatus() == Phantom.Status.SSS){
                    status2.getStyleClass().add("sss");
                }else if (phantom.getPropStatus() == Phantom.Status.SS){
                    status2.getStyleClass().add("ss");
                }else if (phantom.getPropStatus() == Phantom.Status.S){
                    status2.getStyleClass().add("s");
                }else {
                    status2.getStyleClass().add("n");
                }

                HBox topChild1=new HBox(15.0,tip1,status1);
                HBox topChild2=new HBox(15.0,tip2,status2);
                topChild1.setAlignment(Pos.CENTER_LEFT);
                topChild2.setAlignment(Pos.CENTER_LEFT);


                countPane.getChildren().addAll(topChild1,topChild2);
            }else {
                Label label =new Label("当前无法评级");
                label.getStyleClass().add("tip");
                countPane.getChildren().addAll(label);
            }



            Separator separator = new Separator(Orientation.HORIZONTAL);
            separator.setPrefWidth(200);

            top.getChildren().addAll(countPane,separator);

            //头像
            StackPane iconPane=new StackPane();
            iconPane.getStyleClass().add("icon");
            iv.setFitHeight(65);
            iv.setFitWidth(65);
            attrIV.setFitHeight(20);
            attrIV.setFitWidth(20);
            level.getStyleClass().add("cost");
            iconPane.getChildren().addAll(iv,attrIV,level);
            StackPane.setAlignment(attrIV, Pos.TOP_LEFT);
            StackPane.setAlignment(level, Pos.BOTTOM_RIGHT);
            iv.setImage(pair.getValue());
            level.setText(String.valueOf(phantom.getLevel()));
            attrIV.setImage(LocalResourcesManager.imageBuffer(pair.getKey().getFetterDetail().getIconUrl()));
            switch (pair.getKey().getQuality()){
                case 5 -> iconPane.getStyleClass().add("ssr");
                case 4 -> iconPane.getStyleClass().add("sr");
                default -> iconPane.getStyleClass().add("r");
            }
            //主词条
            VBox mainProp=new VBox();
            for (int i = 0; i < phantom.getMainProps().size(); i++) {
                PhoantomMainProps prop = phantom.getMainProps().get(i);
                Label label1=new Label(prop.getAttributeName());
                Label label2=new Label(prop.getAttributeValue());
                label1.getStyleClass().add("main-prop1");
                label2.getStyleClass().add("main-prop2");
                HBox hbox=new HBox(label1,label2);
                hbox.setSpacing(20);
                mainProp.getChildren().add(hbox);
            }

            HBox mainHBox=new HBox();
            mainHBox.setSpacing(5.0);
            mainHBox.getChildren().addAll(iconPane,mainProp);






            VBox subPropVBox=new VBox();
            subPropVBox.setSpacing(8.0);
            if (phantom.getSubProps() != null){
                for (int i = 0; i < phantom.getSubProps().size(); i++) {
                    PhoantomMainProps prop = phantom.getSubProps().get(i);
                    Label label1=new Label(String.format("%s(%.1f)",prop.getAttributeName(),prop.getAttributeMaxValue()));
                    Label label2=new Label(prop.getAttributeValue());

                    Rectangle thumb=new Rectangle(4.0,15.0);
                    thumb.setArcWidth(4);
                    thumb.setArcHeight(4);
                    thumb.getStyleClass().add("thumb");
                    label1.setGraphic(thumb);

                    label1.getStyleClass().add("sub-prop1");
                    label2.getStyleClass().add("sub-prop2");


                    if (prop.getLevel() == 3){
                        thumb.getStyleClass().add("sss");
                    }else if (prop.getLevel() == 2){
                        thumb.getStyleClass().add("ss");
                    }else if (prop.getLevel() == 1){
                        thumb.getStyleClass().add("ss");
                    }
                    HBox hbox=new HBox(label1,label2);
                    hbox.setSpacing(10);
                    subPropVBox.getChildren().addAll(hbox);
                }
            }






            subPropVBox.getStyleClass().add("sub-prop");
            mainHBox.getStyleClass().add("main-prop");
            getChildren().addAll(top,mainHBox,subPropVBox);

            setSpacing(8.0);
            getStyleClass().add("phantom");
        }
    }

}