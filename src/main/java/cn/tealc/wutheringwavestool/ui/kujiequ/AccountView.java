package cn.tealc.wutheringwavestool.ui.kujiequ;

import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.ui.account.AccountUpdateView;
import cn.tealc.wutheringwavestool.ui.account.AccountUpdateViewModel;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-08-04 00:26
 */
public class AccountView implements FxmlView<AccountViewModel>, Initializable {
    private static final Logger LOG= LoggerFactory.getLogger(AccountView.class);
    @InjectViewModel
    private AccountViewModel viewModel;
    @FXML
    private ListView<UserInfo> accountListView;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        accountListView.setItems(viewModel.getAccountList());
        accountListView.setCellFactory((ListView<UserInfo> listView) -> new AccountCell());
    }

    @FXML
    void addUser(ActionEvent event) {
        ViewTuple<AccountUpdateView, AccountUpdateViewModel> viewTuple = FluentViewLoader.fxmlView(AccountUpdateView.class).viewModel(new AccountUpdateViewModel()).load();
        MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG, viewTuple.getView(),viewTuple.getCodeBehind());
    }

    class AccountCell extends ListCell<UserInfo> {
        private final Label userId=new Label();
        private final Label roleId=new Label();
        private final Label index=new Label();
        private final Button delete=new Button(null,new FontIcon(Material2AL.DELETE));
        private final Button update=new Button(null,new FontIcon(Material2AL.EDIT));

        public AccountCell() {
            userId.getStyleClass().add("user-label");
            roleId.getStyleClass().add("role-label");
            delete.setVisible(false);
            delete.getStyleClass().add("delete-btn");
            delete.setOnAction(event -> {
                if (getItem() != null){
                    delete();
                }
            });

            update.setVisible(false);
            update.getStyleClass().add("delete-btn");
            update.setOnAction(event -> {
                if (getItem() != null){
                    update();
                }
            });

            VBox vbox=new VBox(3.0,userId,roleId);
            vbox.setAlignment(Pos.CENTER_LEFT);

            HBox hbox=new HBox(10.0,index,vbox,update,delete);
            HBox.setHgrow(vbox, Priority.ALWAYS);
            hbox.getStyleClass().add("user");
            hbox.setPadding(new Insets(5.0,5.0,5.0,5.0));
            hbox.setAlignment(Pos.CENTER_LEFT);
            setGraphic(hbox);
        }

        @Override
        protected void updateItem(UserInfo signUserInfo, boolean b) {
            super.updateItem(signUserInfo, b);
            if (!b){
                index.setText(String.valueOf(getIndex()+1));
                userId.setText("用户ID: "+signUserInfo.getUserId());
                roleId.setText("游戏ID: "+signUserInfo.getRoleId());
                delete.setVisible(true);
                update.setVisible(true);
            }else {
                index.setText(null);
                roleId.setText(null);
                userId.setText(null);
                delete.setVisible(false);
                update.setVisible(false);
            }
        }

        private void delete(){
            JFXDialogLayout dialogLayout = new JFXDialogLayout();
            Label title=new Label("确认");
            title.getStyleClass().add(Styles.TITLE_2);
            dialogLayout.setHeading(title);
            Label content=new Label(String.format("确认删除用户ID: %s 的数据吗",getItem().getUserId()));
            dialogLayout.setBody(content);
            Button saveBtn=new Button("确认");
            saveBtn.getStyleClass().add(Styles.ACCENT);
            Button cancelBtn=new Button("取消");
            cancelBtn.setCancelButton(true);

            saveBtn.setOnAction(event1 -> {
                viewModel.deleteUser(getIndex(),getItem());
                cancelBtn.fireEvent(event1); //这里是为了触发cancelBtn的事件，从而关闭窗口，属实另辟途径（自夸）
            });
            dialogLayout.setActions(saveBtn, cancelBtn);
            MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG,dialogLayout);

        }

        private void update(){
            ViewTuple<AccountUpdateView, AccountUpdateViewModel> viewTuple = FluentViewLoader.fxmlView(AccountUpdateView.class).viewModel(new AccountUpdateViewModel(getItem())).load();
            MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG, viewTuple.getView(),viewTuple.getCodeBehind());
        }


    }


}