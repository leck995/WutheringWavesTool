package cn.tealc.wutheringwavestool.ui.gacha;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.gacha.GachaStatResult;
import cn.tealc.wutheringwavestool.model.gacha.StatItem;
import cn.tealc.wutheringwavestool.model.game.pool.CardInfo;
import cn.tealc.wutheringwavestool.service.GachaStatService;
import de.saxsys.mvvmfx.ViewModel;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;

import java.io.File;
import java.util.*;

/**
 * 抽卡统计 ViewModel：只管 UI 状态和筛选，分析逻辑委托给 {@link GachaStatService}。
 *
 * @author Leck
 */
public class CardStatViewModel implements ViewModel {

    private final SimpleBooleanProperty empty = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty loading = new SimpleBooleanProperty(true);

    // 筛选状态
    private final SimpleBooleanProperty roleFilter = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty weaponFilter = new SimpleBooleanProperty(false);
    private int rarityLevel = 0; // 0=全部, 5=五星, 4=四星, 3=三星
    private final ObservableList<String> rarityOptions = FXCollections.observableArrayList("全部", "五星", "四星", "三星");

    // 装饰头像
    private final SimpleStringProperty[] roleImagePaths = new SimpleStringProperty[4];

    // —— UI 属性 ——
    private final SimpleStringProperty totalPullsText = new SimpleStringProperty("0");
    private final SimpleStringProperty totalStonesText = new SimpleStringProperty("");
    private final SimpleStringProperty ssrCountText = new SimpleStringProperty("0");
    private final SimpleStringProperty topSsrText = new SimpleStringProperty("");
    private final SimpleStringProperty srCountText = new SimpleStringProperty("0");
    private final SimpleStringProperty topSrText = new SimpleStringProperty("");
    private final SimpleStringProperty upSsrCountText = new SimpleStringProperty("0");
    private final SimpleStringProperty topUpSsrText = new SimpleStringProperty("");
    private final SimpleStringProperty topSsrNameText = new SimpleStringProperty("—");
    private final SimpleIntegerProperty topSsrId = new SimpleIntegerProperty(0);
    private final SimpleStringProperty topSrNameText = new SimpleStringProperty("—");
    private final SimpleIntegerProperty topSrId = new SimpleIntegerProperty(0);
    private final SimpleStringProperty topUpSsrNameText = new SimpleStringProperty("—");
    private final SimpleIntegerProperty topUpSsrId = new SimpleIntegerProperty(0);
    private final SimpleStringProperty nonBannerRateText = new SimpleStringProperty("0%");
    private final SimpleStringProperty ssrAvgText = new SimpleStringProperty("0");
    private final SimpleStringProperty dateRangeText = new SimpleStringProperty("");
    private final SimpleStringProperty rolePullsText = new SimpleStringProperty("0");
    private final SimpleStringProperty roleSsrText = new SimpleStringProperty("0");
    private final SimpleStringProperty roleAvgText = new SimpleStringProperty("0");
    private final SimpleStringProperty weaponPullsText = new SimpleStringProperty("0");
    private final SimpleStringProperty weaponSsrText = new SimpleStringProperty("0");
    private final SimpleStringProperty weaponAvgText = new SimpleStringProperty("0");

    // —— 列表 / 饼图 ——
    private final ObservableList<StatItem> statItems = FXCollections.observableArrayList();
    private final ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

    // service 由外部注入
    private GachaStatService statService;

    // 缓存原始数据供筛选使用
    private List<CardInfo> cachedAllCards;
    private Map<String, List<CardInfo>> cachedPoolMap;

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

    public void setStatService(GachaStatService service) {
        this.statService = service;
    }

    // ==================== 数据加载 ====================

    public void loadData(String playerId) {
        if (statService == null) return;
        Thread.startVirtualThread(() -> {
            Map<String, List<CardInfo>> poolData = statService.loadPoolData(playerId);
            if (poolData == null || poolData.isEmpty()) {
                Platform.runLater(() -> {
                    empty.set(true);
                    loading.set(false);
                    clearAll();
                });
                return;
            }
            List<CardInfo> allCards = poolData.values().stream().flatMap(List::stream).toList();
            if (allCards.isEmpty()) {
                Platform.runLater(() -> {
                    empty.set(true);
                    loading.set(false);
                    clearAll();
                });
                return;
            }

            GachaStatResult result = statService.aggregate(allCards, poolData);

            Platform.runLater(() -> {
                cachedAllCards = allCards;
                cachedPoolMap = poolData;
                applyResult(result);
                applyFilter();
                loading.set(false);
                empty.set(false);
            });
        });
    }

