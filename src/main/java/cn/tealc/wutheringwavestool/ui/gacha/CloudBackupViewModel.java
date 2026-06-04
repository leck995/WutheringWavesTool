package cn.tealc.wutheringwavestool.ui.gacha;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.*;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.thread.system.gachaUpload.GachaCloudDownloadTask;
import cn.tealc.wutheringwavestool.thread.system.gachaUpload.GachaCloudFileListGetTask;
import cn.tealc.wutheringwavestool.thread.system.gachaUpload.GachaCloudUploadTask;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.util.List;
import java.util.Map;

public class CloudBackupViewModel extends BaseViewModel {
    private final ObservableList<CloudFileItem> fileList = FXCollections.observableArrayList();
    private final ObservableList<CloudUploadItem> uploadList = FXCollections.observableArrayList();
    private final SimpleBooleanProperty loading = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty loaded = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty failed = new SimpleBooleanProperty(false);

    public void initialize() {
        refresh();
        loadUploadList();
    }

    private void loadUploadList() {
        Platform.runLater(() -> {
            uploadList.clear();
            File dataDir = new File("data");
            if (dataDir.exists() && dataDir.isDirectory()) {
                File[] subDirs = dataDir.listFiles(File::isDirectory);
                if (subDirs != null) {
                    for (File subDir : subDirs) {
                        File poolFile = new File(subDir, "pool.json");
                        if (poolFile.exists()) {
                            uploadList.add(new CloudUploadItem(subDir.getName(), poolFile));
                        }
                    }
                }
            }
        });
    }

    public void uploadJson(CloudUploadItem item) {
        String playerId= item.getPlayerId();
        File file = item.getFile();
        String username = Config.setting().getServerUsername();
        String password = Config.setting().getServerPassword();

        GachaCloudUploadTask task = new GachaCloudUploadTask(username,password,playerId,file);
        task.setOnSucceeded(event -> {
            ResponseBody<FileUploadResult> value = task.getValue();
            if (value.getCode() == 200){
                NotificationManager.message(MessageInfo.success("云备份成功"));
            }else {
                NotificationManager.message(MessageInfo.warning(value.getMsg()));
            }
        });

        task.setOnFailed(event -> {
            NotificationManager.message(MessageInfo.warning(event.getSource().getException().getMessage()));
        });
        Thread.startVirtualThread(task);


    }

    public void refresh() {
        loadFileList(Config.setting().getServerUsername(), Config.setting().getServerPassword());
    }

    public void loadFileList(String username, String password) {
        loading.set(true);
        loaded.set(false);
        failed.set(false);
        GachaCloudFileListGetTask task = new GachaCloudFileListGetTask(username, password);
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
                loading.set(false);
                loaded.set(true);
            } else {
                NotificationManager.message(MessageInfo.warning(value.getMsg()));
                loading.set(false);
                failed.set(true);
            }
        });
        task.setOnFailed(event -> {
            loading.set(false);
            failed.set(true);
            NotificationManager.message(MessageInfo.error("获取云文件列表失败"));
        });
        Thread.startVirtualThread(task);
    }

    public void downloadJson(CloudFileItem item){
        String name = item.getOriginalName();
        int idx = name.indexOf('-');
        String playerId = idx > 0 ? name.substring(0, idx) : name;

        GachaCloudDownloadTask task = new GachaCloudDownloadTask(item.getDownloadUrl(), playerId,
                Config.setting().getServerUsername(), Config.setting().getServerPassword());

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

    public ObservableList<CloudUploadItem> getUploadList() {
        return uploadList;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public SimpleBooleanProperty loadingProperty() {
        return loading;
    }

    public SimpleBooleanProperty loadedProperty() {
        return loaded;
    }

    public SimpleBooleanProperty failedProperty() {
        return failed;
    }
}
