package cn.tealc.wutheringwavestool.util;

import cn.tealc.wutheringwavestool.MainApplication;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * @program: WutheringWavesTool
 * @description: 用于判断多开，避免多开
 * @author: Leck
 * @create: 2024-11-24 20:00
 */
public class AppLocked{
    private static final Logger LOG = LoggerFactory.getLogger(AppLocked.class);
    private File file = new File("lock");
    private FileInputStream fis;
    public AppLocked() {
        try {
            if(file.exists()){
                boolean delete = file.delete();
                if(!delete){ //说明被占用了，助手存在,弹窗提示
                    LOG.info("检测到多开，即将关闭助手");
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.initOwner(MainApplication.window);
                    alert.setTitle(LanguageManager.getString("ui.app_locked.title"));
                    alert.setHeaderText(LanguageManager.getString("ui.app_locked.header"));
                    alert.setContentText(LanguageManager.getString("ui.app_locked.content"));
                    Optional<ButtonType> buttonType = alert.showAndWait();
                    System.exit(0);
                }else {
                    boolean newFile = file.createNewFile();
                    if(!newFile){
                        LOG.info("无法创建lock文件");
                    }
                    fis = new FileInputStream(file);
                }
            }else {
                boolean newFile = file.createNewFile();
                if(!newFile){
                    LOG.info("无法创建lock文件");
                }
                fis = new FileInputStream(file);
            }

            LOG.info("多开检测通过");
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }



    /**
     * @description: 释放文件
     * @param:
     * @return  void
     * @date:   2024/11/24
     */
    public void release(){
        try {
            fis.close();
            file.delete();
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }
}