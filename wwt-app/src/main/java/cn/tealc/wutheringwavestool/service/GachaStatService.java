package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.gacha.GachaStatResult;
import cn.tealc.wutheringwavestool.model.gacha.StatItem;
import cn.tealc.wutheringwavestool.model.game.pool.CardInfo;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 抽卡统计 Service：读取 pool.json，聚合统计，计算不歪率，筛选列表。
 * 无 JavaFX 依赖，可独立测试。
 *
 * @author Leck
 */
@Singleton
public class GachaStatService {
    private static final Logger LOG = LoggerFactory.getLogger(GachaStatService.class);

    private final ObjectMapper objectMapper;

    private static final Set<String> BASE_SSR_IDS = loadBaseSsrIds();

    @Inject
    public GachaStatService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 加载 pool.json 并返回原始数据。
     * @return Map<卡池名, List<CardInfo>>，null 表示文件不存在
     */
    public Map<String, List<CardInfo>> loadPoolData(String playerId) {
        File poolJson = new File(String.format("data/%s/pool.json", playerId));
        if (!poolJson.exists()) {
            return null;
        }
        try {
            return objectMapper.readValue(poolJson, new TypeReference<>() {});
        } catch (IOException e) {
            LOG.error("加载抽卡数据失败", e);
            return null;
        }
    }

