package cn.tealc.wutheringwavestool.ui.kujiequ.sign;

import atlantafx.base.controls.ToggleSwitch;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.base.Config;
import com.kuro.kujiequ.model.sign.SignGood;
import com.kuro.kujiequ.model.sign.SignRecord;
import com.kuro.kujiequ.model.sign.UserInfo;

import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

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
    private FlowPane goodsView;
    @FXML
    private TextArea logArea;
    @FXML
    private ComboBox<UserInfo> accountBox;
    @FXML
    private Label isSignLabel;
    @FXML
    private ListView<SignRecord> signHistoryListView;
    @FXML
    private ToggleSwitch autoSignSwitch;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        accountBox.setItems(viewModel.getUserInfoList());
        accountBox.getSelectionModel().select(viewModel.getUserIndex());

        viewModel.userIndexProperty().bind(accountBox.getSelectionModel().selectedIndexProperty());



        viewModel.getGoodsList().addListener((ListChangeListener<? super SignGood>) observable -> {
            goodsView.getChildren().clear();
            for (SignGood signGood : observable.getList()) {
                goodsView.getChildren().add(createGoodCell(signGood));
            }
        });


        logArea.textProperty().bind(viewModel.logsProperty());


        isSignLabel.visibleProperty().bind(viewModel.isSignProperty().not());

        signHistoryListView.setItems(viewModel.getSignHistoryList());
        signHistoryListView.setFixedCellSize(40);
        signHistoryListView.setCellFactory(signRecordListView -> new SignHistoryListCell());


        autoSignSwitch.selectedProperty().bindBidirectional(Config.setting().autoKujieQuSignProperty());

    }

    @FXML
    void sign(ActionEvent event) {
        viewModel.sign();
    }

    public Pane createGoodCell(SignGood signGood) {
        ImageView goodIv=new ImageView();
        ImageView signedIv= new ImageView(new Image(FXResourcesLoader.load("image/kujiequ/signed.png"),20,20,true,true));
        Label num=new Label();
        Label index=new Label();
        signedIv.setVisible(signGood.getSign());
        goodIv.setImage(LocalResourcesManager.imageBuffer(signGood.getGoodsUrl(),60,60,true,true));
        num.setText(String.format("x%d",signGood.getGoodsNum()));
        index.setText(String.format("%02d",signGood.getSerialNum()+1));
        StackPane stackPane=new StackPane(goodIv,num,index,signedIv);
        StackPane.setAlignment(signedIv,Pos.TOP_RIGHT);
        StackPane.setAlignment(num, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(index, Pos.TOP_LEFT);
        index.getStyleClass().add("index");
        num.getStyleClass().add("num");
        goodIv.getStyleClass().add("pic");
        signedIv.getStyleClass().add("signed-pic");
        stackPane.getStyleClass().add("goods");
        return stackPane;
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