package cn.tealc.wutheringwavestool.ui.component;

import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.model.roleData.Role;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.controlsfx.control.GridCell;



/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-29 22:54
 */
public class OwnRoleCell extends GridCell<Role> {
    private final StackPane root=new StackPane();
    private final ImageView roleIV=new ImageView();
    private final Label roleName=new Label();
    private final Label level=new Label();
    private final ImageManagerForGridView manager;
    private final ImageView attrIV=new ImageView();
    public OwnRoleCell(ImageManagerForGridView manager) {
        this.manager = manager;
        roleIV.setFitHeight(110);
        roleIV.setFitWidth(110);
        roleIV.setPreserveRatio(true);
        //root.setVisible(false);

        VBox parent=new VBox(roleIV,roleName,level);
        parent.setPrefSize(120,140);

        attrIV.setFitHeight(25);
        attrIV.setFitWidth(25);
        root.setPrefSize(120,140);
        root.getChildren().addAll(parent,attrIV);

        setGraphic(root);
        setPrefSize(120,140);
        setMinSize(120,140);
    }

    @Override
    protected void updateItem(Role role, boolean b) {
        super.updateItem(role, b);
        if (!b){
            roleName.setText(role.getRoleName());
            level.setText(String.valueOf(role.getLevel()));


            attrIV.setImage(
                    new Image(FXResourcesLoader.load(String.format("/cn/tealc/wutheringwavestool/image/attr/%d.png",role.getAttributeId())), 25,25,true,true,false));
            //manager.setImage(role.getRoleIconUrl(), roleIV);
            roleIV.setImage(LocalResourcesManager.imageBuffer(role.getRoleIconUrl()));

        }else {
            //root.setVisible(false);
        }
    }
}