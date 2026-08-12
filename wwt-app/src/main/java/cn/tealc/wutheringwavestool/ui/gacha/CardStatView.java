package cn.tealc.wutheringwavestool.ui.gacha;

import cn.tealc.wutheringwavestool.model.gacha.StatItem;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.antdesignicons.AntDesignIconsOutlined;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 抽卡统计视图。
 *
 * @author Leck
 */
public class CardStatView implements FxmlView<CardStatViewModel>, Initializable {

    // ikonli 图标替代 SVG path
    private static final Ikon STAR_ICON = AntDesignIconsOutlined.STAR;
    private static final Ikon AVG_ICON = AntDesignIconsOutlined.AREA_CHART;
    private static final Ikon DATE_ICON = AntDesignIconsOutlined.CLOCK_CIRCLE;
    private static final Ikon ROLE_ICON = Material2MZ.PERSON;
    private static final Ikon WEAPON_ICON = AntDesignIconsOutlined.TROPHY;

    @InjectViewModel
    private CardStatViewModel viewModel;

    @FXML private StackPane emptyPane;
    @FXML private StackPane loadingPane;
    @FXML private VBox cardTotal;
    @FXML private VBox cardSsr;
    @FXML private VBox cardSr;
    @FXML private VBox cardUpSsr;
    @FXML private PieChart pieChart;
    @FXML private Label donutTotalLabel;
    @FXML private VBox rarityLegend;
    @FXML private HBox statGroup;
    @FXML private VBox roleCard;
    @FXML private VBox weaponCard;
    @FXML private ToggleButton roleBtn;
    @FXML private ToggleButton weaponBtn;
    @FXML private ComboBox<String> rarityCombo;
    @FXML private ListView<StatItem> statListView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        emptyPane.visibleProperty().bind(viewModel.emptyProperty());
        emptyPane.managedProperty().bind(emptyPane.visibleProperty());
        loadingPane.visibleProperty().bind(viewModel.loadingProperty());
        loadingPane.managedProperty().bind(loadingPane.visibleProperty());

        fillStatCard(cardTotal, "总抽数", viewModel.totalPullsTextProperty(), "stat-total",
                SubType.STAR, viewModel.totalStonesTextProperty(), null);
        fillStatCard(cardSsr, "五星数", viewModel.ssrCountTextProperty(), "stat-ssr",
                SubType.AVATAR, viewModel.topSsrNameTextProperty(), viewModel.topSsrIdProperty());
        fillStatCard(cardSr, "四星数", viewModel.srCountTextProperty(), "stat-sr",
                SubType.AVATAR, viewModel.topSrNameTextProperty(), viewModel.topSrIdProperty());
        fillStatCard(cardUpSsr, "UP五星", viewModel.upSsrCountTextProperty(), "stat-up",
                SubType.AVATAR, viewModel.topUpSsrNameTextProperty(), viewModel.topUpSsrIdProperty());

        pieChart.setData(viewModel.getPieChartData());
        pieChart.setAnimated(false);
        pieChart.setLabelsVisible(false);
        pieChart.setLegendVisible(false);
        donutTotalLabel.textProperty().bind(viewModel.totalPullsTextProperty());
        viewModel.getPieChartData().addListener((ListChangeListener<PieChart.Data>) change -> refreshRarityLegend());
        refreshRarityLegend();

        fillStatGroup();
        fillCompareCard(roleCard, "角色池", "stat-role", ROLE_ICON,
                viewModel.rolePullsTextProperty(), viewModel.roleSsrTextProperty(), viewModel.roleAvgTextProperty());
        fillCompareCard(weaponCard, "武器池", "stat-weapon", WEAPON_ICON,
                viewModel.weaponPullsTextProperty(), viewModel.weaponSsrTextProperty(), viewModel.weaponAvgTextProperty());

        statListView.setItems(viewModel.getStatItems());
        statListView.setCellFactory(list -> new StatItemCell());
        viewModel.getStatItems().addListener((ListChangeListener<StatItem>) change -> statListView.refresh());

        roleBtn.setSelected(true);
        roleBtn.selectedProperty().addListener((obs, old, selected) -> {
            if (selected) viewModel.setRoleFilter(true);
        });
        weaponBtn.selectedProperty().addListener((obs, old, selected) -> {
            if (selected) viewModel.setWeaponFilter(true);
        });

        rarityCombo.setItems(viewModel.getRarityOptions());
        rarityCombo.getSelectionModel().select(0);
        rarityCombo.getSelectionModel().selectedIndexProperty().addListener((obs, old, idx) -> {
            if (idx != null) viewModel.setRarityFilter(idx.intValue());
        });
        // 弹出列表样式
        rarityCombo.setVisibleRowCount(4);
        rarityCombo.getStyleClass().add("combo-popup");

