package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.model.CardInfo;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.model.analysis.AnalysisData;
import cn.tealc.wutheringwavestool.model.analysis.SsrData;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.thread.CardPoolRequestTask;
import cn.tealc.wutheringwavestool.util.LogFileUtil;
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

import java.io.*;
import java.util.*;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 01:18
 */
public class AnalysisPoolViewModel implements ViewModel {
    private static final List<String> BASE_SSR_LIST=List.of("凌阳","安可","卡卡罗","维里奈","鉴心","浩境粼光","千古洑流","停驻之烟","擎渊怒涛 ","漪澜浮录");

    private SimpleStringProperty gameRootDir = new SimpleStringProperty();
    private ObservableList<String> poolNameList= FXCollections.observableArrayList();



    private ObservableList<CardInfo> cardInfoList= FXCollections.observableArrayList();
    private ObservableList<PieChart.Data> pieChartData= FXCollections.observableArrayList();
    private ObservableList<String> playerList= FXCollections.observableArrayList();
    private Map<String, String> playerParams;
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

    private SimpleBooleanProperty ssrModel=new SimpleBooleanProperty(true);

    public AnalysisPoolViewModel() {
        gameRootDir.bindBidirectional(Config.setting.gameRootDirProperty());

        Thread.startVirtualThread(()->{
            //查看本地是否存有数据，有则加载
            File dataDir=new File("data");
            if (dataDir.exists()) {
                String[] players = dataDir.list((dir, name) -> dir.isDirectory());
                playerList.setAll(players);
                if (!playerList.isEmpty()){
                    player.set(playerList.getLast());
                    updatePlayer();
                }
            }
        });

    }




