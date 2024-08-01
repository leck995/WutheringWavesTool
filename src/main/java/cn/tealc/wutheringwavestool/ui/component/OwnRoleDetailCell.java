package cn.tealc.wutheringwavestool.ui.component;

import cn.tealc.wutheringwavestool.model.roleData.Role;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-31 22:43
 */
public class OwnRoleDetailCell extends ListCell<Pair<Role, Image>> {
    private final ImageView roleIV=new ImageView();
    private final Label name=new Label();
    private final Label level=new Label();
    private final HBox root=new HBox();
    public OwnRoleDetailCell() {
        roleIV.setFitHeight(45);
        roleIV.setFitWidth(45);
        roleIV.setPreserveRatio(true);
        root.setVisible(false);
        name.getStyleClass().add("role-name");
        level.getStyleClass().add("role-level");


        VBox vbox=new VBox(4.0,name,level);
        root.getStyleClass().add("role-detail-cell");

        root.getChildren().addAll(roleIV,vbox);
        setGraphic(root);

    }

    @Override
    protected void updateItem(Pair<Role, Image> roleImagePair, boolean b) {
        super.updateItem(roleImagePair, b);
        if (!b){
            root.setVisible(true);
            roleIV.setImage(roleImagePair.getValue());
            name.setText(roleImagePair.getKey().getRoleName());
            level.setText(String.format("LV.%d", roleImagePair.getKey().getLevel()));
        }else {
            root.setVisible(false);
        }
    }
}