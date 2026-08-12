package cn.tealc.wutheringwavestool.ui.gacha;

import cn.tealc.wutheringwavestool.ui.gacha.CardStatViewModel.StatItem;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 抽卡统计视图：图文并茂展示总体统计、饼图、角色vs武器对比、右侧抽取次数列表。
 *
 * @author Leck
 */
public class CardStatView implements FxmlView<CardStatViewModel>, Initializable {

    @InjectViewModel
    private CardStatViewModel viewModel;

    @FXML
    private StackPane emptyPane;
    @FXML
    private StackPane loadingPane;

    @FXML
    private VBox cardTotal;
    @FXML
    private VBox cardSsr;
    @FXML
    private VBox cardSr;
    @FXML
    private VBox cardUpSsr;

    @FXML
    private PieChart pieChart;

    @FXML
    private VBox statGroup;

    @FXML
    private VBox roleCard;
    @FXML
    private VBox weaponCard;

    @FXML
    private ToggleButton roleBtn;
    @FXML
    private ToggleButton weaponBtn;

    @FXML
    private ListView<StatItem> statListView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        emptyPane.visibleProperty().bind(viewModel.emptyProperty());
        loadingPane.visibleProperty().bind(viewModel.loadingProperty());

        // 顶部统计卡片：左装饰头像 + 中大数值 + 右说明文本 + 下方副内容
        fillStatCard(cardTotal, "总抽数", viewModel.totalPullsTextProperty(), "stat-total",
                SubType.STAR, "相当于", viewModel.totalStonesTextProperty(), null,
                viewModel.getRoleImagePaths()[0]);
        fillStatCard(cardSsr, "五星数", viewModel.ssrCountTextProperty(), "stat-ssr",
                SubType.AVATAR, "最多角色", viewModel.topSsrNameTextProperty(), viewModel.topSsrIdProperty(),
                viewModel.getRoleImagePaths()[1]);
        fillStatCard(cardSr, "四星数", viewModel.srCountTextProperty(), "stat-sr",
                SubType.AVATAR, "最多角色", viewModel.topSrNameTextProperty(), viewModel.topSrIdProperty(),
                viewModel.getRoleImagePaths()[2]);
        fillStatCard(cardUpSsr, "UP五星", viewModel.upSsrCountTextProperty(), "stat-up",
                SubType.AVATAR, "最多角色", viewModel.topUpSsrNameTextProperty(), viewModel.topUpSsrIdProperty(),
                viewModel.getRoleImagePaths()[3]);

        // 饼图
        pieChart.setData(viewModel.getPieChartData());
        pieChart.setAnimated(false);
        pieChart.setLabelsVisible(false);
        pieChart.setTitle(null);

        // 饼图下方补充统计行
        fillStatGroup(statGroup);

        // 角色 vs 武器对比卡片
        fillCompareCard(roleCard, "角色池", "stat-role",
                viewModel.rolePullsTextProperty(), viewModel.roleSsrTextProperty(), viewModel.roleAvgTextProperty());
        fillCompareCard(weaponCard, "武器池", "stat-weapon",
                viewModel.weaponPullsTextProperty(), viewModel.weaponSsrTextProperty(), viewModel.weaponAvgTextProperty());

        // 右侧列表
        statListView.setItems(viewModel.getStatItems());
        statListView.setCellFactory(lv -> new StatItemCell());

