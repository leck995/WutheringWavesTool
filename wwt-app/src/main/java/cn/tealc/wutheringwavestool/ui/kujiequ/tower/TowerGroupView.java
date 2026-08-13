package cn.tealc.wutheringwavestool.ui.kujiequ.tower;

import atlantafx.base.util.Animations;
import cn.tealc.wutheringwavestool.ui.component.EmptyTipPane;
import cn.tealc.wutheringwavestool.ui.game.GameRecordView;
import cn.tealc.wutheringwavestool.ui.game.GameRecordViewModel;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.animation.RotateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

public class TowerGroupView implements FxmlView<TowerGroupViewModel> {
    @InjectViewModel
    private TowerGroupViewModel viewModel;
    @FXML
    private ToggleGroup childSelectedToggle;
    @FXML
    private ToggleButton towerToggle;
    @FXML
    private ToggleButton slashToggle;
    @FXML
    private ToggleButton newTowerToggle;
    @FXML
    private StackPane content;
    @FXML
    private HBox headerPane;
    @FXML
    private StackPane root;

    private Node towerView;
    private Node slashView;
    private Node newTowerView;

    private TowerViewModel towerViewModel;
    private SlashViewModel slashViewModel;
    private NewTowerViewModel newTowerViewModel;

    private boolean refreshing = false;

    public void initialize() {
        createTowerView();
        viewModel.subscribe("EMPTY",(s, objects) -> {
            EmptyTipPane tipPane = new EmptyTipPane("无主账号","请前往账号-库街区添加设置主账号", Material2MZ.PERSON_ADD_DISABLED);
            root.getChildren().setAll(tipPane);
        });
        viewModel.init();

        // 刷新按钮
        Button refreshBtn = new Button(null, new FontIcon(Material2MZ.REFRESH));
        refreshBtn.getStyleClass().addAll("button-icon", "flat");
        refreshBtn.setTooltip(new Tooltip("刷新数据"));
        refreshBtn.setOnAction(e -> {
            if (refreshing) return;
            refreshing = true;
            refreshBtn.setDisable(true);
            RotateTransition rt = new RotateTransition(Duration.millis(600), refreshBtn.getGraphic());
            rt.setByAngle(360);
            rt.setAxis(Rotate.Z_AXIS);
            rt.setOnFinished(ev -> {
                refreshBtn.setDisable(false);
                refreshing = false;
            });
            rt.play();
            refreshCurrentTab();
        });
        // 手机视图浏览按钮（右上角）
        Button phoneBtn = new Button(null, new FontIcon(Material2MZ.SMARTPHONE));
        phoneBtn.getStyleClass().addAll("button-icon", "flat");
        phoneBtn.setTooltip(new Tooltip("手机视图浏览"));
        phoneBtn.setOnAction(e -> viewModel.openRoleBoxInWebView());
        HBox actionBox = new HBox(10, refreshBtn, phoneBtn);
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerPane.getChildren().addAll(spacer, actionBox);
    }



    @FXML
    void toSlash(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            if (slashView == null) {
                ViewTuple<SlashView, SlashViewModel> viewTuple = FluentViewLoader.fxmlView(SlashView.class).load();
                slashView = viewTuple.getView();
                slashViewModel = viewTuple.getViewModel();
            }
            content.getChildren().setAll(slashView);
            Animations.slideInUp(slashView, Duration.millis(300)).play();
        }else {
            toggleButton.setSelected(true);
        }
    }

    @FXML
    void toNewTower(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            if (newTowerView == null) {
                ViewTuple<NewTowerView, NewTowerViewModel> viewTuple = FluentViewLoader.fxmlView(NewTowerView.class).load();
                newTowerView = viewTuple.getView();
                newTowerViewModel = viewTuple.getViewModel();
            }
            content.getChildren().setAll(newTowerView);
            Animations.slideInUp(newTowerView, Duration.millis(300)).play();
        }else {
            toggleButton.setSelected(true);
        }
    }


    @FXML
    void toTower(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            createTowerView();
        }else {
            toggleButton.setSelected(true);
        }
    }

    private void createTowerView(){
        if (towerView == null) {
            ViewTuple<TowerView, TowerViewModel> viewTuple = FluentViewLoader.fxmlView(TowerView.class).load();
            towerView = viewTuple.getView();
            towerViewModel = viewTuple.getViewModel();
        }
        content.getChildren().setAll(towerView);
        Animations.slideInUp(towerView, Duration.millis(300)).play();
    }

    private void refreshCurrentTab() {
        if (towerToggle.isSelected() && towerViewModel != null) {
            towerViewModel.refresh();
        } else if (slashToggle.isSelected() && slashViewModel != null) {
            slashViewModel.refresh();
        } else if (newTowerToggle.isSelected() && newTowerViewModel != null) {
            newTowerViewModel.refresh();
        }
    }
}