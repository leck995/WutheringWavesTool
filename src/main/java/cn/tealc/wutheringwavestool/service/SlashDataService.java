package cn.tealc.wutheringwavestool.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kuro.kujiequ.model.slash.Challenge;
import com.kuro.kujiequ.model.slash.SlashDifficulty;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Singleton
public class SlashDataService {

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
}
