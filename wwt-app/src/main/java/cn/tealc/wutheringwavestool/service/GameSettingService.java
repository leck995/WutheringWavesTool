package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.dao.GameSettingDao;
import com.google.inject.Singleton;
import javafx.util.Pair;

/**
 * 游戏设置服务，包装 {@link GameSettingDao}（操作游戏自身的 SQLite 数据库）
 */
@Singleton
public class GameSettingService {

    private final GameSettingDao gameSettingDao = new GameSettingDao();

    public Pair<String, String> getSettingValueByKey(String key) {
        return gameSettingDao.getSettingValueByKey(key);
    }

    public boolean updateSettingValueByKey(String key, String value) {
        return gameSettingDao.updateSettingValueByKey(key, value);
    }
}