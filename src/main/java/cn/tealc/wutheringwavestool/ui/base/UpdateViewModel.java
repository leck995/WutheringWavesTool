package cn.tealc.wutheringwavestool.ui.base;

import cn.tealc.wutheringwavestool.base.AppConstants;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.release.Release;
import cn.tealc.wutheringwavestool.thread.system.DownloadUpdateTask;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-27 23:45
 */
public class UpdateViewModel implements ViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateViewModel.class);
    private final SimpleStringProperty version = new SimpleStringProperty();
    private final SimpleStringProperty name = new SimpleStringProperty();
    private final SimpleStringProperty description = new SimpleStringProperty();
    private final SimpleStringProperty type = new SimpleStringProperty();
    private final SimpleStringProperty dateTime = new SimpleStringProperty();
    private final SimpleDoubleProperty progressValue = new SimpleDoubleProperty();
    private final SimpleStringProperty progressLabel = new SimpleStringProperty();
    private final SimpleStringProperty packageSize = new SimpleStringProperty();
    private final SimpleBooleanProperty force = new SimpleBooleanProperty();
    private final SimpleStringProperty forceLabel = new SimpleStringProperty();
    private final SimpleBooleanProperty downloading = new SimpleBooleanProperty(false);
    private final Release release;

    public UpdateViewModel(Release release) {
        this.release = release;
        version.set(String.format("V%s -> V%s", AppConstants.VERSION, release.getVersion()));
        name.set(release.getName());
        description.set(release.getDescription());
        dateTime.set(release.getDate());
        type.set(release.isPre() ? LanguageManager.getString("ui.update.left.grid.type.key02") : LanguageManager.getString("ui.update.left.grid.type.key01"));
        forceLabel.set(release.isForce() ? LanguageManager.getString("ui.update.left.grid.force.key01") : LanguageManager.getString("ui.update.left.grid.force.key02"));
    }


    public void downloadZip(){
        progressValue.unbind();
        progressLabel.unbind();
        packageSize.unbind();
        DownloadUpdateTask task = new DownloadUpdateTask(release);
        progressValue.bind(task.progressProperty());
        progressLabel.bind(task.progressProperty().multiply(100).asString("%.2f%%"));
        packageSize.bind(task.titleProperty());
        downloading.set(true);
        task.setOnSucceeded(event -> {
            downloading.set(false);
            ResponseBody<Boolean> value = task.getValue();
            if (value.getCode() == 200){
                startUpdate();
            }else if (value.getCode() == 201){ //校验失败
                NotificationManager.message(MessageInfo.warning(value.getMsg()));
                downloadZip();
            }else {
                NotificationManager.message(MessageInfo.warning(value.getMsg()));
            }

        });
        Thread.startVirtualThread(task);
    }


    /**
     * @description: 启动bat,进行更新
     * @param:
     * @return  void
     * @date:   2024/12/24
     */
    private void startUpdate(){
        Thread thread = new Thread(() -> {
            File batFile = new File("Update.exe");
           //ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c","start", "/b", "\"\"" ,batFile.getAbsolutePath());
            //ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", batFile.getAbsolutePath());
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "cmd.exe", "/c", "start", "/b", "\"\"", "explorer.exe", batFile.getAbsolutePath()
            );
            try {
                processBuilder.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
    }

/*
    private void startUpdate(){
        Thread thread = new Thread(() -> {
            LOG.debug("复制update.bat至项目目录");
            InputStream batInputStream = this.getClass().getResourceAsStream("/update.bat");
            assert batInputStream != null;
            File batFile = new File("update.bat");
            try (FileOutputStream outputStream = new FileOutputStream(batFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = batInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                batInputStream.close();
            } catch (IOException e) {
                LOG.error(e.getMessage(),e);
            }

            LOG.debug("启动update.bat");
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c","start", "/b", "\"\"" ,batFile.getAbsolutePath());
            try {
                processBuilder.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
    }
*/

    public void setSkipVersion(){
        Config.setting().setSkipVersion(release.getVersion());
    }




    public String getVersion() {
        return version.get();
    }

    public SimpleStringProperty versionProperty() {
        return version;
    }

    public String getName() {
        return name.get();
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public String getDescription() {
        return description.get();
    }

    public SimpleStringProperty descriptionProperty() {
        return description;
    }

    public String getType() {
        return type.get();
    }

    public SimpleStringProperty typeProperty() {
        return type;
    }

    public String getDateTime() {
        return dateTime.get();
    }

    public SimpleStringProperty dateTimeProperty() {
        return dateTime;
    }

    public double getProgressValue() {
        return progressValue.get();
    }

    public SimpleDoubleProperty progressValueProperty() {
        return progressValue;
    }

    public String getProgressLabel() {
        return progressLabel.get();
    }

    public SimpleStringProperty progressLabelProperty() {
        return progressLabel;
    }

    public String getPackageSize() {
        return packageSize.get();
    }

    public SimpleStringProperty packageSizeProperty() {
        return packageSize;
    }

    public boolean isForce() {
        return force.get();
    }

    public SimpleBooleanProperty forceProperty() {
        return force;
    }

    public boolean isDownloading() {
        return downloading.get();
    }

    public SimpleBooleanProperty downloadingProperty() {
        return downloading;
    }

    public String getForceLabel() {
        return forceLabel.get();
    }

    public SimpleStringProperty forceLabelProperty() {
        return forceLabel;
    }
}