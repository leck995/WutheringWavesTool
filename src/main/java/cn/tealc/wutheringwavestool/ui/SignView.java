package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.model.sign.SignGood;
import cn.tealc.wutheringwavestool.model.sign.SignRecord;
import cn.tealc.wutheringwavestool.model.sign.UserInfo;

import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
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

    }

    @FXML
    void sign(ActionEvent event) {
        viewModel.sign();
    }

    public Pane createGoodCell(SignGood signGood) {
        ImageView imageView=new ImageView();
        imageView.setFitHeight(60);
        imageView.setFitWidth(60);
        Label name=new Label("已签");
        Label num=new Label();
        Label index=new Label();
        name.getStyleClass().add("name");
        index.getStyleClass().add("index");
        num.getStyleClass().add("num");
        name.setVisible(signGood.getSign());
        imageView.setImage(LocalResourcesManager.imageBuffer(signGood.getGoodsUrl(),60,60,true,true));
        num.setText(String.format("x%d",signGood.getGoodsNum()));
        index.setText(String.format("%02d",signGood.getSerialNum()+1));
        StackPane stackPane=new StackPane(imageView,num,index,name);
        StackPane.setAlignment(num, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(index, Pos.TOP_LEFT);
        stackPane.setPrefSize(70,90);
        stackPane.setPadding(new Insets(3));
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