package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.dao.GameSettingDao;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.Pair;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-17 22:50
 */
public class GameAppSettingViewModel implements ViewModel {
    private SimpleStringProperty fps = new SimpleStringProperty();
    public GameAppSettingViewModel() {
        GameSettingDao gameSettingDao = new GameSettingDao();
        Pair<String, String> customFrameRate = gameSettingDao.getSettingValueByKey("CustomFrameRate");
        if (customFrameRate != null) {
            fps.set(customFrameRate.getValue());
        }



    }

    /**
     * @description: 修改帧率，此处value必须为0，1，2，3
     * @param:	value
     * @return  void
     * @date:   2024/10/19
     */
    public void setFps(String value) {
        GameSettingDao gameSettingDao = new GameSettingDao();
        boolean customFrameRate = gameSettingDao.updateSettingValueByKey("CustomFrameRate", value);
        if (!customFrameRate) {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.ERROR, LanguageManager.getString("ui.game_app.setting.fps.message")));
        }
    }



    public String getFps() {
        return fps.get();
    }

    public SimpleStringProperty fpsProperty() {
        return fps;
    }
}