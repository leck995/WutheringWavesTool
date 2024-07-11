package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.model.analysis.SsrData;
import cn.tealc.wutheringwavestool.ui.component.PoolNameCell;
import cn.tealc.wutheringwavestool.ui.component.SsrCell;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.utils.notifications.NotificationObserver;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
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

    private boolean playerChange=false;//代码控制角色切换标志
    private boolean poolChange=false;//代码控制卡池切换标志
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

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
        ssrListView.setItems(viewModel.getSsrList());
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

    }

    @FXML
    void load(ActionEvent event) {
        viewModel.load();
    }
    @FXML
    void fresh(ActionEvent event) {
        viewModel.refresh();
    }





}