package cn.tealc.wutheringwavestool.ui.gacha;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.*;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.service.TaskManageService;
import cn.tealc.wutheringwavestool.thread.gacha.cloud.GachaCloudDeleteTask;
import cn.tealc.wutheringwavestool.thread.gacha.cloud.GachaCloudDownloadTask;
import cn.tealc.wutheringwavestool.thread.gacha.cloud.GachaCloudFileListGetTask;
import cn.tealc.wutheringwavestool.thread.gacha.cloud.GachaCloudUploadTask;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

public class CloudBackupViewModel extends BaseViewModel {

    private static final Logger LOG = LoggerFactory.getLogger(CloudBackupViewModel.class);
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
                refresh();
            }else {
                NotificationManager.message(MessageInfo.warning(value.getMsg()));
            }
        });

        task.setOnFailed(event -> {
            NotificationManager.message(MessageInfo.warning(event.getSource().getException().getMessage()));
        });
        AppInjector.getInstance(TaskManageService.class).execute(task);
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
                //刷新页面中的抽卡用户列表，显示最新的
                NotificationManager.publish(NotificationKey.CARD_POOL_USER_LIST_REFRESH);
            }else {
                NotificationManager.message(MessageInfo.warning("从云端取回记录失败，原因："+value.getMsg()));
            }
        });
        task.setOnFailed(ev ->{
            LOG.error("上传抽卡记录错误:{}",ev.getSource().getException().getMessage());
            NotificationManager.message(MessageInfo.error("下载失败: " + name));
                });

        AppInjector.getInstance(TaskManageService.class).execute(task);
    }



    public void deleteJson(CloudFileItem item) {
        GachaCloudDeleteTask task = new GachaCloudDeleteTask(item.getId(),
                Config.setting().getServerUsername(), Config.setting().getServerPassword());

        task.setOnSucceeded(event -> {
            ResponseBody<Void> value = task.getValue();
            if (value.getCode() == 200) {
                fileList.remove(item);
                NotificationManager.message(MessageInfo.success("删除成功"));
            } else {
                NotificationManager.message(MessageInfo.warning(value.getMsg()));
            }
        });
        task.setOnFailed(ev ->
                NotificationManager.message(MessageInfo.error("删除失败: " + item.getOriginalName())));
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