        // 空列表提示
        Label emptyHint = new Label("该筛选条件下无数据");
        emptyHint.getStyleClass().add("stat-empty-hint");
        statListView.setPlaceholder(emptyHint);
    }

    private void fillStatCard(VBox card, String title, SimpleStringProperty valueProperty,
                              String colorClass, SubType subType, SimpleStringProperty detailProperty,
                              SimpleIntegerProperty resourceIdProperty) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-label");
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        StackPane emblem = createEmblem(colorClass, STAR_ICON);
        HBox header = new HBox(titleLabel, spacer, emblem);
        header.setAlignment(Pos.CENTER_LEFT);

        Label valueLabel = new Label();
        valueLabel.textProperty().bind(valueProperty);
        valueLabel.getStyleClass().add("stat-value");

        Node footer;
        if (subType == SubType.STAR) {
            Label stoneValue = new Label();
            stoneValue.textProperty().bind(Bindings.concat(detailProperty, " 星声"));
            stoneValue.getStyleClass().add("stat-card-detail");
            footer = stoneValue;
        } else {
            ImageView avatar = createAvatar(44);
            Label name = new Label();
            name.textProperty().bind(detailProperty);
            name.getStyleClass().add("stat-card-name");
            Label caption = new Label("最多角色");
            caption.getStyleClass().add("stat-card-caption");
            VBox text = new VBox(name, caption);
            text.setSpacing(0);
            StackPane avatarFrame = new StackPane(avatar);
            avatarFrame.getStyleClass().add("stat-card-avatar-frame");
            avatarFrame.setMinSize(48, 48);
            avatarFrame.setMaxSize(48, 48);
            HBox avatarRow = new HBox(avatarFrame, text);
            avatarRow.setAlignment(Pos.CENTER_LEFT);
            avatarRow.setSpacing(8);
            bindAvatar(avatar, resourceIdProperty, 64);
            footer = avatarRow;
        }

        card.getChildren().setAll(header, valueLabel, footer);
        card.getStyleClass().add(colorClass);
    }

    private void refreshRarityLegend() {
        double total = viewModel.getPieChartData().stream().mapToDouble(PieChart.Data::getPieValue).sum();
        rarityLegend.getChildren().clear();
        for (int i = 0; i < viewModel.getPieChartData().size(); i++) {
            PieChart.Data data = viewModel.getPieChartData().get(i);
            double percentage = total == 0 ? 0 : data.getPieValue() * 100 / total;
            Pane dot = new Pane();
            dot.getStyleClass().addAll("rarity-dot", "rarity-" + i);
            Label name = new Label(data.getName());
            name.getStyleClass().add("rarity-name");
            Label value = new Label(String.format("%.2f%%", percentage));
            value.getStyleClass().add("rarity-percent");
            Pane spacer = new Pane();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            HBox row = new HBox(dot, name, spacer, value);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setSpacing(8);
            rarityLegend.getChildren().add(row);
        }
    }

    private void fillStatGroup() {
        statGroup.getChildren().setAll(
                createMetricTile("五星不歪率", viewModel.nonBannerRateTextProperty(), "metric-star", STAR_ICON),
                createMetricTile("五星平均", viewModel.ssrAvgTextProperty(), "metric-average", AVG_ICON),
                createMetricTile("抽卡时间", viewModel.dateRangeTextProperty(), "metric-date", DATE_ICON)
        );
    }

    private VBox createMetricTile(String title, SimpleStringProperty valueProperty,
                                  String styleClass, Ikon iconIkon) {
        FontIcon icon = new FontIcon(iconIkon);
        icon.setIconSize(18);
        icon.getStyleClass().add("metric-icon");
        StackPane iconBox = new StackPane(icon);
        iconBox.setMinSize(24, 24);
        iconBox.setPrefSize(24, 24);
        iconBox.setMaxSize(24, 24);
        iconBox.getStyleClass().add("metric-icon-box");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("metric-title");
        HBox heading = new HBox(iconBox, titleLabel);
        heading.setAlignment(Pos.CENTER_LEFT);
        heading.setSpacing(5);
        heading.getStyleClass().add("metric-heading");

        Node valueNode;
        if ("metric-date".equals(styleClass)) {
            Label startDate = new Label();
            Label endDate = new Label();
            startDate.textProperty().bind(Bindings.createStringBinding(
                    () -> dateRangePart(valueProperty.get(), 0), valueProperty));
            endDate.textProperty().bind(Bindings.createStringBinding(
                    () -> dateRangePart(valueProperty.get(), 1), valueProperty));
            startDate.getStyleClass().add("metric-date-line");
            endDate.getStyleClass().add("metric-date-line");
            VBox dateLines = new VBox(startDate, endDate);
            dateLines.setSpacing(1);
            dateLines.getStyleClass().add("metric-date-lines");
            valueNode = dateLines;
        } else {
            Label value = new Label();
            value.textProperty().bind(valueProperty);
            value.setWrapText(true);
            value.getStyleClass().add("metric-value");
            valueNode = value;
        }
        Pane spacer = new Pane();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        VBox tile = new VBox(heading, spacer, valueNode);
        tile.getStyleClass().addAll("metric-tile", styleClass);
        tile.setMinWidth(0);
        tile.setPrefWidth(0);
        tile.setMaxWidth(Double.MAX_VALUE);
        tile.setSpacing(3);
        HBox.setHgrow(tile, javafx.scene.layout.Priority.ALWAYS);
        return tile;
    }

    private void fillCompareCard(VBox card, String title, String styleClass, Ikon iconIkon,
                                 SimpleStringProperty pullsProperty, SimpleStringProperty ssrProperty,
                                 SimpleStringProperty averageProperty) {
        StackPane icon = createEmblem(styleClass, iconIkon);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("pool-title");
        HBox header = new HBox(icon, titleLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(8);
        header.getStyleClass().add("pool-header");

        HBox values = new HBox(
                createPoolMetric("总抽数", pullsProperty, false),
                createPoolMetric("五星数", ssrProperty, false),
                createPoolMetric("五星平均", averageProperty, true));
        values.setAlignment(Pos.CENTER);
        VBox.setVgrow(values, javafx.scene.layout.Priority.ALWAYS);
        values.getStyleClass().add("pool-values");
        values.setSpacing(0);
        card.getChildren().setAll(header, values);
        card.getStyleClass().add(styleClass);
    }

    private VBox createPoolMetric(String title, SimpleStringProperty valueProperty, boolean last) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("pool-metric-title");
        Label valueLabel = new Label();
        valueLabel.textProperty().bind(valueProperty);
        valueLabel.getStyleClass().add("pool-metric-value");
        VBox metric = new VBox(titleLabel, valueLabel);
        metric.setAlignment(Pos.CENTER);
        metric.setMaxHeight(Double.MAX_VALUE);
        metric.getStyleClass().add("pool-metric");
        if (last) metric.getStyleClass().add("last");
        HBox.setHgrow(metric, Priority.ALWAYS);
        return metric;
    }

    private static StackPane createEmblem(String styleClass, Ikon iconIkon) {
        FontIcon icon = new FontIcon(iconIkon);
        icon.setIconSize(16);
        icon.getStyleClass().add("emblem-icon");
        StackPane emblem = new StackPane(icon);
        emblem.getStyleClass().addAll("stat-emblem", styleClass);
        return emblem;
    }

    private static String dateRangePart(String value, int index) {
        if (value == null || value.isBlank()) return "—";
        String[] parts = value.split(" ~ ");
        if (parts.length != 2) return index == 0 ? datePart(value) : "—";
        return datePart(parts[index]);
    }

    private static String datePart(String value) {
        int separator = value.indexOf(' ');
        return separator > 0 ? value.substring(0, separator) : value;
    }

    private static ImageView createAvatar(double size) {
        ImageView avatar = new ImageView();
        avatar.setFitWidth(size);
        avatar.setFitHeight(size);
        avatar.setPreserveRatio(false);
        avatar.setSmooth(true);
        avatar.setClip(new Circle(size / 2, size / 2, size / 2));
        avatar.getStyleClass().add("stat-avatar");
        return avatar;
    }

    private static void bindAvatar(ImageView avatar, SimpleIntegerProperty resourceIdProperty, double loadSize) {
        if (resourceIdProperty == null) return;
        Runnable load = () -> {
            int id = resourceIdProperty.get();
            if (id <= 0) {
                avatar.setImage(null);
                return;
            }
            Thread.startVirtualThread(() -> {
                Image image = LocalResourcesManager.header(id, loadSize, loadSize);
                Platform.runLater(() -> {
                    if (resourceIdProperty.get() == id) avatar.setImage(image);
                });
            });
        };
        load.run();
        resourceIdProperty.addListener((obs, old, value) -> load.run());
    }

    private enum SubType { STAR, AVATAR }

    private class StatItemCell extends ListCell<StatItem> {
        @Override
        protected void updateItem(StatItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            VBox root = new VBox();
            String rarityClass = item.getQualityLevel() == 5 ? "rank-ssr" : "rank-sr";
            root.getStyleClass().addAll("rank-cell", rarityClass);
            root.setMinWidth(0);
            root.setMaxWidth(Double.MAX_VALUE);
            root.prefWidthProperty().bind(getListView().widthProperty().subtract(20));

            ImageView avatar = createAvatar(42);
            StackPane avatarFrame = new StackPane(avatar);
            avatarFrame.getStyleClass().add("rank-avatar-frame");
            avatarFrame.setMinSize(46, 46);
            avatarFrame.setMaxSize(46, 46);

            Label name = new Label(item.getName());
            name.getStyleClass().add("rank-name");
            Pane nameSpacer = new Pane();
            HBox.setHgrow(nameSpacer, javafx.scene.layout.Priority.ALWAYS);
            Label count = new Label("x" + item.getCount());
            count.getStyleClass().add("rank-count");
            HBox line = new HBox(avatarFrame, name, nameSpacer, count);
            line.setAlignment(Pos.CENTER_LEFT);
            line.setSpacing(9);
            line.setMinWidth(0);
            line.setMaxWidth(Double.MAX_VALUE);

            root.getChildren().add(line);
            setGraphic(root);

            int resourceId = item.getResourceId();
            Image image = LocalResourcesManager.header(resourceId, 64, 64);
            avatar.setImage(image);
        }
    }
}
