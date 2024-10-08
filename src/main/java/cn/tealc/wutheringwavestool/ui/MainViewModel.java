package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.thread.CheckVersionTask;
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
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.WARNING,"网络异常，无法检测新版本"));
                }else if (task.getValue()==1){
                    Platform.runLater(()->{
                        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.INFO,"有新版本，请前往设置更新"));
                    });
                }
            });
            Thread.startVirtualThread(task);
        }
    }
}