# 方案：抽卡分析新增"统计"子界面

## 布局

```
┌──────────────────────────────────────────────────────────────┬──────────────────┐
│  [总体统计卡片]                                               │                  │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┐    │  角色/武器抽取    │
│  │总抽数     │五星数     │四星数     │UP五星     │五星不歪率  │    │  数量列表         │
│  │ 1234     │ 15       │ 120      │ 10       │ 67%      │    │                  │
│  │≈197440石 │最多:XXX  │最多:XXX  │最多:XXX  │          │    │  名称      抽数   │
│  └──────────┴──────────┴──────────┴──────────┴──────────┘    │  ───────────────  │
│  ┌──────────┬──────────┐                                    │  安可      12     │
│  │五星平均  │抽卡时间   │                                    │  卡列宁    8      │
│  │ 82.3抽  │ 跨度      │                                    │  长离      6      │
│  └──────────┴──────────┘                                    │  ...             │
│                                                              │                  │
│  [角色 vs 武器对比卡片]                                      │  (仅SSR+SR)       │
│  ┌─────────────────────┬─────────────────────┐              │                  │
│  │ 角色池              │ 武器池              │              │  ── 列表项样式 ── │
│  │ 总抽数: 800         │ 总抽数: 434         │              │  名称左 | 抽数右  │
│  │ 五星: 12            │ 五星: 3             │              │  ───────────────  │
│  │ 平均: 66.7          │ 平均: 144.7         │              │                  │
│  └─────────────────────┴─────────────────────┘              │                  │
│                                                              │                  │
│  [空状态/加载状态]                                           │                  │
└──────────────────────────────────────────────────────────────┴──────────────────┘
```

- 左侧（HBox，`Priority.ALWAYS`）：VBox 纵向排总体统计 + 角色 vs 武器对比
- 右侧（固定宽度 ~280px）：角色/武器抽取数量列表，`ListView` 垂直滚动

## 数据聚合逻辑（CardStatViewModel）

### 总体统计

```java
// 基础求和
int totalPulls = data.stream().mapToInt(AnalysisData::getTotalCount).sum();
int totalSsr   = data.stream().mapToInt(AnalysisData::getSsrCount).sum();
int totalSr    = data.stream().mapToInt(AnalysisData::getSrCount).sum();
int totalUpSsr = data.stream().mapToInt(AnalysisData::getUpSsrCount).sum();

// 派生
long stones = (long) totalPulls * 160;  // 相当于 xxx 石头
double ssrAvgWeighted = data.stream()
    .mapToDouble(d -> d.getSsrAvg() * d.getTotalCount()).sum() / totalPulls;
double nonBannerRate = data.stream()
    .filter(d -> d.getSsrCount() > 0)
    .mapToDouble(d -> d.getNonBannerRate() * d.getSsrCount()).sum() / totalSsr;

// 时间跨度
String startDate = data.stream().map(AnalysisData::getStartDate).min(String::compareTo).orElse("");
String endDate = data.stream().map(AnalysisData::getEndDate).max(String::compareTo).orElse("");
```

### "最多抽取的角色/武器"

把所有池的 `ssrDataList`/`srDataList` flatMap 拼接，按 `name` 分组对 `count` 求和，取 count 最大的：

```java
// 五星最多
Map<String, Integer> ssrByName = data.stream()
    .flatMap(d -> d.getSsrDataList().stream())
    .collect(Collectors.groupingBy(SsrData::getName, Collectors.summingInt(SsrData::getCount)));
String topSsr = ssrByName.entrySet().stream()
    .max(Map.Entry.comparingByValue())
    .map(e -> e.getKey() + " ×" + e.getValue()).orElse("—");

// 四星最多（同理用 srDataList）
// UP五星最多：在 event==true 的五星里分组
Map<String, Integer> upSsrByName = data.stream()
    .flatMap(d -> d.getSsrDataList().stream())
    .filter(SsrData::isEvent)
    .collect(Collectors.groupingBy(SsrData::getName, Collectors.summingInt(SsrData::getCount)));
String topUpSsr = ...;
```

### 角色/武器抽取数量列表（右侧 ListView）