    /** 将 service 返回的结果填入 UI 属性 */
    private void applyResult(GachaStatResult r) {
        totalPullsText.set(String.valueOf(r.totalPulls));
        totalStonesText.set(String.format("≈%d", r.totalStones));
        ssrCountText.set(String.valueOf(r.ssrCount));
        srCountText.set(String.valueOf(r.srCount));
        upSsrCountText.set(String.valueOf(r.upSsrCount));
        ssrAvgText.set(String.format("%.1f", r.ssrAvg));
        nonBannerRateText.set(String.format("%.0f%%", r.nonBannerRate * 100));
        dateRangeText.set(r.startDate.isEmpty() || r.endDate.isEmpty() ? "" : r.startDate + " ~ " + r.endDate);

        topSsrNameText.set(r.topSsrName); topSsrId.set(r.topSsrId);
        topSsrText.set(r.topSsrCount > 0 ? "最多: " + r.topSsrName + " ×" + r.topSsrCount : "—");
        topSrNameText.set(r.topSrName); topSrId.set(r.topSrId);
        topSrText.set(r.topSrCount > 0 ? "最多: " + r.topSrName + " ×" + r.topSrCount : "—");
        topUpSsrNameText.set(r.topUpSsrName); topUpSsrId.set(r.topUpSsrId);
        topUpSsrText.set(r.topUpSsrCount > 0 ? "最多: " + r.topUpSsrName + " ×" + r.topUpSsrCount : "—");

        rolePullsText.set(String.valueOf(r.rolePulls));
        roleSsrText.set(String.valueOf(r.roleSsr));
        roleAvgText.set(String.format("%.1f", r.roleAvg));
        weaponPullsText.set(String.valueOf(r.weaponPulls));
        weaponSsrText.set(String.valueOf(r.weaponSsr));
        weaponAvgText.set(String.format("%.1f", r.weaponAvg));

        pieChartData.setAll(
                new PieChart.Data("五星", r.ssrCount),
                new PieChart.Data("四星", r.srCount),
                new PieChart.Data("三星", r.rCount)
        );
    }

    // ==================== 筛选 ====================

    public void applyFilter() {
        if (cachedAllCards == null || statService == null) return;
        // 筛选在后台线程执行，避免大数据量时卡 UI
        final boolean roleOnly = roleFilter.get();
        final boolean weaponOnly = weaponFilter.get();
        final int level = rarityLevel;
        Thread.startVirtualThread(() -> {
            List<StatItem> items = statService.filterItems(cachedAllCards, roleOnly, weaponOnly, level);
            Platform.runLater(() -> statItems.setAll(items));
        });
    }

    public void setRoleFilter(boolean selected) {
        roleFilter.set(selected);
        if (selected) weaponFilter.set(false);
        applyFilter();
    }
    public void setWeaponFilter(boolean selected) {
        weaponFilter.set(selected);
        if (selected) roleFilter.set(false);
        applyFilter();
    }
    public void setRarityFilter(int index) {
        this.rarityLevel = switch (index) {
            case 1 -> 5;
            case 2 -> 4;
            case 3 -> 3;
            default -> 0;
        };
        applyFilter();
    }

    // ==================== 辅助 ====================

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
            roleImagePaths[i].set(pool.get(i).toURI().toString());
        }
    }

    private void clearAll() {
        totalPullsText.set("0"); totalStonesText.set("");
        ssrCountText.set("0"); topSsrText.set("");
        srCountText.set("0"); topSrText.set("");
        upSsrCountText.set("0"); topUpSsrText.set("");
        topSsrNameText.set("—"); topSsrId.set(0);
        topSrNameText.set("—"); topSrId.set(0);
        topUpSsrNameText.set("—"); topUpSsrId.set(0);
        nonBannerRateText.set("0%"); ssrAvgText.set("0"); dateRangeText.set("");
        rolePullsText.set("0"); roleSsrText.set("0"); roleAvgText.set("0");
        weaponPullsText.set("0"); weaponSsrText.set("0"); weaponAvgText.set("0");
        statItems.clear(); pieChartData.clear();
    }

    // ==================== getters ====================

    public SimpleBooleanProperty emptyProperty() { return empty; }
    public SimpleBooleanProperty loadingProperty() { return loading; }
    public SimpleBooleanProperty roleFilterProperty() { return roleFilter; }
    public SimpleBooleanProperty weaponFilterProperty() { return weaponFilter; }
    public ObservableList<String> getRarityOptions() { return rarityOptions; }
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
}
