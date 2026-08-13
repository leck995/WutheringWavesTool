# TowerView 界面改版方案

## 背景约束（必须遵守）

- 本页在导航里 `showBg: false`，底层已是 **MainView 高斯模糊壁纸 + 亚克力层**。
- 根节点必须保持**透明**，不要铺实色整页背景，否则会挡住模糊图。
- 视觉语言对齐项目内已有做法：
  - 布局骨架 → **NewTowerView**（标题行 + 摘要条 + 滚动卡片区）
  - 卡片质感 → **ResourceBriefing**（圆角 16、细边框、轻阴影、半透明玻璃）
  - 头像/命座徽章 → 现有 `chain-unlock-num` / NewTower 的 `role-item`

---

## 现状问题（为何不好看）

| 问题 | 表现 |
|------|------|
| Anchor 硬布局 | 标题/倒计时绝对定位，左右列宽不一致（150 vs 210） |
| 区域卡片简陋 | 固定 400×160、无边框阴影、高度被压扁 |
| 星星只画已获得 | 未满星时列宽不齐、信息感弱 |
| 区域星数未展示 | `TowerArea.star/maxStar` 有数据却没用 |
| 无摘要信息 | 切换难度后缺少总览 |
| 历史区突兀 | 仅难度 3 显示，侧栏空荡 |
| 样式偏旧 | `#ffffffxx` 本地覆盖、radius 8、无 hover |

---

## 目标视觉结构

```
StackPane.tower（透明，透出 Main 模糊背景）
└── AnchorPane
    ├── 左侧栏 ~200px（透明）
    │   ├── ListView 难度（玻璃 hover cell + 星数）
    │   └── 往期历史（仅深境区显示，标题+列表）
    └── 右侧内容区
        ├── Header：title-2  |  Spacer  |  end-time 胶囊
        ├── SummaryBar（玻璃条）：总星数  star/max  ·  区域进度
        └── ScrollPane → FlowPane 区域卡片
              └── AreaCard × N
                    ├── 标题行：区域名 + 区域星数
                    ├── 渐变 Separator
                    └── FloorRow × N
                          [第N层]  [★☆☆]  [头像圈×角色 / 暂无数据]
```

**ASCII 示意（区域卡片）：**

```
┌─────────────────────────────────────────────┐
│  共鸣之塔                          9 / 12 ★ │
│  ─────────────────────────────────────────  │
│  第1层   ★ ★ ★     (角色头像)(头像)(头像)   │
│  第2层   ★ ★ ☆     (角色头像)(头像)         │
│  第3层   ★ ☆ ☆     暂无数据                 │
│  第4层   ☆ ☆ ☆     暂无数据                 │
└─────────────────────────────────────────────┘
```

---

## 改动范围

**只改经典 `TowerView`（逆境深塔）**，不碰 Slash / NewTower 业务逻辑；CSS 在共享 `Tower.css` 中**增量**增强，避免破坏 NewTower。

| 文件 | 动作 |
|------|------|
| `TowerView.fxml` | 重构右侧为 NewTower 式 VBox 结构 |
| `TowerView.java` | 重写 `AreaCell`、DifficultyCell 微调、绑定摘要 |
| `TowerViewModel.java` | 少量：增加当前难度总星摘要属性（可选计算放 View） |
| `Tower.css` | 升级 `.area`、新增 `.floor-row` / 空星样式 / 左侧栏统一宽度 |

---

## 详细设计

### 1. FXML 布局（对齐 NewTower）

把现在的：

- 绝对定位 `title` + 绝对定位 `seasonEndTimeLabel` + 单独 `ScrollPane`

改成右侧：

```xml
<StackPane AnchorPane.leftAnchor="210" ...>
  <VBox spacing="12">
    <HBox> <!-- header -->
      <Label fx:id="title" styleClass="title-2"/>
      <Spacer/>
      <Label fx:id="seasonEndTimeLabel" styleClass="end-time"/>
    </HBox>
    <HBox fx:id="summaryBox" styleClass="summary-bar" spacing="24">
      <Label fx:id="totalStarLabel" styleClass="summary-score"/>
      <Label fx:id="areaProgressLabel" styleClass="summary-progress"/>
    </HBox>
    <ScrollPane styleClass="right" VBox.vgrow="ALWAYS">
      <FlowPane fx:id="areaFlowPane" hgap="24" vgap="24"/>
    </ScrollPane>
  </VBox>
</StackPane>
```

左侧：

- 统一 `prefWidth="200"`
- 历史区块用 `styleClass="history-section"`，标题加 `text-muted`
- 难度列表与历史列表间距更紧凑（`spacing="8"`）

### 2. 区域卡片 AreaCell（核心美观点）

结构对齐 NewTower + ResourceBriefing：

```
VBox.area
├── HBox.area-header
│   ├── Label.area-title（区域名）
│   ├── Spacer
│   └── Label.area-score（"star / maxStar" + 星图标）
├── Separator（沿用渐变线）
└── VBox.floor-list
    └── HBox.floor-row × N
```

**具体规则：**

1. **去掉固定 prefHeight(160)**，改为 `prefWidth(380~420)` + 高度随楼层自适应。
2. **区域星数**：用 `towerArea.getStar()` / `getMaxStar()` 显示在右上角（accent 色）。
3. **楼层星**：每层固定 **3 格**（鸣潮深塔惯例）：
   - 已获得：`star01.png`（可略缩到 22–24px，更精致）
   - 未获得：半透明空星（同一图 + `opacity 0.25`，或 CSS 类 `.star-empty`）
