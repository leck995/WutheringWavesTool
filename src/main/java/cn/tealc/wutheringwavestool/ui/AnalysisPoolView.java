package cn.tealc.wutheringwavestool.ui;

import atlantafx.base.controls.ToggleSwitch;
import cn.tealc.wutheringwavestool.NotificationKey;
import cn.tealc.wutheringwavestool.model.analysis.SsrData;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.thread.DownloadHeadImgTask;
import cn.tealc.wutheringwavestool.ui.component.PoolNameCell;
import cn.tealc.wutheringwavestool.ui.component.SsrCell;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.utils.notifications.NotificationObserver;
import javafx.collections.ListChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class AnalysisPoolView implements Initializable, FxmlView<AnalysisPoolViewModel> {
    @InjectViewModel
    private AnalysisPoolViewModel viewModel;
    @FXML
    private HBox children;

    @FXML
    private Label currentCountLabel;

    @FXML
    private PieChart pieChart;

    @FXML
    private ListView<String> poolListview;

    @FXML
    private Label rLabel1;

    @FXML
    private Label rLabel2;

    @FXML
    private Label srLabel1;

    @FXML
    private Label srLabel2;

    @FXML
    private Label ssrLabel1;

    @FXML
    private Label ssrLabel2;

    @FXML
    private ListView<SsrData> ssrListView;

    @FXML
    private FlowPane ssrFlowPane;
    @FXML
    private Label totalCountLabel;
    @FXML
    private Label totalCostLabel;
    @FXML
    private Label ssrAvgLabel;

    @FXML
    private Label ssrMaxLabel;

    @FXML
    private Label ssrMinLabel;
    @FXML
    private ComboBox<String> playerComboBox;

    @FXML
    private ToggleSwitch ssrModelSwitch;

    private boolean playerChange=false;//代码控制角色切换标志
    private boolean poolChange=false;//代码控制卡池切换标志
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ssrModelSwitch.selectedProperty().bindBidirectional(viewModel.ssrModelProperty());
        totalCountLabel.textProperty().bind(viewModel.totalTextProperty());
        totalCostLabel.textProperty().bind(viewModel.totalCostTextProperty());
        currentCountLabel.textProperty().bind(viewModel.currentTextProperty());
        ssrLabel1.textProperty().bind(viewModel.ssrText1Property());
        ssrLabel2.textProperty().bind(viewModel.ssrText2Property());
        srLabel1.textProperty().bind(viewModel.srText1Property());
        srLabel2.textProperty().bind(viewModel.srText2Property());
        rLabel1.textProperty().bind(viewModel.rText1Property());
        rLabel2.textProperty().bind(viewModel.rText2Property());

        ssrAvgLabel.textProperty().bind(viewModel.ssrAvgTextProperty());
        ssrMaxLabel.textProperty().bind(viewModel.ssrMaxTextProperty());
        ssrMinLabel.textProperty().bind(viewModel.ssrMinTextProperty());


        poolListview.setItems(viewModel.getPoolNameList());
        poolListview.setCellFactory(stringListView -> new PoolNameCell());
        poolListview.getSelectionModel().selectedIndexProperty().addListener((observableValue, number, t1) -> {
            if (t1 != null && t1.intValue() >= 0 && !poolChange){
                viewModel.changePool(t1.intValue());
            }
        });
        if (!poolListview.getItems().isEmpty()){
            poolListview.getSelectionModel().select(0);
        }

        pieChart.titleProperty().bind(viewModel.chartTitleProperty());
        pieChart.setData(viewModel.getPieChartData());
        pieChart.setAnimated(false);
        pieChart.setClockwise(false);
        pieChart.setLabelsVisible(false);

        ssrListView.setCellFactory(ssrDataListView -> new SsrCell());

        playerComboBox.setItems(viewModel.getPlayerList());
        if (!viewModel.getPlayerList().isEmpty()){
            playerComboBox.getSelectionModel().select(viewModel.getPlayerList().indexOf(viewModel.getPlayer()));
        }
        playerComboBox.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            if (t1 != null && !playerChange){
                viewModel.changePlayer(t1);
            }
        });

        viewModel.subscribe("update-player", (s, objects) -> {
            playerChange=true;
            playerComboBox.getSelectionModel().select(viewModel.getPlayerList().indexOf(viewModel.getPlayer()));
            playerChange=false;
        });
        viewModel.subscribe("update", (s, objects) -> {
            poolChange=true;
            poolListview.getSelectionModel().select(0);
            poolChange=false;
        });


        reBind();
        ssrModelSwitch.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            reBind();
        });


    /*    ssrFlowPane.visibleProperty().bind(viewModel.ssrModelProperty().not());
        ssrFlowPane.disableProperty().bind(viewModel.ssrModelProperty());
        ssrListView.visibleProperty().bind(viewModel.ssrModelProperty());
        ssrListView.disableProperty().bind(viewModel.ssrModelProperty().not());*/
        ssrFlowPane.visibleProperty().bind(ssrModelSwitch.selectedProperty().not());
        ssrListView.visibleProperty().bind(ssrModelSwitch.selectedProperty());

    }

    private void reBind(){
        if (ssrModelSwitch.isSelected()){
            ssrListView.setItems(viewModel.getSsrList());
            ssrFlowPane.getChildren().clear();
        }else {
            ssrListView.setItems(null);
            ssrFlowPane.getChildren().clear();
            for (SsrData ssrData :  viewModel.getSsrList()) {
                ssrFlowPane.getChildren().add(new SsrChildView(ssrData));
            }
            viewModel.getSsrList().addListener((ListChangeListener<? super SsrData>) change -> {
                if (!ssrModelSwitch.isSelected()){
                    ssrFlowPane.getChildren().clear();
                    for (SsrData ssrData : change.getList()) {
                        ssrFlowPane.getChildren().add(new SsrChildView(ssrData));
                    }
                }
            });


        }
    }


    @FXML
    void load(ActionEvent event) {
        viewModel.load();
    }
    @FXML
    void fresh(ActionEvent event) {
        viewModel.refresh();
    }




    class SsrChildView extends StackPane {
        private ImageView iv=new ImageView();
        private Label count=new Label();
        public SsrChildView(SsrData ssrData) {
            VBox vBox=new VBox(iv,count);
            vBox.setAlignment(Pos.TOP_CENTER);
            getChildren().add(vBox);
            getStyleClass().add("child");
            iv.setFitWidth(70.0);
            iv.setFitHeight(70.0);
            File headFile=new File(String.format("assets/header/%s.png",ssrData.getName()));
            if (headFile.exists()){
                iv.setImage(new Image(headFile.toURI().toString(),70,70,true,true,true));
            }else {
                iv.setImage(null);
                if (!DownloadHeadImgTask.hasUpdate){
                    DownloadHeadImgTask task=new DownloadHeadImgTask();
                    task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                        @Override
                        public void handle(WorkerStateEvent workerStateEvent) {
                            if (task.getValue()){
                                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                                        new MessageInfo(MessageType.WARNING,"头像下载完毕，切换卡池即可查看"),false);
                            }
                        }
                    });
                    Thread.startVirtualThread(task);
                }
            }

            count.setText(String.valueOf(ssrData.getCount()));
            count.getStyleClass().add("count");

        }
    }


}