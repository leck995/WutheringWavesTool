package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.dao.GameSlashDataDao;
import cn.tealc.wutheringwavestool.model.tower.SlashDataForDB;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kuro.kujiequ.model.slash.Challenge;
import com.kuro.kujiequ.model.slash.SlashData;
import com.kuro.kujiequ.model.slash.SlashDifficulty;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

@Singleton
public class SlashDataService {

    @Inject
    private GameSlashDataDao gameSlashDataDao;
    @Inject
    private ObjectMapper objectMapper;

    @Inject
    public SlashDataService() {}

    /** 过滤并排序海墟难度列表：移除难度 0 和总分为 0 的记录，按难度降序 */
    public List<SlashDifficulty> filterAndSort(List<SlashDifficulty> difficulties) {
        return difficulties.stream()
                .filter(d -> d.getDifficulty() != 0 && d.getAllScore() > 0)
                .sorted(Comparator.comparingInt(SlashDifficulty::getDifficulty).reversed())
                .toList();
    }

    /** 将"无尽湍渊"（难度 2）的关卡合并到"再生海域"（难度 1）中 */
    public List<SlashDifficulty> mergeDifficulties(List<SlashDifficulty> sortedDifficulties) {
        if (sortedDifficulties.size() < 2) return sortedDifficulties;
        List<SlashDifficulty> result = new ArrayList<>(sortedDifficulties);
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).getDifficulty() == 1) {
                for (int j = 0; j < result.size(); j++) {
                    if (result.get(j).getDifficulty() == 2) {
                        List<Challenge> merged = new ArrayList<>(result.get(j).getChallengeList());
                        merged.addAll(result.get(i).getChallengeList());
                        result.get(i).setChallengeList(merged);
                        result.remove(j);
                        break;
                    }
                }
                break;
            }
        }
        return result;
    }

    /** 提取格式化后的总分/最高分文本 */
    public static String formatScore(int allScore, int maxScore) {
        return allScore + "/" + maxScore;
    }

    /** 将海墟数据保存到数据库（原 SlashDataDetailTask.saveToDB 上移） */
    public void saveToDB(SlashData slashData, String roleId) throws JsonProcessingException {
        // 过滤一次性的关卡数据,以及总分为0的记录
        List<SlashDifficulty> list = slashData.getDifficultyList().stream()
                .filter(d -> d.getDifficulty() != 0 && d.getAllScore() > 0)
                .toList();
        if (list.isEmpty())
            return;

        String json = objectMapper.writeValueAsString(list);
        long seasonEndTime = slashData.getSeasonEndTime();
        long date = convertToHourlyTimestamp(System.currentTimeMillis() + seasonEndTime);

        SlashDataForDB data = new SlashDataForDB();
        data.setRoleId(roleId);
        data.setData(json);
        data.setEndTime(date);

        gameSlashDataDao.add(data);
    }

    /** 将给定的时间戳转换成当天 4 点（原 SlashDataDetailTask.convertToHourlyTimestamp 上移） */
    public long convertToHourlyTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
