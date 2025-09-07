package cn.tealc.wutheringwavestool.ui.cardpool;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.CardInfo;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.analysis.AnalysisData;
import cn.tealc.wutheringwavestool.model.analysis.SsrData;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.thread.system.CardPoolRequestTask;
import cn.tealc.wutheringwavestool.util.FileIO;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewModel;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 01:18
 */
public class CardDetailAnalysisViewModel implements ViewModel {
    private static final Logger LOG= LoggerFactory.getLogger(CardDetailAnalysisViewModel.class);
    private ObservableList<String> poolNameList= FXCollections.observableArrayList();

    private ObservableList<CardInfo> cardInfoList= FXCollections.observableArrayList();
    private ObservableList<PieChart.Data> pieChartData= FXCollections.observableArrayList();
    private SimpleStringProperty player=new SimpleStringProperty();
    private Map<String, List<CardInfo>> data;

    private ObservableList<AnalysisData> analysisDataList= FXCollections.observableArrayList();
    private ObservableList<SsrData> ssrList= FXCollections.observableArrayList();
    private SimpleStringProperty totalText=new SimpleStringProperty("0");
    private SimpleStringProperty totalCostText=new SimpleStringProperty("0");
    private SimpleStringProperty currentText=new SimpleStringProperty("0");
    private SimpleStringProperty ssrText1=new SimpleStringProperty();
    private SimpleStringProperty ssrText2=new SimpleStringProperty();
    private SimpleStringProperty srText1=new SimpleStringProperty();
    private SimpleStringProperty srText2=new SimpleStringProperty();
    private SimpleStringProperty rText1=new SimpleStringProperty();
    private SimpleStringProperty rText2=new SimpleStringProperty();
    private SimpleStringProperty chartTitle=new SimpleStringProperty();

    private SimpleStringProperty ssrAvgText=new SimpleStringProperty();
    private SimpleStringProperty ssrMinText=new SimpleStringProperty();
    private SimpleStringProperty ssrMaxText=new SimpleStringProperty();
    private SimpleStringProperty ssrEventAvgText=new SimpleStringProperty(); //限定平均抽数

    private SimpleBooleanProperty ssrModel=new SimpleBooleanProperty();
    private SimpleBooleanProperty empty = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty poolEmpty = new SimpleBooleanProperty(true);
    private SimpleBooleanProperty loading = new SimpleBooleanProperty(false);


    public CardDetailAnalysisViewModel(boolean isEmpty) {
        empty.set(isEmpty);
        loading.set(!isEmpty);

        ssrModel.bindBidirectional(Config.setting.gachaListModelProperty());
        NotificationManager.subscribe(NotificationKey.CARD_POOL_USER_UPDATE,(s, objects) -> {
            empty.set(false);
            @SuppressWarnings("unchecked")
            List<AnalysisData> list = (List<AnalysisData>) objects[0];
            updatePlayer(list);
            loading.set(false);
        });

        NotificationManager.subscribe(NotificationKey.CARD_POOL_USER_EMPTY,(s, objects) -> {
            reset();
            empty.set(true);
            loading.set(false);
        });
    }

    private void updatePlayer(List<AnalysisData> list) {
        analysisDataList.setAll(list);
        poolNameList.setAll(list.stream().map(AnalysisData::getPoolName).collect(Collectors.toList()));
        changePool(0);
        publish("update");
    }

    /**
     * @description: 切换池子数据
     * @param:	key
     * @return  void
     * @date:   2024/7/3
     */
    public void changePool(int index){
        AnalysisData analysis = analysisDataList.get(index);
        if (analysis.isEmpty()){
            poolEmpty.set(true);
            resetCurrentPool();
        }else {
            poolEmpty.set(false);
            updatePoolDate(analysis);
        }

    }

    /**
     * @description: 更新对应文本
     * @param:	analysis
     * @return  void
     * @date:   2024/7/4
     */
    private void updatePoolDate(AnalysisData analysis){
        //文本
        totalText.set(String.valueOf(analysis.getTotalCount()));
        totalCostText.set(String.valueOf(analysis.getTotalCount() * 160));
        currentText.set(String.valueOf(analysis.getNoUpSsrCount()));
        ssrText1.set(String.format("SSR: %d",analysis.getSsrCount()));
        ssrText2.set(String.format("[ %.3f ]",((double)analysis.getSsrCount()/(double)analysis.getTotalCount())));
        srText1.set(String.format("SR: %d",analysis.getSrCount()));
        srText2.set(String.format("[ %.3f ]",((double)analysis.getSrCount()/(double)analysis.getTotalCount())));
        rText1.set(String.format("R: %d",analysis.getrCount()));
        rText2.set(String.format("[ %.3f ]",((double)analysis.getrCount()/(double)analysis.getTotalCount())));
        chartTitle.set(analysis.getPoolName());
        ssrAvgText.set(String.format("%.3f", analysis.getSsrAvg()));
        ssrMaxText.set(String.valueOf(analysis.getSsrMax()));
        ssrMinText.set(String.valueOf(analysis.getSsrMin()));
        int sum = analysis.getSsrDataList().stream().mapToInt(ssrData -> ssrData.getCount()).sum();
        long count = analysis.getSsrDataList().stream().filter(SsrData::isEvent).count();
        if (count != 0){
            ssrEventAvgText.set(String.format("%.3f", (double)sum / (double) count));
        }else {
            ssrEventAvgText.set(null);
        }


        ssrList.setAll(analysis.getSsrDataList());
        //扇形图数据
        List<PieChart.Data> pieChartDataList=new ArrayList<>();
        pieChartDataList.add(new PieChart.Data("SSR",analysis.getSsrCount()));
        pieChartDataList.add(new PieChart.Data("SR",analysis.getSrCount()));
        pieChartDataList.add(new PieChart.Data("R",analysis.getrCount()));
        pieChartData.setAll(pieChartDataList);
    }


