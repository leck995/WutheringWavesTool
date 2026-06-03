package cn.tealc.wutheringwavestool.ui.gacha;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.game.pool.CardInfo;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.analysis.AnalysisData;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.thread.system.CardPoolAnalysisTask;
import cn.tealc.wutheringwavestool.thread.system.CardPoolRequestTask;
import cn.tealc.wutheringwavestool.thread.system.gachaUpload.GachaUploadTask;
import cn.tealc.wutheringwavestool.util.FileIO;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.fasterxml.jackson.core.type.TypeReference;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-23 12:30
 */
public class CardAnalysisBaseViewModel extends BaseViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(CardAnalysisBaseViewModel.class);
    public static final String EVENT_SELECTED_PLAYER = "EVENT_SELECTED_PLAYER";
    private static final int TEN_MB = 10 * 1024 * 1024;
    @Inject
    private ObjectMapper objectMapper;
    private SimpleStringProperty player = new SimpleStringProperty();
    private ObservableList<String> playerList = FXCollections.observableArrayList();
    private List<AnalysisData> poolData;

    public CardAnalysisBaseViewModel() {
        player.bindBidirectional(Config.setting().gachaCurrentPlayerIdProperty());
        CompletableFuture.delayedExecutor(300, TimeUnit.MILLISECONDS).execute(()->{
            Platform.runLater(()->{
                loadFile(player.get());
            });

        });
    }




    public void loadFile(String playerId) {
        Thread.startVirtualThread(()->{
            //查看本地是否存有数据，有则加载
            File dataDir = new File("data");
            if (dataDir.exists()) {
                File[] players = dataDir.listFiles(File::isDirectory);
                if (players != null) {
                    List<String> directoryNames = Arrays.stream(players)
                            .map(File::getName)
                            .collect(Collectors.toList());
                    playerList.setAll(directoryNames);
                }
                if (playerId != null && !playerList.isEmpty() && playerList.contains(playerId)) {
                    player.set(playerId);
                    publish(EVENT_SELECTED_PLAYER);
                } else {
                    if (!playerList.isEmpty()) {
                        player.set(playerList.getLast());
                        publish(EVENT_SELECTED_PLAYER);
                    }else{
                        NotificationManager.publish(NotificationKey.CARD_POOL_USER_EMPTY);
                    }
                }
            }else {
                NotificationManager.publish(NotificationKey.CARD_POOL_USER_EMPTY);
            }
        });
    }



    public void changePlayer(String playerId) {
        int index = playerList.indexOf(playerId);
        if (index != -1){
            player.set(playerId);
            analysis(playerId);
        }
    }


    private void analysis(String playerId) {
        CardPoolAnalysisTask task = new CardPoolAnalysisTask(playerId);
        task.setOnSucceeded(e -> {
            ResponseBody<List<AnalysisData>> response = task.getValue();
            if (response.getCode() == 200) {
                poolData = response.getData();
                NotificationManager.publish(NotificationKey.CARD_POOL_USER_UPDATE,response.getData());
            }
        });
        Thread.startVirtualThread(task);
    }


    public void refreshFromNet() {
        File dataJson = new File(String.format("data/%s/data.json", player.get()));
        try {
            if (dataJson.exists()) {
                Map<String, String> playerParams = objectMapper.readValue(dataJson, new TypeReference<Map<String, String>>() {
                });
                if (player != null && playerParams != null) {
                    CardPoolRequestTask task = new CardPoolRequestTask(playerParams);
                    task.setOnSucceeded(workerStateEvent -> {
                        ResponseBody<Map<String, List<CardInfo>>> responseBody = task.getValue();
                        if (responseBody.getCode() == 200) {
                            analysis(player.get());
                            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                                    new MessageInfo(MessageType.SUCCESS, LanguageManager.getString("ui.analysis.message.type01")));
                        } else {
                            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                                    new MessageInfo(MessageType.WARNING, responseBody.getMsg()));
                        }
                    });
                    Thread.startVirtualThread(task);
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                            new MessageInfo(MessageType.INFO, LanguageManager.getString("ui.analysis.message.type02")));
                } else {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                            new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.analysis.message.type03")));
                }
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }



    }

    public boolean delete() {
        File dataDir = new File("data/" + player.get());
        if (dataDir.exists()) {
            boolean isSuccess = FileIO.deleteDirectory(dataDir);
            if (isSuccess) {
                playerList.remove(player.get());
                if (!playerList.isEmpty()) {
                    player.set(playerList.getLast());
                    changePlayer(player.get());
                } else {
                    poolData = null;
                    NotificationManager.publish(NotificationKey.CARD_POOL_USER_EMPTY);
                }
                NotificationManager.message(MessageInfo.success(LanguageManager.getString("ui.analysis.message.type08")));
            } else {
                NotificationManager.message(MessageInfo.error(LanguageManager.getString("ui.analysis.message.type09")));
            }
            return isSuccess;
        }
        return false;
    }


    public void uploadGachaFile(){
        String username = Config.setting().getServerUsername();
        String password = Config.setting().getServerPassword();
        String playerId = getPlayer();
        File dateJson=new File(String.format("data/%s/pool.json",playerId));
        System.out.println(dateJson.getAbsolutePath());
        if (!dateJson.exists()){
            NotificationManager.message(MessageInfo.warning("选中用户的抽卡数据不存在"));
            return;
        }
        if (dateJson.length() > TEN_MB){
            NotificationManager.message(MessageInfo.warning("选中用户的抽卡数据文件过大，无法备份，请联系开发者"));
            return;
        }

        GachaUploadTask task = new GachaUploadTask(username,password,playerId,dateJson);
        task.setOnSucceeded(event -> {
            if (task.getValue().getCode() == 200){
                NotificationManager.message(MessageInfo.success("选中用户的抽卡数据备份成功"));
            }else {
                NotificationManager.message(MessageInfo.warning(task.getValue().getMsg()));
            }
        });

        task.setOnFailed(event ->  {
            NotificationManager.message(MessageInfo.warning(task.getException().getMessage()));
        }  );
        Thread.startVirtualThread(task);
        NotificationManager.message(MessageInfo.info("开始进行云备份"));
    }

    public void loadFromNet() {
        CardPoolRequestTask task = new CardPoolRequestTask();
        task.setOnSucceeded(workerStateEvent -> {
            ResponseBody<Map<String, List<CardInfo>>> responseBody = task.getValue();
            if (responseBody.getCode() == 200) {
                analysis(player.get());
                String playerId = responseBody.getMsg();
                if (!playerList.contains(playerId)) {
                    playerList.add(playerId);
                }
                this.player.set(playerId);
                publish(EVENT_SELECTED_PLAYER);
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.SUCCESS, LanguageManager.getString("ui.analysis.message.type01")));
                publish("upload");
            } else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.WARNING, responseBody.getMsg()));
            }
        });
        Thread.startVirtualThread(task);
        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                new MessageInfo(MessageType.INFO, LanguageManager.getString("ui.analysis.message.type02")));
    }

    public String getPlayer() {
        return player.get();
    }

    public SimpleStringProperty playerProperty() {
        return player;
    }

    public ObservableList<String> getPlayerList() {
        return playerList;
    }

    public List<AnalysisData> getPoolData() {
        return poolData;
    }
}