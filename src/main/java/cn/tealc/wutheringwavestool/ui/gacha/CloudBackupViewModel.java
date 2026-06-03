package cn.tealc.wutheringwavestool.ui.gacha;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.CloudFileItem;
import cn.tealc.wutheringwavestool.model.CloudFileListData;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.thread.system.gachaUpload.CloudFileDownloadTask;
import cn.tealc.wutheringwavestool.thread.system.gachaUpload.CloudFileListGetTask;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Map;

public class CloudBackupViewModel extends BaseViewModel {
    private final ObservableList<CloudFileItem> fileList = FXCollections.observableArrayList();
    private final SimpleBooleanProperty loading = new SimpleBooleanProperty(false);

    public void initialize() {
        loadFileList(Config.setting().getServerUsername(), Config.setting().getServerPassword());
    }

    public void loadFileList(String username, String password) {
        loading.set(true);
        CloudFileListGetTask task = new CloudFileListGetTask(username, password);
        task.setOnSucceeded(event -> {
            ResponseBody<CloudFileListData> value = task.getValue();
            if (value.getCode() == 200 && value.getData() != null) {
                Platform.runLater(() -> {
                    fileList.clear();
                    Map<String, List<CloudFileItem>> folders = value.getData().getFolders();
                    if (folders != null) {
                        for (List<CloudFileItem> items : folders.values()) {
                            fileList.addAll(items);
                        }
                    }
                });
            } else {
                NotificationManager.message(MessageInfo.warning(value.getMsg()));
            }
            loading.set(false);
        });
        task.setOnFailed(event -> {
            loading.set(false);
            NotificationManager.message(MessageInfo.error("获取云文件列表失败"));
        });
        Thread.startVirtualThread(task);
    }

    public void downloadJson(CloudFileItem item){
        String name = item.getOriginalName();
        int idx = name.indexOf('-');
        String playerId = idx > 0 ? name.substring(0, idx) : name;

        CloudFileDownloadTask task = new CloudFileDownloadTask(item.getDownloadUrl(),playerId);

        task.setOnSucceeded(event -> {
            ResponseBody<Void> value = task.getValue();
            if (value.getCode() == 200){
                NotificationManager.message(MessageInfo.success("从云端取回记录成功"));
            }else {
                NotificationManager.message(MessageInfo.warning("从云端取回记录失败，原因："+value.getMsg()));
            }
        });
        task.setOnFailed(ev ->
                NotificationManager.message(MessageInfo.error("下载失败: " + name)));
        Thread.startVirtualThread(task);
    }



    public ObservableList<CloudFileItem> getFileList() {
        return fileList;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public SimpleBooleanProperty loadingProperty() {
        return loading;
    }
}
