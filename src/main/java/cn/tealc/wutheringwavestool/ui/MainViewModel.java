package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.kujiequ.sign.UserInfo;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.DifficultyTotal;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.thread.CheckVersionTask;
import cn.tealc.wutheringwavestool.thread.api.TowerDataDetailTask;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewModel;
import javafx.application.Platform;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 18:59
 */
public class MainViewModel implements ViewModel {
    public MainViewModel() {
        if (Config.setting.isCheckNewVersion()){
            CheckVersionTask task = new CheckVersionTask();
            task.setOnSucceeded(workerStateEvent -> {
                if (task.getValue()==-1){
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.main.message.type01")));
                }else if (task.getValue()==1){
                    Platform.runLater(()->{
                        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.INFO,LanguageManager.getString("ui.main.message.type02")));
                    });
                }
            });
            Thread.startVirtualThread(task);
        }
        updateKujiequ();
    }


    private void updateKujiequ(){
        if (!Config.setting.isNoKuJieQu()){
            //获取深塔刷新时间，同时更新深塔历史记录
            UserInfoDao dao = new UserInfoDao();
            UserInfo main = dao.getMain();
            if (main!=null){
                TowerDataDetailTask task = new TowerDataDetailTask(main);
                task.setOnSucceeded(workerStateEvent -> {
                    ResponseBody<DifficultyTotal> value = task.getValue();
                    if (value.getCode() == 200){
                        long milliseconds = value.getData().getSeasonEndTime();
                        long millisecondsInADay = 24 * 60 * 60 * 1000;
                        double days = (double) milliseconds / (double) millisecondsInADay;
                        if (days > 0 && days < 1){//不足一天时,提醒
                            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.main.sync.message.tower")));
                        }
                    }
                });
                Thread.startVirtualThread(task);
            }
        }
    }
}