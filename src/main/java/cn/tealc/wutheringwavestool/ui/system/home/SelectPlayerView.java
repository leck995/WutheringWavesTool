package cn.tealc.wutheringwavestool.ui.system.home;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import com.jfoenixN.controls.JFXDialogLayout;
import com.kuro.launcher.model.LocalCacheUser;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

import java.net.URL;
import java.util.ResourceBundle;

public class SelectPlayerView extends JFXDialogLayout implements JavaView<SelectedPlayerViewModel>, Initializable {
    @InjectViewModel
    private SelectedPlayerViewModel viewModel;

    private final ListView<LocalCacheUser> listview;

    private final Button okBtn;

    private final Button cancelBtn;


    public SelectPlayerView() {
        setPrefSize(450,350);
        Label title = new Label("选择账号");
        title.getStyleClass().add(Styles.TITLE_2);
        setHeading(title);

        listview = new ListView<>();
        Label tip = new Label("账号不存在的进入一次游戏后重试");
        tip.setWrapText(true);
        tip.getStyleClass().addAll(Styles.TEXT_MUTED,Styles.TEXT_SMALL);
        VBox box = new VBox(10,tip,listview);
        setBody(box);

        okBtn = new Button("确定");
        okBtn.setDefaultButton(true);
        cancelBtn = new Button("取消");
        cancelBtn.setCancelButton(true);
        setActions(okBtn, cancelBtn);
        okBtn.setOnAction(this::onOk);

        okBtn.disableProperty().bind(listview.getSelectionModel().selectedItemProperty().isNull());
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        listview.setItems(viewModel.getLocalCacheUserList());
        listview.setCellFactory(c -> new PlayerCell() {});
    }



    void onOk(ActionEvent event) {
        viewModel.update(listview.getSelectionModel().getSelectedIndex());
        cancelBtn.fire();
    }



    private static class PlayerCell extends ListCell<LocalCacheUser> {
        private final CheckBox checkBox = new CheckBox();
        private final Label text = new Label();
        private final static String TEMPLATE_TEXT = "%s (%s)";
        public PlayerCell() {
            checkBox.setMouseTransparent(true);
            checkBox.setFocusTraversable(true);

            text.setGraphic(new FontIcon(Material2MZ.PHONE_ANDROID));


            HBox box = new HBox(text,new Spacer(),checkBox);
            box.setAlignment(Pos.CENTER_LEFT);
            setGraphic(box);
            setGraphicTextGap(50.0);
        }

        @Override
        protected void updateItem(LocalCacheUser localCacheUser, boolean empty) {
            super.updateItem(localCacheUser, empty);
            if (!empty) {
                if (localCacheUser.getThirdNickName() != null){
                    text.setText(String.format(TEMPLATE_TEXT,localCacheUser.getPhone(),localCacheUser.getThirdNickName()));
                }else {
                    text.setText(localCacheUser.getPhone());
                }

            }else {
                text.setText(null);
            }
            checkBox.setVisible(!empty);
            text.setVisible(!empty);
        }

        @Override
        public void updateSelected(boolean empty) {
            super.updateSelected(empty);
            checkBox.setSelected(empty);
        }

    }
}
