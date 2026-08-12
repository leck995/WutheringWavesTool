package cn.tealc.wutheringwavestool.ui.gacha;

import cn.tealc.wutheringwavestool.ui.gacha.CardStatViewModel.StatItem;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 抽卡统计视图。
 *
 * @author Leck
 */
public class CardStatView implements FxmlView<CardStatViewModel>, Initializable {
    private static final String SPARKLE_PATH =
            "M12 0 L15.2 8.8 L24 12 L15.2 15.2 L12 24 L8.8 15.2 L0 12 L8.8 8.8 Z";
    private static final String ROLE_PATH =
            "M12 2 L14.7 5.8 L19.4 4.6 L19 9.4 L23 12 L19 14.6 L19.4 19.4 L14.7 18.2 L12 22 " +
            "L9.3 18.2 L4.6 19.4 L5 14.6 L1 12 L5 9.4 L4.6 4.6 L9.3 5.8 Z";
    private static final String WEAPON_PATH =
            "M12 1 L14.8 6.2 L20.6 5.4 L18 10.6 L23 14 L17.2 15 L17.6 21 L12 18.4 " +
            "L6.4 21 L6.8 15 L1 14 L6 10.6 L3.4 5.4 L9.2 6.2 Z";

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
        fillCompareCard(roleCard, "角色池", "stat-role", ROLE_PATH,
                viewModel.rolePullsTextProperty(), viewModel.roleSsrTextProperty(), viewModel.roleAvgTextProperty());
        fillCompareCard(weaponCard, "武器池", "stat-weapon", WEAPON_PATH,
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
    }

    private void fillStatCard(VBox card, String title, SimpleStringProperty valueProperty,
                              String colorClass, SubType subType, SimpleStringProperty detailProperty,
                              SimpleIntegerProperty resourceIdProperty) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-label");
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        StackPane emblem = createEmblem(colorClass, SPARKLE_PATH);
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
                createMetricTile("五星不歪率", viewModel.nonBannerRateTextProperty(), "metric-star", SPARKLE_PATH),
                createMetricTile("五星平均", viewModel.ssrAvgTextProperty(), "metric-average",
                        "M2 18 L7 12 L11 15 L17 5 L22 8"),
                createMetricTile("抽卡时间", viewModel.dateRangeTextProperty(), "metric-date",
                        "M12 2 A10 10 0 1 0 12 22 A10 10 0 1 0 12 2 M12 6 L12 12 L16 15")
        );
    }

    private VBox createMetricTile(String title, SimpleStringProperty valueProperty,
                                  String styleClass, String iconPath) {
        SVGPath icon = new SVGPath();
        icon.setContent(iconPath);
        icon.setScaleX(0.68);
        icon.setScaleY(0.68);
        icon.getStyleClass().add("metric-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("metric-title");
        HBox heading = new HBox(icon, titleLabel);
        heading.setAlignment(Pos.CENTER_LEFT);
        heading.setSpacing(6);

        Label value = new Label();
        if ("metric-date".equals(styleClass)) {
            value.textProperty().bind(Bindings.createStringBinding(
                    () -> compactDateRange(valueProperty.get()), valueProperty));
        } else {
            value.textProperty().bind(valueProperty);
        }
        value.setWrapText(true);
        value.getStyleClass().add("metric-value");
        VBox tile = new VBox(heading, value);
        tile.getStyleClass().addAll("metric-tile", styleClass);
        tile.setSpacing(4);
        HBox.setHgrow(tile, javafx.scene.layout.Priority.ALWAYS);
        return tile;
    }

    private void fillCompareCard(VBox card, String title, String styleClass, String iconPath,
                                 SimpleStringProperty pullsProperty, SimpleStringProperty ssrProperty,
                                 SimpleStringProperty averageProperty) {
        StackPane icon = createEmblem(styleClass, iconPath);
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
        HBox.setHgrow(metric, javafx.scene.layout.Priority.ALWAYS);
        return metric;
    }

    private static StackPane createEmblem(String styleClass, String path) {
        SVGPath sparkle = new SVGPath();
        sparkle.setContent(path);
        sparkle.setScaleX(0.65);
        sparkle.setScaleY(0.65);
        sparkle.getStyleClass().add("emblem-shape");
        StackPane emblem = new StackPane(sparkle);
        emblem.getStyleClass().addAll("stat-emblem", styleClass);
        return emblem;
    }

    private static String compactDateRange(String value) {
        if (value == null || value.isBlank()) return "—";
        String[] parts = value.split(" ~ ");
        if (parts.length != 2) return value;
        return datePart(parts[0]) + " ~ " + datePart(parts[1]);
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
            root.getStyleClass().add("rank-cell");
            root.setSpacing(5);

            ImageView avatar = createAvatar(42);
            StackPane avatarFrame = new StackPane(avatar);
            avatarFrame.getStyleClass().add("rank-avatar-frame");
            avatarFrame.setMinSize(46, 46);
            avatarFrame.setMaxSize(46, 46);

            Label name = new Label(item.getName());
            name.getStyleClass().add("rank-name");
            name.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(name, javafx.scene.layout.Priority.ALWAYS);
            Label count = new Label("x" + item.getCount());
            count.getStyleClass().add("rank-count");
            HBox line = new HBox(avatarFrame, name, count);
            line.setAlignment(Pos.CENTER_LEFT);
            line.setSpacing(9);

            int max = getListView().getItems().stream().mapToInt(StatItem::getCount).max().orElse(1);
            double ratio = (double) item.getCount() / max;
            Pane progressFill = new Pane();
            progressFill.getStyleClass().add("rank-progress-fill");
            StackPane progress = new StackPane(progressFill);
            progress.setAlignment(Pos.CENTER_LEFT);
            progress.setMaxWidth(Double.MAX_VALUE);
            progress.getStyleClass().add("rank-progress");
            progressFill.prefWidthProperty().bind(progress.widthProperty().multiply(ratio));
            VBox.setMargin(progress, new Insets(0, 0, 0, 55));

            root.getChildren().addAll(line, progress);
            setGraphic(root);

            int resourceId = item.getResourceId();
            Thread.startVirtualThread(() -> {
                Image image = LocalResourcesManager.header(resourceId, 64, 64);
                Platform.runLater(() -> {
                    if (getItem() == item && getGraphic() == root) avatar.setImage(image);
                });
            });
        }
    }
}
