package cn.tealc.wutheringwavestool.ui.gacha;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.game.pool.CardInfo;
import de.saxsys.mvvmfx.ViewModel;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 抽卡统计 ViewModel：直接读取 pool.json 原始 CardInfo 记录，独立聚合统计。
 *
 * @author Leck
 */
public class CardStatViewModel implements ViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(CardStatViewModel.class);

    /** 角色筛选类型 */
    public enum FilterType { ALL, ROLE, WEAPON }

    private final SimpleBooleanProperty empty = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty loading = new SimpleBooleanProperty(true);

    // 筛选：默认选中角色
    private final SimpleBooleanProperty roleFilter = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty weaponFilter = new SimpleBooleanProperty(false);

    // 装饰头像路径（随机从 image/role 取一张）
    private final SimpleStringProperty[] roleImagePaths = new SimpleStringProperty[4];

    // —— 总体统计 ——
    private final SimpleStringProperty totalPullsText = new SimpleStringProperty("0");
    private final SimpleStringProperty totalStonesText = new SimpleStringProperty("");
    private final SimpleStringProperty ssrCountText = new SimpleStringProperty("0");
    private final SimpleStringProperty topSsrText = new SimpleStringProperty("");
    private final SimpleStringProperty srCountText = new SimpleStringProperty("0");
    private final SimpleStringProperty topSrText = new SimpleStringProperty("");
    private final SimpleStringProperty upSsrCountText = new SimpleStringProperty("0");
    private final SimpleStringProperty topUpSsrText = new SimpleStringProperty("");

    // 最多抽取的角色/武器信息（含 resourceId 供头像加载）
    private final SimpleStringProperty topSsrNameText = new SimpleStringProperty("—");
    private final SimpleIntegerProperty topSsrId = new SimpleIntegerProperty(0);
    private final SimpleStringProperty topSrNameText = new SimpleStringProperty("—");
    private final SimpleIntegerProperty topSrId = new SimpleIntegerProperty(0);
    private final SimpleStringProperty topUpSsrNameText = new SimpleStringProperty("—");
    private final SimpleIntegerProperty topUpSsrId = new SimpleIntegerProperty(0);
    private final SimpleStringProperty nonBannerRateText = new SimpleStringProperty("0%");
    private final SimpleStringProperty ssrAvgText = new SimpleStringProperty("0");
    private final SimpleStringProperty dateRangeText = new SimpleStringProperty("");

    // —— 角色 vs 武器 ——
    private final SimpleStringProperty rolePullsText = new SimpleStringProperty("0");
    private final SimpleStringProperty roleSsrText = new SimpleStringProperty("0");
    private final SimpleStringProperty roleAvgText = new SimpleStringProperty("0");
    private final SimpleStringProperty weaponPullsText = new SimpleStringProperty("0");
    private final SimpleStringProperty weaponSsrText = new SimpleStringProperty("0");
    private final SimpleStringProperty weaponAvgText = new SimpleStringProperty("0");

    // —— 右侧列表 ——
    private final ObservableList<StatItem> statItems = FXCollections.observableArrayList();

    // —— 饼图 ——
    private final ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

    // 缓存全量数据，供筛选使用
    private List<CardInfo> cachedAllCards;
    private Map<String, List<CardInfo>> cachedPoolMap;

    private static final Set<String> BASE_SSR_IDS = loadBaseSsrIds();

    public CardStatViewModel(boolean isEmpty) {
        empty.set(isEmpty);
        loading.set(!isEmpty);

        for (int i = 0; i < 4; i++) {
            roleImagePaths[i] = new SimpleStringProperty("");
        }
        pickRandomRoleImages();

        NotificationManager.subscribe(NotificationKey.CARD_POOL_USER_CHANGE, (s, objects) -> {
            String playerId = (String) objects[0];
            loadData(playerId);
        });

        NotificationManager.subscribe(NotificationKey.CARD_POOL_USER_EMPTY, (s, objects) -> {
            empty.set(true);
            loading.set(false);
            clearAll();
        });
    }

    /** 随机选 4 张 role 图片做装饰 */
    private void pickRandomRoleImages() {
        File dir = new File("wwt-app/src/main/resources/cn/tealc/wutheringwavestool/image/role");
        if (!dir.exists()) {
            dir = new File("src/main/resources/cn/tealc/wutheringwavestool/image/role");
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));
        if (files == null || files.length == 0) return;

        List<File> pool = new ArrayList<>(Arrays.asList(files));
        Collections.shuffle(pool);
        for (int i = 0; i < Math.min(4, pool.size()); i++) {
            String uri = pool.get(i).toURI().toString();
            roleImagePaths[i].set(uri);
        }
    }

    public void loadData(String playerId) {
        Thread.startVirtualThread(() -> {
            File poolJson = new File(String.format("data/%s/pool.json", playerId));
            if (!poolJson.exists()) {
                Platform.runLater(() -> {
                    empty.set(true);
                    loading.set(false);
                    clearAll();
                });
                return;
            }
            ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
            final Map<String, List<CardInfo>> poolData;
            try {
                poolData = mapper.readValue(poolJson, new TypeReference<>() {});
            } catch (IOException e) {
                LOG.error("加载抽卡数据失败", e);
                Platform.runLater(() -> {
                    clearAll();
                    empty.set(true);
                    loading.set(false);
                });
                return;
            }
            List<CardInfo> allCards = poolData.values().stream().flatMap(List::stream).toList();

            Platform.runLater(() -> {
                if (allCards.isEmpty()) {
                    empty.set(true);
                    loading.set(false);
                    clearAll();
                    return;
                }
                cachedAllCards = allCards;
                cachedPoolMap = poolData;
                update(allCards, poolData);
                applyFilter();
                loading.set(false);
                empty.set(false);
            });
        });
    }

    private void update(List<CardInfo> allCards, Map<String, List<CardInfo>> poolMap) {
        int totalPulls = allCards.stream().mapToInt(CardInfo::getCount).sum();
        List<CardInfo> ssrCards = allCards.stream().filter(c -> c.getQualityLevel() == 5).toList();
        List<CardInfo> srCards = allCards.stream().filter(c -> c.getQualityLevel() == 4).toList();
        int totalSsr = ssrCards.size();
        int totalSr = srCards.size();

        totalPullsText.set(String.valueOf(totalPulls));
        totalStonesText.set(String.format("≈%d", (long) totalPulls * 160));
        ssrCountText.set(String.valueOf(totalSsr));
        srCountText.set(String.valueOf(totalSr));

        double ssrAvg = totalSsr > 0 ? (double) totalPulls / totalSsr : 0;
        ssrAvgText.set(String.format("%.1f", ssrAvg));

        List<CardInfo> upSsrCards = ssrCards.stream()
                .filter(c -> !BASE_SSR_IDS.contains(String.valueOf(c.getResourceId())))
                .toList();
        upSsrCountText.set(String.valueOf(upSsrCards.size()));

        // 不歪率（50/50 胜率）：只统计非常驻角色池，按大小保底机制计算
        double nonBannerRate = calculateNonBannerRate(poolMap);
        nonBannerRateText.set(String.format("%.0f%%", nonBannerRate * 100));

        String startDate = allCards.stream().map(CardInfo::getTime).filter(Objects::nonNull).min(String::compareTo).orElse("");
        String endDate = allCards.stream().map(CardInfo::getTime).filter(Objects::nonNull).max(String::compareTo).orElse("");
        dateRangeText.set(startDate.isEmpty() || endDate.isEmpty() ? "" : startDate + " ~ " + endDate);

        topSsrText.set(findTop(ssrCards, topSsrNameText, topSsrId));
        topSrText.set(findTop(srCards, topSrNameText, topSrId));
        topUpSsrText.set(findTop(upSsrCards, topUpSsrNameText, topUpSsrId));

        // 角色 vs 武器
        List<CardInfo> roleCards = poolMap.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().startsWith("角色"))
                .flatMap(e -> e.getValue().stream()).toList();
        List<CardInfo> weaponCards = poolMap.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().startsWith("武器"))
                .flatMap(e -> e.getValue().stream()).toList();

        int rolePulls = roleCards.stream().mapToInt(CardInfo::getCount).sum();
        int roleSsr = (int) roleCards.stream().filter(c -> c.getQualityLevel() == 5).count();
        double roleAvg = roleSsr > 0 ? (double) rolePulls / roleSsr : 0;
        rolePullsText.set(String.valueOf(rolePulls));
        roleSsrText.set(String.valueOf(roleSsr));
        roleAvgText.set(String.format("%.1f", roleAvg));

        int weaponPulls = weaponCards.stream().mapToInt(CardInfo::getCount).sum();
        int weaponSsr = (int) weaponCards.stream().filter(c -> c.getQualityLevel() == 5).count();
        double weaponAvg = weaponSsr > 0 ? (double) weaponPulls / weaponSsr : 0;
        weaponPullsText.set(String.valueOf(weaponPulls));
        weaponSsrText.set(String.valueOf(weaponSsr));
        weaponAvgText.set(String.format("%.1f", weaponAvg));

        // 饼图
        int totalR = (int) allCards.stream().filter(c -> c.getQualityLevel() == 3).count();
        pieChartData.setAll(
                new PieChart.Data("五星", totalSsr),
                new PieChart.Data("四星", totalSr),
                new PieChart.Data("三星", totalR)
        );
    }

    /** 筛选右侧列表：只显示角色或武器 */
    public void applyFilter() {
        if (cachedAllCards == null) return;

        List<CardInfo> ssrCards = cachedAllCards.stream().filter(c -> c.getQualityLevel() == 5).toList();
        List<CardInfo> srCards = cachedAllCards.stream().filter(c -> c.getQualityLevel() == 4).toList();

        FilterType filter;
        if (roleFilter.get() && !weaponFilter.get()) filter = FilterType.ROLE;
        else if (weaponFilter.get() && !roleFilter.get()) filter = FilterType.WEAPON;
        else filter = FilterType.ALL;

        Map<String, StatAggregator> byName = new LinkedHashMap<>();
        for (CardInfo c : ssrCards) {
            if (filter == FilterType.ROLE && !"角色".equals(c.getResourceType())) continue;
            if (filter == FilterType.WEAPON && !"武器".equals(c.getResourceType())) continue;
            byName.computeIfAbsent(c.getName(), k -> new StatAggregator(c.getResourceId(), c.getQualityLevel()))
                    .add(c.getCount());
        }
        for (CardInfo c : srCards) {
            if (filter == FilterType.ROLE && !"角色".equals(c.getResourceType())) continue;
            if (filter == FilterType.WEAPON && !"武器".equals(c.getResourceType())) continue;
            byName.computeIfAbsent(c.getName(), k -> new StatAggregator(c.getResourceId(), c.getQualityLevel()))
                    .add(c.getCount());
        }

        statItems.setAll(byName.entrySet().stream()
                .map(e -> new StatItem(e.getKey(), e.getValue().totalCount,
                        e.getValue().resourceId, e.getValue().qualityLevel))
                .sorted(Comparator.comparingInt(StatItem::getCount).reversed())
                .toList());
    }

    /**
     * 找出出现次数最多的物品，设置名字和 resourceId 到对应属性。
     * @return 格式 "最多: xxx ×N" 供 topSsrText 等显示
     */
    private String findTop(List<CardInfo> cards, SimpleStringProperty nameProp, SimpleIntegerProperty idProp) {
        Map<String, Integer> countByName = new LinkedHashMap<>();
        Map<String, Integer> idByName = new LinkedHashMap<>();
        for (CardInfo c : cards) {
            countByName.merge(c.getName(), 1, Integer::sum);
            idByName.putIfAbsent(c.getName(), c.getResourceId());
        }
        var max = countByName.entrySet().stream()
                .max(Map.Entry.comparingByValue());
        if (max.isPresent()) {
            String name = max.get().getKey();
            int count = max.get().getValue();
            nameProp.set(name);
            idProp.set(idByName.getOrDefault(name, 0));
            return "最多: " + name + " ×" + count;
        }
        nameProp.set("—");
        idProp.set(0);
        return "—";
    }

    /**
     * 计算真实不歪率（50/50 胜率）：只统计非常驻角色池。
     * 按时间正序遍历每个非常驻角色池的五星列表，
     * 大保底出 UP 不计入 50/50 统计，小保底出 UP=赢了，出常驻=歪了且下次大保底。
     */
    private double calculateNonBannerRate(Map<String, List<CardInfo>> poolMap) {
        int totalFiftyFifty = 0;
        int wonFiftyFifty = 0;

        for (Map.Entry<String, List<CardInfo>> entry : poolMap.entrySet()) {
            String poolName = entry.getKey();
            // 只统计非常驻角色池（排除"角色常驻唤取"，且只看角色池）
            if (poolName == null || !poolName.startsWith("角色") || poolName.contains("常驻")) {
                continue;
            }

            List<CardInfo> cards = entry.getValue();
            if (cards == null || cards.isEmpty()) continue;

            // 找出五星记录的索引（按时间倒序排列，索引0=最新）
            List<CardInfo> ssrCards = new ArrayList<>();
            for (CardInfo c : cards) {
                if (c.getQualityLevel() == 5) {
                    ssrCards.add(c);
                }
            }
            if (ssrCards.isEmpty()) continue;

            // 按时间正序遍历（从最旧到最新，即列表末尾到开头）
            boolean isGuaranteed = false;
            for (int i = ssrCards.size() - 1; i >= 0; i--) {
                CardInfo card = ssrCards.get(i);
                boolean isUp = !BASE_SSR_IDS.contains(String.valueOf(card.getResourceId()));
                if (isGuaranteed) {
                    // 大保底：必出 UP，不计入 50/50 统计
                    isGuaranteed = false;
                } else {
                    // 小保底（50/50 情形）
                    totalFiftyFifty++;
                    if (isUp) {
                        wonFiftyFifty++;
                    } else {
                        // 歪了，下次进入大保底
                        isGuaranteed = true;
                    }
                }
            }
        }

        return totalFiftyFifty > 0 ? (double) wonFiftyFifty / totalFiftyFifty : 0;
    }

    private void clearAll() {
        totalPullsText.set("0");
        totalStonesText.set("");
        ssrCountText.set("0");
        topSsrText.set("");
        srCountText.set("0");
        topSrText.set("");
        upSsrCountText.set("0");
        topUpSsrText.set("");
        topSsrNameText.set("—"); topSsrId.set(0);
        topSrNameText.set("—"); topSrId.set(0);
        topUpSsrNameText.set("—"); topUpSsrId.set(0);
        nonBannerRateText.set("0%");
        ssrAvgText.set("0");
        dateRangeText.set("");
        rolePullsText.set("0"); roleSsrText.set("0"); roleAvgText.set("0");
        weaponPullsText.set("0"); weaponSsrText.set("0"); weaponAvgText.set("0");
        statItems.clear();
        pieChartData.clear();
    }

    private static Set<String> loadBaseSsrIds() {
        try {
            String ids = cn.tealc.wutheringwavestool.util.LanguageManager.getString("ui.analysis.base_role");
            if (ids != null && !ids.isBlank()) {
                return new HashSet<>(Arrays.asList(ids.split("#")));
            }
        } catch (Exception e) {
            LOG.warn("加载常驻五星ID列表失败", e);
        }
        return Set.of();
    }

    // ====== 筛选 toggle ======
    public void setRoleFilter(boolean selected) {
        roleFilter.set(selected);
        if (selected) weaponFilter.set(false); // 互斥
        applyFilter();
    }
    public void setWeaponFilter(boolean selected) {
        weaponFilter.set(selected);
        if (selected) roleFilter.set(false);
        applyFilter();
    }
    public SimpleBooleanProperty roleFilterProperty() { return roleFilter; }
    public SimpleBooleanProperty weaponFilterProperty() { return weaponFilter; }

    // ====== getters ======
    public boolean isEmpty() { return empty.get(); }
    public SimpleBooleanProperty emptyProperty() { return empty; }
    public boolean isLoading() { return loading.get(); }
    public SimpleBooleanProperty loadingProperty() { return loading; }

    public SimpleStringProperty totalPullsTextProperty() { return totalPullsText; }
    public SimpleStringProperty totalStonesTextProperty() { return totalStonesText; }
    public SimpleStringProperty ssrCountTextProperty() { return ssrCountText; }
    public SimpleStringProperty topSsrTextProperty() { return topSsrText; }
    public SimpleStringProperty srCountTextProperty() { return srCountText; }
    public SimpleStringProperty topSrTextProperty() { return topSrText; }
    public SimpleStringProperty upSsrCountTextProperty() { return upSsrCountText; }
    public SimpleStringProperty topUpSsrTextProperty() { return topUpSsrText; }
    public SimpleStringProperty topSsrNameTextProperty() { return topSsrNameText; }
    public SimpleIntegerProperty topSsrIdProperty() { return topSsrId; }
    public SimpleStringProperty topSrNameTextProperty() { return topSrNameText; }
    public SimpleIntegerProperty topSrIdProperty() { return topSrId; }
    public SimpleStringProperty topUpSsrNameTextProperty() { return topUpSsrNameText; }
    public SimpleIntegerProperty topUpSsrIdProperty() { return topUpSsrId; }
    public SimpleStringProperty nonBannerRateTextProperty() { return nonBannerRateText; }
    public SimpleStringProperty ssrAvgTextProperty() { return ssrAvgText; }
    public SimpleStringProperty dateRangeTextProperty() { return dateRangeText; }
    public SimpleStringProperty rolePullsTextProperty() { return rolePullsText; }
    public SimpleStringProperty roleSsrTextProperty() { return roleSsrText; }
    public SimpleStringProperty roleAvgTextProperty() { return roleAvgText; }
    public SimpleStringProperty weaponPullsTextProperty() { return weaponPullsText; }
    public SimpleStringProperty weaponSsrTextProperty() { return weaponSsrText; }
    public SimpleStringProperty weaponAvgTextProperty() { return weaponAvgText; }
    public SimpleStringProperty[] getRoleImagePaths() { return roleImagePaths; }
    public ObservableList<StatItem> getStatItems() { return statItems; }
    public ObservableList<PieChart.Data> getPieChartData() { return pieChartData; }

    public static class StatItem {
        private final String name;
        private final int count;
        private final int resourceId;
        private final int qualityLevel;
        public StatItem(String name, int count, int resourceId, int qualityLevel) {
            this.name = name;
            this.count = count;
            this.resourceId = resourceId;
            this.qualityLevel = qualityLevel;
        }
        public String getName() { return name; }
        public int getCount() { return count; }
        public int getResourceId() { return resourceId; }
        public int getQualityLevel() { return qualityLevel; }
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
