package cn.tealc.wutheringwavestool.ui.game;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.dao.GameSettingDao;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.io.File;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-17 22:50
 */
public class GameAppSettingViewModel implements ViewModel {
    private final SimpleStringProperty fps = new SimpleStringProperty();
    private final ObservableList<String> startUpParams;


    public GameAppSettingViewModel() {
        startUpParams = Config.setting.getStartUpParams();
        if (hasDbFile()) {
            GameSettingDao gameSettingDao = new GameSettingDao();
            Pair<String, String> customFrameRate = gameSettingDao.getSettingValueByKey("CustomFrameRate");
            if (customFrameRate != null) {
                fps.set(customFrameRate.getValue());
            }
        }else {
            NotificationManager.message(new MessageInfo(MessageType.WARNING,"无法找到配置数据库，请检测目录是否正确"));
        }
    }

    public void deleteParam(int index) {
        startUpParams.remove(index);
    }
    public void addParam(String param) {
        startUpParams.add(param);
    }

    public boolean isDx11(){
        return startUpParams.contains("-dx11");
    }
    public boolean isDx12(){
        return startUpParams.contains("-dx12");
    }

    public void addDx11(){
        int index = startUpParams.indexOf("-dx12");
        if (index != -1){
            startUpParams.set(index,"-dx11");
        }else {
            startUpParams.add("-dx11");
        }
    }
    public void addDx12(){
        int index = startUpParams.indexOf("-dx11");
        if (index != -1){
            startUpParams.set(index,"-dx12");
        }else {
            startUpParams.add("-dx12");
        }
    }

    public void replaceParam(String param1, String param2) {

    }

    /**
     * @description: 修改帧率，此处value必须为0，1，2，3
     * @param:	value
     * @return  void
     * @date:   2024/10/19
     */
    public void setFps(String value) {
        boolean exist = hasDbFile();
        if (exist) {
            GameSettingDao gameSettingDao = new GameSettingDao();
            boolean customFrameRate = gameSettingDao.updateSettingValueByKey("CustomFrameRate", value);
            if (!customFrameRate) {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.ERROR, LanguageManager.getString("ui.game_app.setting.fps.message01")));
            }
        }else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.ERROR, LanguageManager.getString("ui.game_app.setting.fps.message02")));
        }

    }

    public boolean hasDbFile(){
        File gameDB = GameResourcesManager.getGameDB();
        return gameDB != null;

    }



    public String getFps() {
        return fps.get();
    }

    public SimpleStringProperty fpsProperty() {
        return fps;
    }

    public ObservableList<String> getStartUpParams() {
        return startUpParams;
    }
}