只含 SSR + SR，把所有池的 `ssrDataList` + `srDataList` flatMap，按 `name` 分组对 `count` 求和，按总抽数降序排列，每项显示 `名称 + 抽取次数`：

```java
ObservableList<StatItem> statItems = FXCollections.observableArrayList();

// 合并 SSR + SR，按 name 分组求和
Map<String, Integer> allByName = new LinkedHashMap<>();
data.stream().flatMap(d -> d.getSsrDataList().stream())
    .forEach(s -> allByName.merge(s.getName(), s.getCount(), Integer::sum));
data.stream().flatMap(d -> d.getSrDataList().stream())
    .forEach(s -> allByName.merge(s.getName(), s.getCount(), Integer::sum));

// 转为列表项并按抽数降序
statItems.setAll(allByName.entrySet().stream()
    .map(e -> new StatItem(e.getKey(), e.getValue()))
    .sorted(Comparator.comparingInt(StatItem::getCount).reversed())
    .toList());
```

`StatItem` 是 ViewModel 内部 record/class：`name`、`count`。

### 角色 vs 武器对比

```java
int rolePulls = data.stream().filter(d -> d.getPoolName().startsWith("角色"))
    .mapToInt(AnalysisData::getTotalCount).sum();
int weaponPulls = data.stream().filter(d -> d.getPoolName().startsWith("武器"))
    .mapToInt(AnalysisData::getTotalCount).sum();
int roleSsr = data.stream().filter(d -> d.getPoolName().startsWith("角色"))
    .mapToInt(AnalysisData::getSsrCount).sum();
int weaponSsr = data.stream().filter(d -> d.getPoolName().startsWith("武器"))
    .mapToInt(AnalysisData::getSsrCount).sum();
// 平均同理加权
```

## 展示属性（CardStatViewModel）

SimpleStringProperty 绑定到 FXML Label：
- `totalPullsText`（"1234"）
- `totalStonesText`（"≈197440"）
- `ssrCountText`（"15"）
- `topSsrText`（"最多: 安可 ×12"）
- `srCountText`（"120"）
- `topSrText`（"最多: XXX ×N"）
- `upSsrCountText`（"10"）
- `topUpSsrText`（"最多: XXX ×N"）
- `nonBannerRateText`（"67%"）
- `ssrAvgText`（"82.3抽"）
- `dateRangeText`（"2024-01-01 ~ 2025-08-12"）
- `rolePullsText` / `roleSsrText` / `roleAvgText`
- `weaponPullsText` / `weaponSsrText` / `weaponAvgText`

ObservableList<StatItem> `statItems` 绑定到右侧 ListView。

`SimpleBooleanProperty empty` / `loading` 控制空状态/加载状态显示。

## 接入方式（复用现有模式）

### CardAnalysisBaseView.fxml 加 ToggleButton

在"表格"ToggleButton 后加：
```fxml
<ToggleButton contentDisplay="BOTTOM" mnemonicParsing="false"
    onAction="#toStatChild" styleClass="child-select" text="统计"
    toggleGroup="$childSelectedToggle">
   <graphic><Pane prefHeight="200.0" prefWidth="200.0" /></graphic>
</ToggleButton>
```

### CardAnalysisBaseView.java 加 toStatChild

仿 `toTableChild` 懒加载+缓存模式，切换时补发 `CARD_POOL_USER_UPDATE`（和 toDetailChild 一样）。

## 新建文件清单

| 操作 | 文件 |
|---|---|
| 修改 | `CardAnalysisBaseView.fxml`（加统计 ToggleButton） |
| 修改 | `CardAnalysisBaseView.java`（加 statChild 字段 + toStatChild 方法） |
| 新建 | `ui/gacha/CardStatView.java`（控制器，绑定 ViewModel 属性到 FXML） |
| 新建 | `ui/gacha/CardStatViewModel.java`（聚合逻辑 + 展示属性 + StatItem 内部类） |
| 新建 | `resources/.../ui/gacha/CardStatView.fxml`（卡片式布局 + 右侧 ListView） |
| 修改 | `resources/.../css/gacha/CardPool.css`（加 .stat-card 样式） |

## 不在范围内
- 各卡池明细表格（已移除）
- 图表/柱状图（纯数字+列表展示）
- 三星数据（AnalysisData 的 rDataList 当前未被 Task 填充）