    public void refresh() {
        if (player != null && playerParams != null){
            CardPoolRequestTask task=new CardPoolRequestTask(playerParams,Config.setting.getGameRootDir());
            task.setOnSucceeded(workerStateEvent -> {
                data=task.getValue();
                init();
                savePoolData(playerParams);
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.SUCCESS,"抽卡数据已完成分析"));
            });
            Thread.startVirtualThread(task);
            pieChartData.clear();
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    new MessageInfo(MessageType.INFO,"正在获取数据，请稍候"));
        }else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    new MessageInfo(MessageType.WARNING,"角色不存在，请使用获取功能"));
        }
    }


    public void load() {
        String dir = Config.setting.getGameRootDir();
        if (dir != null) {
            File file=null;
            if (Config.setting.getGameRootDirSource() == SourceType.DEFAULT){
                file=new File(dir + File.separator + "Wuthering Waves Game/Client/Saved/Logs/Client.log");
            }else {
                file=new File(dir + File.separator + "Client/Saved/Logs/Client.log");
            }

            if (file.exists()){
                String url = LogFileUtil.getLogFileUrl(file);
                if (url != null){
                    Map<String, String> params = LogFileUtil.getParamFromUrl(url);
                    CardPoolRequestTask task=new CardPoolRequestTask(params,Config.setting.getGameRootDir());
                    task.setOnSucceeded(workerStateEvent -> {
                        data = task.getValue();
                        init();
                        String playerId = params.get("playerId");
                        if (!playerList.contains(playerId)){
                            playerList.add(playerId);
                        }
                        this.player.set(playerId);
                        savePoolData(params);
                        publish("update-player");
                        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                                new MessageInfo(MessageType.SUCCESS,"抽卡数据已完成分析"));
                    });
                    Thread.startVirtualThread(task);
                    pieChartData.clear();
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                            new MessageInfo(MessageType.INFO,"正在获取数据，请稍候"));
                }else {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                            new MessageInfo(MessageType.WARNING,"无法获取抽卡数据链接，请打开游戏内抽卡的历史记录页面后再次尝试"),false);
                }
            }else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.WARNING,"抽卡数据不存在，请打开游戏内抽卡的历史记录页面后再次尝试"),false);
            }
        }else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    new MessageInfo(MessageType.WARNING,"请在设置在先设置游戏目录"),false);
        }
    }




    /**
     * @description: 切换池子数据
     * @param:	key
     * @return  void
     * @date:   2024/7/3
     */
    public void changePool(int index){
        AnalysisData analysis = analysisDataList.get(index);
        updatePoolDate(analysis);
    }

    public void changePlayer(String player){
        int index = playerList.indexOf(player);
        if (index != -1){
            this.player.set(player);
            updatePlayer();
        }
    }


    /**
     * @description: 保存角色及卡池信息至本地
     * @param:	params
     * @return  void
     * @date:   2024/7/4
     */
    private void savePoolData(Map<String, String> params){
        String playerId = params.get("playerId");

 /*       File userDataDir=new File(String.format("data/%s",playerId));
        if (userDataDir.exists()){
            userDataDir.lastModified()
        }
*/
        ObjectMapper mapper = new ObjectMapper();
        File poolJson=new File(String.format("data/%s/pool.json",playerId));
        File dateJson=new File(String.format("data/%s/data.json",playerId));
        File parentDir = poolJson.getParentFile();
        File dataDir = parentDir.getParentFile();
        if (!dataDir.exists()){
            dataDir.mkdirs();
        }
        if (!parentDir.exists()){
            parentDir.mkdirs();
        }
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(poolJson,data);
            mapper.writerWithDefaultPrettyPrinter().writeValue(dateJson,params);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void updatePlayer(){
        File poolJson=new File(String.format("data/%s/pool.json",player.get()));
        File dateJson=new File(String.format("data/%s/data.json",player.get()));
        if(poolJson.exists()){
            Thread.startVirtualThread(()->{
                ObjectMapper mapper = new ObjectMapper();
                try {
                    data= mapper.readValue(poolJson, new TypeReference<Map<String, List<CardInfo>>>() {});
                    playerParams=mapper.readValue(dateJson, new TypeReference<Map<String, String>>() {});
                    init();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    /**
     * @description: 获取并分析数据
     * @param:
     * @return  void
     * @date:   2024/7/4
     */
    private void init(){
        poolNameList.setAll(data.keySet());
        Thread.startVirtualThread(() -> {
            List<AnalysisData> list=new ArrayList<>();
            for (Map.Entry<String, List<CardInfo>> entry : data.entrySet()) {
                list.add(analysis(entry.getKey(),entry.getValue().stream().toList()));
            }
            analysisDataList.setAll(list);
            Platform.runLater(()->{
                updatePoolDate(analysisDataList.getFirst());
            });

            publish("update");
        });

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
        currentText.set(String.valueOf(analysis.getCurrentCount()));
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
/*        if (analysis.getSsrCount()!=0){
            pieChartDataList.add(new PieChart.Data("SSR",analysis.getSsrCount()));
        }
        if (analysis.getSrCount()!=0){
            pieChartDataList.add(new PieChart.Data("SR",analysis.getSrCount()));
        }
        if (analysis.getrCount()!=0){
            pieChartDataList.add(new PieChart.Data("R",analysis.getrCount()));
        }*/
        pieChartDataList.add(new PieChart.Data("SSR",analysis.getSsrCount()));
        pieChartDataList.add(new PieChart.Data("SR",analysis.getSrCount()));
        pieChartDataList.add(new PieChart.Data("R",analysis.getrCount()));
        pieChartData.setAll(pieChartDataList);
    }

    private AnalysisData analysis(String name,List<CardInfo> cardInfoList){
        AnalysisData analysisData=new AnalysisData();
        analysisData.setTotalCount(cardInfoList.size());
        analysisData.setPoolName(name);


        int srCount=0;
        int rCount=0;
        //先获取五星在列表中的索引
        List<Integer> ssrIndexList=new ArrayList<>();
        for (int i = 0; i < cardInfoList.size(); i++) {
            CardInfo cardInfo = cardInfoList.get(i);
            if (cardInfo.getQualityLevel() == 5){
                ssrIndexList.add(i);
            }else if (cardInfo.getQualityLevel() == 4){
                srCount++;
            }else if (cardInfo.getQualityLevel() == 3){
                rCount++;
            }
        }

        analysisData.setSsrCount(ssrIndexList.size());
        analysisData.setSrCount(srCount);
        analysisData.setrCount(rCount);

        if (!ssrIndexList.isEmpty()){
            analysisData.setCurrentCount(ssrIndexList.getFirst());
            List<SsrData> ssrDataList=new ArrayList<>();
            SsrData ssrData;
            int ssrMin=80;
            int ssrMax=0;
            double totalCount=0;
            //按照五星索引进行列表分段
            for (int i = 0; i < ssrIndexList.size(); i++) {
                Integer index = ssrIndexList.get(i);
                CardInfo cardInfo = cardInfoList.get(ssrIndexList.get(i));
                List<CardInfo> cardInfos;
                if (i == ssrIndexList.size() - 1){
                    cardInfos = cardInfoList.subList(index, cardInfoList.size());
                }else {
                    cardInfos = cardInfoList.subList(index, ssrIndexList.get(i+1));
                }
                if (cardInfos.size() < ssrMin){
                    ssrMin=cardInfos.size();
                }
                if (cardInfos.size() > ssrMax){
                    ssrMax=cardInfos.size();
                }
                totalCount+=cardInfos.size();
                ssrData=new SsrData();
                ssrData.setDate(cardInfo.getTime());
                ssrData.setName(cardInfo.getName());
                ssrData.setCount(cardInfos.size());
                ssrData.setEvent(!BASE_SSR_LIST.contains(cardInfo.getName()));
                ssrDataList.add(ssrData);
            }
            double avg = totalCount / ssrIndexList.size();
            analysisData.setSsrAvg(avg);
            analysisData.setSsrMax(ssrMax);
            analysisData.setSsrMin(ssrMin);

            analysisData.setSsrDataList(ssrDataList);
        }else {
            analysisData.setCurrentCount(cardInfoList.size());
            analysisData.setSsrAvg(0.0);
            analysisData.setSsrMax(0);
            analysisData.setSsrMin(0);
            analysisData.setSsrDataList(new ArrayList<>());
        }

        return analysisData;
    }

    public String getGameRootDir() {
        return gameRootDir.get();
    }

    public SimpleStringProperty gameRootDirProperty() {
        return gameRootDir;
    }

    public void setGameRootDir(String gameRootDir) {
        this.gameRootDir.set(gameRootDir);
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

    public ObservableList<String> getPlayerList() {
        return playerList;
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
}