package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.dao.GameTimeDao;
import cn.tealc.wutheringwavestool.model.game.GameTime;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Singleton
public class GameTimeService {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final GameTimeDao gameTimeDao;

    @Inject
    public GameTimeService(GameTimeDao gameTimeDao) {
        this.gameTimeDao = gameTimeDao;
    }

    /** 获取今日所有游玩记录 */
    public List<GameTime> getTodayGameTimes() {
        return gameTimeDao.getTimeListByData(DATE_FMT.format(LocalDate.now()));
    }

    /** 计算今日总游玩时长（毫秒） */
    public long getTodayTotalDuration() {
        List<GameTime> list = getTodayGameTimes();
        if (list == null) return 0;
        return list.stream().mapToLong(GameTime::getDuration).sum();
    }

    /** 计算今日总游玩时长 + 当前未保存的时长（毫秒） */
    public long getTodayTotalDuration(long unsavedDuration) {
        return getTodayTotalDuration() + unsavedDuration;
    }

    /** 格式化游玩时长为 时:分 文本 */
    public static String formatDuration(long durationMillis) {
        int hour = (int) (durationMillis / (1000 * 60 * 60));
        int minute = (int) ((durationMillis % (1000 * 60 * 60)) / (1000 * 60));
        return String.format("%d小时%02d分钟", hour, minute);
    }

    public List<String> getAllRoleIds() {
        return gameTimeDao.getAllRoleId();
    }

    public List<GameTime> getAllTimes() {
        return gameTimeDao.getAllTime();
    }

    public List<GameTime> getTimesByRoleId(String roleId) {
        return gameTimeDao.getTimeListByRoleId(roleId);
    }

    public List<GameTime> getTimeListByData(String date) {
        return gameTimeDao.getTimeListByData(date);
    }

    public List<GameTime> getTimeListByDataAndRoleId(String date, String roleId) {
        return gameTimeDao.getTimeListByDataAndRoleId(date, roleId);
    }

    public int addTime(GameTime gameTime) {
        return gameTimeDao.addTime(gameTime);
    }

    public int updateTime(GameTime gameTime) {
        return gameTimeDao.updateTime(gameTime);
    }

    public int deleteTimeById(Integer id) {
        return gameTimeDao.deleteTimeById(id);
    }

    public boolean deleteTimeByData(String date) {
        return gameTimeDao.deleteTimeByData(date);
    }

    public boolean deleteTimeByDataAndRoleId(String date, String roleId) {
        return gameTimeDao.deleteTimeByDataAndRoleId(date, roleId);
    }
}
