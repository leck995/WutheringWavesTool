package cn.tealc.wutheringwavestool.ui;

import atlantafx.base.util.Animations;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.model.kujiequ.roleData.Role;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.*;
import javafx.animation.Timeline;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.util.Pair;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-29 22:13
 */
public class OwnRoleView implements FxmlView<OwnRoleViewModel>, Initializable {
    private static final Logger LOG= LoggerFactory.getLogger(OwnRoleView.class);

    @InjectViewModel
    private OwnRoleViewModel viewModel;
    @FXML
    private StackPane root;
    @FXML
    private AnchorPane child;
    @FXML
    private FlowPane roleFlowPane;

    private List<ImageView> imageViews = new ArrayList<>();
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        for (Role role :   viewModel.getRoleList()) {
            roleFlowPane.getChildren().add(createItem(role));
        }
        viewModel.getRoleList().addListener((ListChangeListener<? super Role>) change -> {
            roleFlowPane.getChildren().clear();
            for (Role role : change.getList()) {
                roleFlowPane.getChildren().add(createItem(role));
            }
        });

    }


    private Pane createItem(Role role){
        ImageView roleIV=new ImageView(LocalResourcesManager.imageBuffer(role.getRoleIconUrl(),110,110,true,true));
        imageViews.add(roleIV);
        roleIV.setFitHeight(110);
        roleIV.setFitWidth(110);
        roleIV.setPreserveRatio(true);


        Label roleName=new Label(role.getRoleName());
        roleName.getStyleClass().add("name");
        Label level=new Label(String.format("LV.%d",role.getLevel()));
        level.getStyleClass().add("level");
        VBox parent=new VBox(roleIV,level,roleName);

        parent.setAlignment(Pos.BOTTOM_CENTER);
        ImageView attrIV=new ImageView(
                new Image(
                        FXResourcesLoader.load(
                                String.format("/cn/tealc/wutheringwavestool/image/attr/%d.png",role.getAttributeId())),
                        30,30,true,true,true));


        attrIV.setFitHeight(30);
        attrIV.setFitWidth(30);

        StackPane item=new StackPane();
        item.setPrefSize(120,140);


        Rectangle thumb=new Rectangle(100.0,5.0);
        thumb.setArcWidth(5);
        thumb.setArcHeight(5);
        if (role.getStarLevel()==5){
            thumb.setFill(Color.web("#f8f05c"));
        }else {
            thumb.setFill(Color.web("#bc60f2"));
        }
        item.getChildren().addAll(parent,attrIV,thumb);
        StackPane.setAlignment(attrIV, Pos.TOP_RIGHT);
        StackPane.setAlignment(thumb, Pos.BOTTOM_CENTER);

        item.getStyleClass().add("role-cell");

        item.setOnContextMenuRequested(contextMenuEvent -> {
            Menu main= new Menu("主页");
            MenuItem menuItem01=new MenuItem("设为头像");
            main.getItems().addAll(menuItem01);

            menuItem01.setOnAction(event -> {
                String filename = LocalResourcesManager.addHomeIcon(role.getRoleName(), role.getRoleIconUrl());
                Config.setting.setHomeViewIcon(filename);
            });

            ContextMenu contextMenu=new ContextMenu();
            contextMenu.getItems().addAll(main);
            contextMenu.show(item,contextMenuEvent.getScreenX(),contextMenuEvent.getScreenY());
        });



        item.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton()== MouseButton.PRIMARY){
                if (imageViews.size()==viewModel.getRoleList().size()){
                    List<Pair<Role,Image>> list = new ArrayList<>();
                    for (int i = 0; i < imageViews.size(); i++) {
                        list.add(new Pair<>(viewModel.getRoleList().get(i),imageViews.get(i).getImage()));
                    }
                    int index = viewModel.getRoleList().indexOf(role);
                    ViewTuple<OwnRoleDetailView,OwnRoleDetailViewModel> viewTuple = FluentViewLoader
                            .fxmlView(OwnRoleDetailView.class)
                            .viewModel(new OwnRoleDetailViewModel(viewModel.getUserInfo(),index,list))
                            .load();

                    Button backBtn=new Button("返回", new FontIcon(Material2AL.EXIT_TO_APP));
                    backBtn.getStyleClass().add("back-btn");

                    Parent view = viewTuple.getView();
                    StackPane stackPane=new StackPane(view,backBtn);
                    StackPane.setAlignment(backBtn,Pos.TOP_LEFT);
                    backBtn.setOnAction(event -> {
                        Timeline timeline = Animations.slideOutRight(stackPane, Duration.millis(300));
                        timeline.setOnFinished(event1 -> {
                            root.getChildren().removeLast();
                            child.setVisible(true);
                        });
                        timeline.play();

                    });
                    root.getChildren().add(stackPane);

                    child.setVisible(false);
                    Timeline timeline = Animations.slideInRight(root, Duration.millis(300));
                    timeline.play();
                }else {
                    LOG.error("出错了，角色数量与图片不一致,无法进入详情界面");
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.ERROR,"出错了，角色数量与图片不一致"));
                }
            }

        });
        return item;
    }





}