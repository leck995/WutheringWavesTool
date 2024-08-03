package cn.tealc.wutheringwavestool.ui;

import atlantafx.base.layout.InputGroup;
import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.MainApplication;
import cn.tealc.wutheringwavestool.NotificationKey;
import cn.tealc.wutheringwavestool.model.sign.SignGood;
import cn.tealc.wutheringwavestool.model.sign.SignRecord;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.model.sign.UserInfo;
import cn.tealc.wutheringwavestool.ui.component.SignGoodCell;
import cn.tealc.wutheringwavestool.ui.component.SignUserCell;

import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.controlsfx.control.GridView;

import javax.swing.text.Style;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-07 17:53
 */
public class SignView implements Initializable, FxmlView<SignViewModel> {
    @InjectViewModel
    private SignViewModel viewModel;
    @FXML
    private GridView<SignGood> goodsListview;
    @FXML
    private TextArea logArea;
    @FXML
    private ComboBox<UserInfo> accountBox;
    @FXML
    private Label isSignLabel;
    @FXML
    private ListView<SignRecord> signHistoryListView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        accountBox.setItems(viewModel.getUserInfoList());
        accountBox.getSelectionModel().select(viewModel.getUserIndex());

        viewModel.userIndexProperty().bind(accountBox.getSelectionModel().selectedIndexProperty());

        goodsListview.setItems(viewModel.getGoodsList());
        goodsListview.setCellWidth(70);
        goodsListview.setCellHeight(90);
        goodsListview.setCellFactory((GridView<SignGood> view) -> new SignGoodCell());
        //goodsListview.setHorizontalCellSpacing(5.0);
        goodsListview.setVerticalCellSpacing(5.0);

        logArea.textProperty().bind(viewModel.logsProperty());


        isSignLabel.visibleProperty().bind(viewModel.isSignProperty().not());

        signHistoryListView.setItems(viewModel.getSignHistoryList());
        signHistoryListView.setFixedCellSize(40);
        signHistoryListView.setCellFactory(signRecordListView -> new SignHistoryListCell());

    }

    @FXML
    void sign(ActionEvent event) {
        viewModel.sign();
    }

    class SignHistoryListCell extends ListCell<SignRecord> {
        private ImageView iv=new ImageView();
        private Label name=new Label();

        public SignHistoryListCell() {
            iv.setFitHeight(30);
            iv.setFitWidth(30);
            name.setGraphic(iv);
            setGraphic(name);
        }

        @Override
        protected void updateItem(SignRecord signRecord, boolean b) {
            super.updateItem(signRecord, b);
            if (!b){
                iv.setImage(LocalResourcesManager.imageBuffer(signRecord.getGoodsUrl(),30,30,true,true));
                name.setText(String.format("%s x%d",signRecord.getGoodsName(),signRecord.getGoodsNum()));
                name.setVisible(true);
            }else {
                name.setVisible(false);
            }
        }

        @Override
        public void updateSelected(boolean b) {

        }
    }
}