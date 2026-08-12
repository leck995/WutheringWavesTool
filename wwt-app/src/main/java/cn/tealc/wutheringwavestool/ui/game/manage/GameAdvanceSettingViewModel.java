package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.dao.GameSettingDao;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.teafx.utils.message.MessageType;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.io.File;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-17 22:50
 */
public class GameAdvanceSettingViewModel implements ViewModel {
    private final SimpleStringProperty fps = new SimpleStringProperty();

    public GameAdvanceSettingViewModel() {
        if (hasDbFile()) {
            GameSettingDao gameSettingDao = new GameSettingDao();
            Pair<String, String> customFrameRate = gameSettingDao.getSettingValueByKey("CustomFrameRate");
            if (customFrameRate != null) {
                fps.set(customFrameRate.getValue());
            }
        }else {
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.game_manager.advance.fps.message02")));
        }
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
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,MessageInfo.error(LanguageManager.getString("ui.game_manager.advance.fps.message01")));
            }
        }else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,MessageInfo.error(LanguageManager.getString("ui.game_manager.advance.fps.message02")));
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

}