    /**
     * 当当前卡池为空时，清除残余数据
     */
    private void resetCurrentPool(){
        totalText.set(null);
        totalCostText.set(null);
        currentText.set(null);
        ssrText1.set(null);
        ssrText2.set(null);
        srText1.set(null);
        srText2.set(null);
        rText1.set(null);
        rText2.set(null);
        chartTitle.set(null);
        ssrAvgText.set(null);
        ssrMaxText.set(null);
        ssrMinText.set(null);
        ssrEventAvgText.set(null);
        ssrList.clear();
        pieChartData.clear();
    }


    /**
     * 当无用户数据，清空所有数据
     */
    private void reset(){
        resetCurrentPool();
        poolNameList.clear();
        analysisDataList.clear();
    }



    public ObservableList<CardInfo> getCardInfoList() {
        return cardInfoList;
    }

    public void setCardInfoList(ObservableList<CardInfo> cardInfoList) {
        this.cardInfoList = cardInfoList;
    }

    public ObservableList<String> getPoolNameList() {
        return poolNameList;
    }

    public void setPoolNameList(ObservableList<String> poolNameList) {
        this.poolNameList = poolNameList;
    }

    public ObservableList<PieChart.Data> getPieChartData() {
        return pieChartData;
    }

    public void setPieChartData(ObservableList<PieChart.Data> pieChartData) {
        this.pieChartData = pieChartData;
    }

    public ObservableList<AnalysisData> getAnalysisDataList() {
        return analysisDataList;
    }

    public void setAnalysisDataList(ObservableList<AnalysisData> analysisDataList) {
        this.analysisDataList = analysisDataList;
    }

    public ObservableList<SsrData> getSsrList() {
        return ssrList;
    }

    public String getTotalText() {
        return totalText.get();
    }

    public SimpleStringProperty totalTextProperty() {
        return totalText;
    }

    public String getCurrentText() {
        return currentText.get();
    }

    public SimpleStringProperty currentTextProperty() {
        return currentText;
    }

    public String getSsrText1() {
        return ssrText1.get();
    }

    public SimpleStringProperty ssrText1Property() {
        return ssrText1;
    }

    public String getSsrText2() {
        return ssrText2.get();
    }

    public SimpleStringProperty ssrText2Property() {
        return ssrText2;
    }

    public String getSrText1() {
        return srText1.get();
    }

    public SimpleStringProperty srText1Property() {
        return srText1;
    }

    public String getSrText2() {
        return srText2.get();
    }

    public SimpleStringProperty srText2Property() {
        return srText2;
    }

    public String getrText1() {
        return rText1.get();
    }

    public SimpleStringProperty rText1Property() {
        return rText1;
    }

    public String getrText2() {
        return rText2.get();
    }

    public SimpleStringProperty rText2Property() {
        return rText2;
    }

    public String getChartTitle() {
        return chartTitle.get();
    }

    public SimpleStringProperty chartTitleProperty() {
        return chartTitle;
    }

    public String getTotalCostText() {
        return totalCostText.get();
    }

    public SimpleStringProperty totalCostTextProperty() {
        return totalCostText;
    }

    public String getSsrAvgText() {
        return ssrAvgText.get();
    }

    public SimpleStringProperty ssrAvgTextProperty() {
        return ssrAvgText;
    }

    public String getSsrMinText() {
        return ssrMinText.get();
    }

    public SimpleStringProperty ssrMinTextProperty() {
        return ssrMinText;
    }

    public String getSsrMaxText() {
        return ssrMaxText.get();
    }

    public SimpleStringProperty ssrMaxTextProperty() {
        return ssrMaxText;
    }

    public String getPlayer() {
        return player.get();
    }

    public SimpleStringProperty playerProperty() {
        return player;
    }

    public boolean isSsrModel() {
        return ssrModel.get();
    }

    public SimpleBooleanProperty ssrModelProperty() {
        return ssrModel;
    }

    public String getSsrEventAvgText() {
        return ssrEventAvgText.get();
    }

    public SimpleStringProperty ssrEventAvgTextProperty() {
        return ssrEventAvgText;
    }

    public boolean isEmpty() {
        return empty.get();
    }

    public SimpleBooleanProperty emptyProperty() {
        return empty;
    }

    public boolean isPoolEmpty() {
        return poolEmpty.get();
    }

    public SimpleBooleanProperty poolEmptyProperty() {
        return poolEmpty;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public SimpleBooleanProperty loadingProperty() {
        return loading;
    }
}
