package cn.tealc.wutheringwavestool.ui;

import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-08-04 04:57
 */
public class GameTimeView implements FxmlView<GameTimeViewModel>, Initializable {
    @InjectViewModel
    private GameTimeViewModel viewModel;
    @FXML
    private LineChart<String, Double> lineChart;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        lineChart.setTitle("七天时长统计");
        lineChart.setData(viewModel.getChartData());
    }
}