4. **楼层行** 使用 `floor-row`：
   - 左：`第 N 层`（固定宽 ~56）
   - 中：星星 HBox（固定宽，保证对齐）
   - 右：角色头像行（`role-row`）
5. **角色头像**：
   - 圆形 clip 保持
   - `role-item` + CSS 控制命座徽章位移（去掉 Java 里 `translateX/Y`）
   - 无角色：`Label` + `Styles.TEXT_MUTED`「暂无数据」
6. **满星区域**：`area-score` 可加 `full-star` 金色强调（与左侧难度满星一致）。

### 3. 摘要条 SummaryBar

切换难度 / 历史后根据当前 `towerAreaList` 计算：

- **总星数**：`sum(area.star) / sum(area.maxStar)`
- **区域进度**：已有星的区域数 / 总区域数，或「已通关楼层」粗统计

实现建议（改动小）：

- 在 `TowerView` 的 `towerAreaList` listener 里同步刷新两个 Label  
- **不必强依赖 ViewModel 新属性**（若你希望 MVVM 更纯，再抽到 ViewModel）

### 4. 左侧栏 polish

- DifficultyCell 保持「名称 + 星数」布局，微调 padding / 字号
- 满星金色逻辑保留
- HistoryCell 日期格式改为 `yyyy.MM.dd — yyyy.MM.dd`（去掉生硬 `--`）
- 历史仍仅 `difficulty == 3` 显示（业务不变），但侧栏宽度固定，避免跳动

### 5. CSS 升级（`Tower.css`）

**区域卡（ResourceBriefing 配方，挂在 `.tower .right .area`）：**

```css
.tower .right .area {
  -fx-spacing: 8;
  -fx-padding: 16 18 14 18;
  -fx-background-color: -color-bg-default-40;
  -fx-background-radius: 14;
  -fx-border-radius: 14;
  -fx-border-color: -color-border-default;
  -fx-border-width: 1;
  -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 12, 0, 0, 4);
}
/* 可选 hover，轻微抬升 */
.tower .right .area:hover {
  -fx-border-color: -color-accent-emphasis-20;
  -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 16, 0, 0, 6);
}
```

**新增：**

```css
.tower .right .area .floor-row { -fx-spacing: 16; -fx-alignment: CENTER_LEFT; -fx-padding: 4 0; }
.tower .right .area .floor-title { -fx-min-width: 56; -fx-font-size: 14; -fx-text-fill: -color-font-muted; }
.tower .right .area .star-box { -fx-spacing: 2; -fx-min-width: 78; -fx-alignment: CENTER_LEFT; }
.tower .right .area .star-empty { -fx-opacity: 0.28; }
.tower .right .area .area-header { -fx-alignment: CENTER_LEFT; }
.tower .history-section > .label { -fx-padding: 8 0 4 10; }
```

**主题注意：**

- 尽量少硬编码 `#ffffffxx`；卡片用全局/主题 token `-color-bg-default-40`。
- `.tower` 顶部的本地 token 覆盖可保留以兼容 NewTower，但新卡片优先用主题 token。

### 6. FlowPane 间距

- `hgap/vgap`：`50/40` → **`24/24`**（更密、更现代，仍透出背景）
- `columnHalignment="LEFT"`（卡片左对齐更整齐）

### 7. 明确不改

- 不改 MainView 模糊实现
- 不改 API / `TowerViewModel` 拉数逻辑（除非做摘要属性）
- 不改 `TowerGroupView` 顶栏 Tab
- 不顺手修 NewTower 的 `isUnLock` bug（可另开任务）
- 不做 i18n 全量替换（硬编码中文可暂留，与现状一致）

---

## 实现步骤

1. **改 `TowerView.fxml`**：右侧 header + summary + scroll 结构；左侧统一 200 宽。
2. **改 `TowerView.java`**：
   - 绑定/刷新 summary
   - 重写 `AreaCell`（header 星数、3 星槽、floor-row、role-item）
   - HistoryCell 文案微调
3. **改 `Tower.css`**：卡片 elevation、floor-row、空星、header 间距；FlowPane 间距在 FXML。
4. **肉眼验收**：浅色主题下在模糊背景上对比「难度切换 / 历史切换 / 未满星 / 无角色楼层」。

---

## 验收标准

- 模糊背景清晰透出，卡片呈玻璃质感（非白板）
- 区域卡圆角/边框/阴影接近 ResourceBriefing
- 每层固定 3 星对齐，角色与「暂无数据」不把布局撑乱
- 标题 + 倒计时同一行，不再漂浮绝对定位
- 摘要条随难度变化正确更新
- NewTower / Slash 外观不被明显破坏

---

## 风险与取舍

| 点 | 说明 |
|----|------|
| 共享 CSS | `.area` 增强会影响 NewTower 卡片——这是**加分**（统一质感）；若 NewTower 过挤，可加 `.tower .right .area.classic` 作用域隔离 |
| 每层 max=3 | API 无 per-floor maxStar，采用惯例 3；若以后有差异再改 |
| hover 阴影 | JavaFX CSS 对伪类支持有限，`:hover` 在部分节点可用；不行则只做静态 elevation |

---

## 建议实施优先级

**P0（本次必做）**：FXML 右侧结构 + AreaCell 重构 + 卡片 CSS + 3 星槽 + 区域星数  
**P1**：SummaryBar 总星/进度  
**P2**：历史文案、间距微调、空状态文案样式  

按上述方案改完后，逆境深塔会从「列表 + 简陋方块」升级为与 NewTower / 资源简报一致的玻璃仪表盘风格，同时完整保留 MainView 模糊背景。