    /**
     * 聚合全部统计数据。
     */
    public GachaStatResult aggregate(List<CardInfo> allCards, Map<String, List<CardInfo>> poolMap) {
        GachaStatResult r = new GachaStatResult();

        // 总体统计
        r.totalPulls = allCards.stream().mapToInt(CardInfo::getCount).sum();
        r.totalStones = (long) r.totalPulls * 160;

        List<CardInfo> ssrCards = allCards.stream().filter(c -> c.getQualityLevel() == 5).toList();
        List<CardInfo> srCards = allCards.stream().filter(c -> c.getQualityLevel() == 4).toList();
        List<CardInfo> rCards = allCards.stream().filter(c -> c.getQualityLevel() == 3).toList();

        r.ssrCount = ssrCards.size();
        r.srCount = srCards.size();
        r.rCount = rCards.size();
        r.ssrAvg = r.ssrCount > 0 ? (double) r.totalPulls / r.ssrCount : 0;

        // UP 五星
        List<CardInfo> upSsrCards = ssrCards.stream()
                .filter(c -> !BASE_SSR_IDS.contains(String.valueOf(c.getResourceId())))
                .toList();
        r.upSsrCount = upSsrCards.size();

        // 不歪率
        r.nonBannerRate = calculateNonBannerRate(poolMap);

        // 时间跨度
        r.startDate = allCards.stream().map(CardInfo::getTime).filter(Objects::nonNull).min(String::compareTo).orElse("");
        r.endDate = allCards.stream().map(CardInfo::getTime).filter(Objects::nonNull).max(String::compareTo).orElse("");

        // 最多抽取
        fillTop(ssrCards, r, true, false);
        fillTop(srCards, r, false, false);
        fillTop(upSsrCards, r, true, true);

        // 角色 vs 武器
        List<CardInfo> roleCards = poolMap.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().startsWith("角色"))
                .flatMap(e -> e.getValue().stream()).toList();
        List<CardInfo> weaponCards = poolMap.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().startsWith("武器"))
                .flatMap(e -> e.getValue().stream()).toList();

        r.rolePulls = roleCards.stream().mapToInt(CardInfo::getCount).sum();
        r.roleSsr = (int) roleCards.stream().filter(c -> c.getQualityLevel() == 5).count();
        r.roleAvg = r.roleSsr > 0 ? (double) r.rolePulls / r.roleSsr : 0;

        r.weaponPulls = weaponCards.stream().mapToInt(CardInfo::getCount).sum();
        r.weaponSsr = (int) weaponCards.stream().filter(c -> c.getQualityLevel() == 5).count();
        r.weaponAvg = r.weaponSsr > 0 ? (double) r.weaponPulls / r.weaponSsr : 0;

        return r;
    }

    /**
     * 筛选排行列表：按角色/武器 + 品质过滤，按 name 分组求和。
     */
    public List<StatItem> filterItems(List<CardInfo> allCards,
                                       boolean roleOnly, boolean weaponOnly,
                                       int rarityLevel) {
        Map<String, StatAggregator> byName = new LinkedHashMap<>();
        for (CardInfo c : allCards) {
            if (rarityLevel > 0 && c.getQualityLevel() != rarityLevel) continue;
            if (roleOnly && !"角色".equals(c.getResourceType())) continue;
            if (weaponOnly && !"武器".equals(c.getResourceType())) continue;
            byName.computeIfAbsent(c.getName(), k -> new StatAggregator(c.getResourceId(), c.getQualityLevel()))
                    .add(c.getCount());
        }

        return byName.entrySet().stream()
                .map(e -> new StatItem(e.getKey(), e.getValue().totalCount,
                        e.getValue().resourceId, e.getValue().qualityLevel))
                .sorted(Comparator.comparingInt(StatItem::getCount).reversed())
                .toList();
    }

    // ==================== 内部方法 ====================

    /**
     * 找出出现次数最多的物品，设置到 result 的 topXxx 字段。
     */
    private void fillTop(List<CardInfo> cards, GachaStatResult r, boolean isSsr, boolean isUp) {
        Map<String, Integer> countByName = new LinkedHashMap<>();
        Map<String, Integer> idByName = new LinkedHashMap<>();
        for (CardInfo c : cards) {
            countByName.merge(c.getName(), 1, Integer::sum);
            idByName.putIfAbsent(c.getName(), c.getResourceId());
        }
        var max = countByName.entrySet().stream().max(Map.Entry.comparingByValue());
        if (max.isPresent()) {
            String name = max.get().getKey();
            int count = max.get().getValue();
            int id = idByName.getOrDefault(name, 0);
            if (isUp) {
                r.topUpSsrName = name; r.topUpSsrId = id; r.topUpSsrCount = count;
            } else if (isSsr) {
                r.topSsrName = name; r.topSsrId = id; r.topSsrCount = count;
            } else {
                r.topSrName = name; r.topSrId = id; r.topSrCount = count;
            }
        }
    }

    /**
     * 计算真实不歪率（50/50 胜率）：只统计非常驻角色池，按大小保底机制计算。
     */
    private double calculateNonBannerRate(Map<String, List<CardInfo>> poolMap) {
        int totalFiftyFifty = 0;
        int wonFiftyFifty = 0;

        for (Map.Entry<String, List<CardInfo>> entry : poolMap.entrySet()) {
            String poolName = entry.getKey();
            if (poolName == null || !poolName.startsWith("角色") || poolName.contains("常驻")) continue;

            List<CardInfo> cards = entry.getValue();
            if (cards == null || cards.isEmpty()) continue;

            List<CardInfo> ssrCards = new ArrayList<>();
            for (CardInfo c : cards) {
                if (c.getQualityLevel() == 5) ssrCards.add(c);
            }
            if (ssrCards.isEmpty()) continue;

            boolean isGuaranteed = false;
            for (int i = ssrCards.size() - 1; i >= 0; i--) {
                CardInfo card = ssrCards.get(i);
                boolean isUp = !BASE_SSR_IDS.contains(String.valueOf(card.getResourceId()));
                if (isGuaranteed) {
                    isGuaranteed = false;
                } else {
                    totalFiftyFifty++;
                    if (isUp) {
                        wonFiftyFifty++;
                    } else {
                        isGuaranteed = true;
                    }
                }
            }
        }
        return totalFiftyFifty > 0 ? (double) wonFiftyFifty / totalFiftyFifty : 0;
    }

    private static Set<String> loadBaseSsrIds() {
        try {
            String ids = LanguageManager.getString("ui.analysis.base_role");
            if (ids != null && !ids.isBlank()) {
                return new HashSet<>(Arrays.asList(ids.split("#")));
            }
        } catch (Exception e) {
            LOG.warn("加载常驻五星ID列表失败", e);
        }
        return Set.of();
    }

    private static class StatAggregator {
        final int resourceId;
        final int qualityLevel;
        int totalCount = 0;
        StatAggregator(int resourceId, int qualityLevel) {
            this.resourceId = resourceId;
            this.qualityLevel = qualityLevel;
        }
        void add(int count) { totalCount += count; }
    }
}
