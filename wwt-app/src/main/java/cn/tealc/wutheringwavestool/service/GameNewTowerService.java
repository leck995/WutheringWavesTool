package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.dao.GameNewTowerDao;
import cn.tealc.wutheringwavestool.model.tower.SlashDataForDB;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kuro.kujiequ.model.newTowerData.NewTowerData;
import com.kuro.kujiequ.model.newTowerData.NewTowerModeDetail;

import java.util.Calendar;
import java.util.List;
import java.util.Optional;

@Singleton
public class GameNewTowerService {

    @Inject
    private GameNewTowerDao gameNewTowerDao;
    @Inject
    private ObjectMapper objectMapper;

    @Inject
    public GameNewTowerService() {}

    public void saveToDB(NewTowerData newTowerData, String roleId) {
        try {
            List<NewTowerModeDetail> list = newTowerData.getModeDetails().stream()
                    .filter(d -> d.isHasRecord() && d.getScore() > 0)
                    .toList();
            if (list.isEmpty())
                return;

            String json = objectMapper.writeValueAsString(list);
            long date = convertToHourlyTimestamp(System.currentTimeMillis() + newTowerData.getEndTime());

            SlashDataForDB data = new SlashDataForDB();
            data.setRoleId(roleId);
            data.setData(json);
            data.setEndTime(date);

            gameNewTowerDao.add(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /** 获取指定角色的所有赛季结束时间列表，降序 */
    public List<Long> getEndTimesByRoleId(String roleId) {
        return gameNewTowerDao.getEndTimesByRoleId(roleId);
    }

    /** 获取指定角色和赛季结束时间的新深塔数据 */
    public Optional<SlashDataForDB> getByRoleIdAndEndTime(String roleId, long endTime) {
        return gameNewTowerDao.getByRoleIdAndEndTime(roleId, endTime);
    }

    private long convertToHourlyTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
