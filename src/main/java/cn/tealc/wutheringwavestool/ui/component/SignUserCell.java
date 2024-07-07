package cn.tealc.wutheringwavestool.ui.component;

import cn.tealc.wutheringwavestool.MainApplication;
import cn.tealc.wutheringwavestool.NotificationKey;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-07 21:19
 */
public class SignUserCell extends ListCell<SignUserInfo> {
    private final Label userId=new Label();
    private final Label roleId=new Label();
    private final Label index=new Label();
    private final Button delete=new Button(null,new FontIcon(Material2AL.DELETE));

    public SignUserCell() {
        userId.getStyleClass().add("user-label");
        roleId.getStyleClass().add("role-label");
        delete.setVisible(false);
        delete.setOnAction(event -> {
            if (getItem() != null){
                delete();
            }
        });

        VBox vbox=new VBox(3.0,userId,roleId);
        vbox.setAlignment(Pos.CENTER);

        HBox hbox=new HBox(15.0,index,vbox,delete);
        hbox.setPadding(new Insets(5.0,5.0,5.0,5.0));
        hbox.setAlignment(Pos.CENTER_LEFT);
        setGraphic(hbox);
    }

    @Override
    protected void updateItem(SignUserInfo signUserInfo, boolean b) {
        super.updateItem(signUserInfo, b);
        if (!b){
            index.setText(String.valueOf(getIndex()+1));
            userId.setText("用户ID: "+signUserInfo.userId());
            roleId.setText("游戏ID: "+signUserInfo.roleId());
            delete.setVisible(true);
        }else {
            index.setText(null);
            roleId.setText(null);
            userId.setText(null);
            delete.setVisible(false);
        }
    }

    private void delete(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(null);
        alert.setContentText(String.format("确认删除用户ID: %s 的数据吗",getItem().userId()));
        alert.initOwner(MainApplication.window);
        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK){
                MvvmFX.getNotificationCenter().publish(NotificationKey.SIGN_USER_DELETE,getItem());
            }
        });


    }
}