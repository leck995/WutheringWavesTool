package cn.tealc.wutheringwavestool.ui.component;

import atlantafx.base.util.Animations;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.ViewModel;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 通用的“标题 + 页签导航 + 内容区”页面基类。
 * <p>
 * 子类直接继承该 JavaFX 节点，并通过 {@link #addTab(String, boolean, Class)}
 * 声明需要懒加载的 mvvmFX FXML 子视图。
 */
public abstract class TabbedViewLayout extends StackPane {
    private static final Duration DEFAULT_ANIMATION_DURATION = Duration.millis(300);
    private static final String COMMON_STYLESHEET =
            FXResourcesLoader.load("css/component/TabbedViewLayout.css");

    private final AnchorPane pagePane = new AnchorPane();
    private final HBox headerPane = new HBox(20);
    private final HBox tabPane = new HBox();
    private final StackPane contentPane = new StackPane();
    private final ToggleGroup toggleGroup = new ToggleGroup();

    protected TabbedViewLayout(String title, double contentTopAnchor) {
        this(title, null, null, contentTopAnchor);
    }

    protected TabbedViewLayout(
            String title,
            String pageStyleClass,
            String stylesheet,
            double contentTopAnchor
    ) {
        Objects.requireNonNull(title, "title");

        configureHeader(title);
        configureContent(contentTopAnchor);
        configurePage(pageStyleClass, stylesheet);
        getChildren().add(pagePane);
    }

    /**
     * 添加一个选项卡，子视图只会在首次切换到该选项卡时创建。
     */
    protected final <VM extends ViewModel, V extends FxmlView<VM>> Tab<VM> addTab(
            String title,
            boolean selected,
            Class<V> viewType
    ) {
        return addTab(title, selected, viewType, TabAnimation.DIRECT);
    }

    protected final <VM extends ViewModel, V extends FxmlView<VM>> Tab<VM> addTab(
            String title,
            boolean selected,
            Class<V> viewType,
            TabAnimation animation
    ) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(viewType, "viewType");
        Objects.requireNonNull(animation, "animation");

        ToggleButton toggleButton = createToggleButton(title, selected);
        Tab<VM> tab = new Tab<>(toggleButton, animation, () -> {
            ViewTuple<V, VM> viewTuple = FluentViewLoader.fxmlView(viewType).load();
            return new LoadedView<>(viewTuple.getView(), viewTuple.getViewModel());
        });
        toggleButton.setOnAction(event -> {
            if (toggleButton.isSelected()) {
                showTab(tab);
            } else {
                // ToggleGroup 默认允许取消当前项，这里保留页面始终有当前页签的行为。
                toggleButton.setSelected(true);
            }
        });
        tabPane.getChildren().add(toggleButton);
        return tab;
    }

    /**
     * 显示指定选项卡，但不主动修改选中状态。
     */
    protected final void showTab(Tab<?> tab) {
        showTab(tab, true);
    }

    protected final void showTab(Tab<?> tab, boolean animate) {
        Objects.requireNonNull(tab, "tab");

        Parent view = tab.load().view();
        showContent(view, animate ? tab.animation : TabAnimation.NONE);
    }

    /**
     * 主动选择并显示指定选项卡，供外部通知或初始化流程使用。
     */
    protected final void selectTab(Tab<?> tab) {
        selectTab(tab, true);
    }

    protected final void selectTab(Tab<?> tab, boolean animate) {
        Objects.requireNonNull(tab, "tab");
        tab.toggleButton.setSelected(true);
        showTab(tab, animate);
    }

    /**
     * 在标题栏末尾添加一个自动靠右的操作区。
     */
    protected final HBox addHeaderActions(double spacing, Node... actions) {
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actionBox = new HBox(spacing, actions);
        headerPane.getChildren().addAll(spacer, actionBox);
        return actionBox;
    }

    /**
     * 使用单个节点替换整个页面，适用于空状态或不可用状态。
     */
    protected final void showPageState(Node stateNode) {
        getChildren().setAll(Objects.requireNonNull(stateNode, "stateNode"));
    }

    protected final void showContent(Node content) {
        showContent(content, TabAnimation.NONE);
    }

    protected final StackPane contentPane() {
        return contentPane;
    }

    protected final void configureRootLayout(
            double prefWidth,
            double prefHeight,
            Insets padding,
            String styleClass,
            String stylesheet
    ) {
        setPrefSize(prefWidth, prefHeight);
        setPadding(Objects.requireNonNull(padding, "padding"));
        if (styleClass != null && !styleClass.isBlank()) {
            getStyleClass().add(styleClass);
        }
        if (stylesheet != null && !stylesheet.isBlank()) {
            getStylesheets().add(stylesheet);
        }
    }

    protected final void configurePageLayout(
            double prefWidth,
            double prefHeight,
            Insets padding
    ) {
        pagePane.setPrefSize(prefWidth, prefHeight);
        pagePane.setPadding(Objects.requireNonNull(padding, "padding"));
    }

    protected final void configureHeaderLayout(double layoutX, double layoutY) {
        headerPane.setLayoutX(layoutX);
        headerPane.setLayoutY(layoutY);
    }

    protected final void configureContentLayout(double layoutX, double layoutY) {
        contentPane.setLayoutX(layoutX);
        contentPane.setLayoutY(layoutY);
    }

    protected final void setHeaderDisabled(boolean disabled) {
        headerPane.setDisable(disabled);
    }

    private void configureHeader(String title) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title-2");

        Separator separator = new Separator(Orientation.VERTICAL);
        headerPane.setPrefHeight(35);
        headerPane.setLayoutX(20);
        headerPane.setLayoutY(20);
        headerPane.getChildren().addAll(titleLabel, separator, tabPane);

        AnchorPane.setTopAnchor(headerPane, 0.0);
        AnchorPane.setLeftAnchor(headerPane, 0.0);
        AnchorPane.setRightAnchor(headerPane, 0.0);
    }

    private void configureContent(double contentTopAnchor) {
        contentPane.setPrefSize(200, 150);
        contentPane.setLayoutX(10);
        contentPane.setLayoutY(10);

        AnchorPane.setTopAnchor(contentPane, contentTopAnchor);
        AnchorPane.setBottomAnchor(contentPane, 0.0);
        AnchorPane.setLeftAnchor(contentPane, 0.0);
        AnchorPane.setRightAnchor(contentPane, 0.0);
    }

    private void configurePage(String pageStyleClass, String stylesheet) {
        pagePane.setPrefSize(1300, 800);
        pagePane.setPadding(new Insets(10));
        pagePane.getStyleClass().add("tabbed-view-layout");
        if (pageStyleClass != null && !pageStyleClass.isBlank()) {
            pagePane.getStyleClass().add(pageStyleClass);
        }
        pagePane.getStylesheets().add(COMMON_STYLESHEET);
        if (stylesheet != null && !stylesheet.isBlank()) {
            pagePane.getStylesheets().add(stylesheet);
        }
        pagePane.getChildren().addAll(headerPane, contentPane);
    }

    private void showContent(Node content, TabAnimation animation) {
        contentPane.getChildren().setAll(Objects.requireNonNull(content, "content"));
        if (animation == TabAnimation.DIRECT) {
            Animations.slideInUp(content, DEFAULT_ANIMATION_DURATION).play();
        } else if (animation == TabAnimation.DEFERRED) {
            content.setOpacity(0);
            Platform.runLater(() -> {
                content.setOpacity(1);
                Animations.slideInUp(content, DEFAULT_ANIMATION_DURATION).play();
            });
        }
    }

    private ToggleButton createToggleButton(String title, boolean selected) {
        Pane selectionIndicator = new Pane();
        selectionIndicator.setPrefSize(200, 200);

        ToggleButton toggleButton = new ToggleButton(title, selectionIndicator);
        toggleButton.setContentDisplay(ContentDisplay.BOTTOM);
        toggleButton.setMnemonicParsing(false);
        toggleButton.setSelected(selected);
        toggleButton.setToggleGroup(toggleGroup);
        toggleButton.getStyleClass().add("child-select");
        return toggleButton;
    }

    public enum TabAnimation {
        NONE,
        DIRECT,
        DEFERRED
    }

    public static final class Tab<VM extends ViewModel> {
        private final ToggleButton toggleButton;
        private final TabAnimation animation;
        private final Supplier<LoadedView<VM>> loader;
        private LoadedView<VM> loadedView;

        private Tab(
                ToggleButton toggleButton,
                TabAnimation animation,
                Supplier<LoadedView<VM>> loader
        ) {
            this.toggleButton = toggleButton;
            this.animation = animation;
            this.loader = loader;
        }

        public boolean isSelected() {
            return toggleButton.isSelected();
        }

        public void setVisible(boolean visible) {
            toggleButton.setVisible(visible);
        }

        public void setDisabled(boolean disabled) {
            toggleButton.setDisable(disabled);
        }

        public void setSelected(boolean selected) {
            toggleButton.setSelected(selected);
        }

        /**
         * 仅在选项卡已经加载后执行操作，不会因此创建子视图。
         */
        public void ifLoaded(Consumer<? super VM> action) {
            Objects.requireNonNull(action, "action");
            if (loadedView != null) {
                action.accept(loadedView.viewModel());
            }
        }

        private LoadedView<VM> load() {
            if (loadedView == null) {
                loadedView = Objects.requireNonNull(loader.get(), "loader 返回了 null");
            }
            return loadedView;
        }
    }

    private record LoadedView<VM extends ViewModel>(Parent view, VM viewModel) {
        private LoadedView {
            Objects.requireNonNull(view, "view");
            Objects.requireNonNull(viewModel, "viewModel");
        }
    }
}