        // 筛选按钮：默认选中角色
        roleBtn.setSelected(true);
        roleBtn.selectedProperty().addListener((obs, old, val) -> {
            if (val) viewModel.setRoleFilter(true);
        });
        weaponBtn.selectedProperty().addListener((obs, old, val) -> {
            if (val) viewModel.setWeaponFilter(true);
        });
    }

    /**
     * 填充统计卡片：左装饰头像 + 中大数值 + 右说明文本/副内容
     *
     * 右侧布局：上方说明文本，下方副内容（星声图标+数量 或 角色头像+名字）
     */
    private void fillStatCard(VBox card, String title,
                              javafx.beans.property.SimpleStringProperty valueProp,
                              String styleClass,
                              SubType subType,
                              String subLabel,
                              javafx.beans.property.SimpleStringProperty subValueProp,
                              javafx.beans.property.SimpleIntegerProperty idProp,
                              javafx.beans.property.SimpleStringProperty decorImagePath) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-label");

        // 左侧装饰头像
        ImageView decorAvatar = new ImageView();
        decorAvatar.setFitHeight(48);
        decorAvatar.setFitWidth(48);
        decorAvatar.setSmooth(true);
        decorAvatar.imageProperty().bind(javafx.beans.binding.Bindings.createObjectBinding(() -> {
            String path = decorImagePath.get();
            return path.isEmpty() ? null : new Image(path, 45, 45, true, true, true);
        }, decorImagePath));

        // 中间大数值
        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("stat-value");
        if (valueProp != null) {
            valueLabel.textProperty().bind(valueProp);
        }

        // 右侧：上方说明文本 + 下方副内容
        VBox rightBox = new VBox();
        rightBox.setAlignment(Pos.CENTER_LEFT);
        rightBox.setSpacing(2);

        Label subTextLabel = new Label(subLabel);
        subTextLabel.getStyleClass().add("stat-sub-label");
        rightBox.getChildren().add(subTextLabel);

        if (subType == SubType.STAR) {
            // 下方：数量 + 星声图标
            HBox starRow = new HBox();
            starRow.setAlignment(Pos.CENTER_LEFT);
            starRow.setSpacing(4);

            Label starValue = new Label();
            starValue.getStyleClass().add("stat-sub");
            if (subValueProp != null) {
                starValue.textProperty().bind(subValueProp);
            }

            ImageView starIcon = new ImageView();
            try {
                starIcon.setImage(new Image(
                    getClass().getResource("/cn/tealc/wutheringwavestool/image/icon/xingsheng.png").toExternalForm(),
                    14, 14, true, true));
            } catch (Exception e) { /* fallback */ }

            starRow.getChildren().addAll(starValue, starIcon);
            rightBox.getChildren().add(starRow);

        } else if (subType == SubType.AVATAR) {
            // 下方：角色头像 + 名字
            HBox avatarRow = new HBox();
            avatarRow.setAlignment(Pos.CENTER_LEFT);
            avatarRow.setSpacing(6);

            ImageView avatar = new ImageView();
            avatar.setFitHeight(28);
            avatar.setFitWidth(28);
            avatar.setSmooth(true);

            Label nameValue = new Label();
            nameValue.getStyleClass().add("stat-sub");
            if (subValueProp != null) {
                nameValue.textProperty().bind(subValueProp);
            }

            avatarRow.getChildren().addAll(avatar, nameValue);
            rightBox.getChildren().add(avatarRow);

            if (idProp != null) {
                Runnable loadAvatar = () -> {
                    int id = idProp.get();
                    if (id > 0) {
                        Thread.startVirtualThread(() -> {
                            Image image = LocalResourcesManager.header(id, 40, 40);
                            Platform.runLater(() -> avatar.setImage(image));
                        });
                    }
                };
                loadAvatar.run();
                idProp.addListener((obs, old, val) -> loadAvatar.run());
            }
        }

        // 左中右行
        HBox mainRow = new HBox(decorAvatar, valueLabel, rightBox);
        mainRow.setAlignment(Pos.CENTER_LEFT);
        mainRow.setSpacing(10);

        card.getChildren().setAll(titleLabel, mainRow);
        card.getStyleClass().add(styleClass);
    }

    private enum SubType { STAR, AVATAR }

    private void fillStatGroup(VBox group) {
        group.getChildren().setAll(
                createStatRow("五星不歪率", viewModel.nonBannerRateTextProperty(), "stat-ssr"),
                createStatRow("五星平均", viewModel.ssrAvgTextProperty(), "stat-ssr"),
                createStatRow("抽卡时间", viewModel.dateRangeTextProperty(), "stat-total")
        );
    }

    private HBox createStatRow(String title, javafx.beans.property.SimpleStringProperty prop, String colorClass) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-row-label");

        Label valueLabel = new Label();
        valueLabel.textProperty().bind(prop);
        valueLabel.getStyleClass().addAll("stat-row-value", colorClass);

        HBox row = new HBox(titleLabel, valueLabel);
        row.getStyleClass().add("stat-compare-row");
        return row;
    }

    private void fillCompareCard(VBox card, String title, String styleClass,
                                 javafx.beans.property.SimpleStringProperty pullsProp,
                                 javafx.beans.property.SimpleStringProperty ssrProp,
                                 javafx.beans.property.SimpleStringProperty avgProp) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title-3");
        Separator sep = new Separator();
        sep.getStyleClass().add("stat-separator");

        card.getChildren().setAll(titleLabel, sep,
                createStatRow("总抽数", pullsProp, styleClass),
                createStatRow("五星数", ssrProp, styleClass),
                createStatRow("五星平均", avgProp, styleClass));
        card.getStyleClass().add(styleClass);
    }

    /**
     * 右侧列表项：头像 + 名称 + 抽数
     */
    private static class StatItemCell extends ListCell<StatItem> {
        @Override
        protected void updateItem(StatItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                BorderPane root = new BorderPane();
                root.getStyleClass().add("role-cell");

                ImageView iv = new ImageView();
                iv.setFitHeight(40);
                iv.setFitWidth(40);
                iv.setSmooth(true);
                root.setLeft(iv);

                Label name = new Label(item.getName());
                name.getStyleClass().add("role-name");
                VBox center = new VBox(name);
                center.setPadding(new Insets(0, 0, 0, 8));
                center.setAlignment(Pos.CENTER_LEFT);
                root.setCenter(center);

                Label count = new Label("×" + item.getCount());
                count.getStyleClass().addAll("role-name", "stat-item-count");
                root.setRight(count);

                setGraphic(root);

                Thread.startVirtualThread(() -> {
                    Image image = LocalResourcesManager.header(item.getResourceId(), 60, 60);
                    Platform.runLater(() -> {
                        if (getGraphic() == root) {
                            iv.setImage(image);
                        }
                    });
                });
            }
        }
    }
}
