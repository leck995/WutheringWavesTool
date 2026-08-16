package cn.tealc.wutheringwavestool.ui.kujiequ.tower;

import cn.tealc.wutheringwavestool.ui.component.EmptyTipPane;
import cn.tealc.wutheringwavestool.ui.component.TabbedViewLayout;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import javafx.animation.RotateTransition;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

import java.net.URL;
import java.util.ResourceBundle;

public class TowerGroupView extends TabbedViewLayout
        implements JavaView<TowerGroupViewModel>, Initializable {

    @InjectViewModel
    private TowerGroupViewModel viewModel;

    private final Tab<TowerViewModel> towerTab;
    private final Tab<SlashViewModel> slashTab;
    private final Tab<NewTowerViewModel> newTowerTab;

    private boolean refreshInProgress;

    public TowerGroupView() {
        super(
                LanguageManager.getString("ui.tower.title"),
                50
        );

        towerTab = addTab(
                LanguageManager.getString("ui.tower.title.nav.title01"),
                true,
                TowerView.class
        );
        slashTab = addTab(
                LanguageManager.getString("ui.tower.title.nav.title02"),
                false,
                SlashView.class
        );
        newTowerTab = addTab(
                LanguageManager.getString("ui.tower.title.nav.title03"),
                false,
                NewTowerView.class
        );

        addHeaderActions(10, createRefreshButton(), createPhoneButton());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showTab(towerTab);

        viewModel.subscribe("EMPTY", (key, payload) -> {
            EmptyTipPane tipPane = new EmptyTipPane(
                    "无主账号",
                    "请前往账号-库街区添加设置主账号",
                    Material2MZ.PERSON_ADD_DISABLED
            );
            showPageState(tipPane);
        });
        viewModel.init();
    }

    private Button createRefreshButton() {
        Button refreshButton = new Button(null, new FontIcon(Material2MZ.REFRESH));
        refreshButton.getStyleClass().addAll("button-icon", "flat");
        refreshButton.setTooltip(new Tooltip("刷新数据"));
        refreshButton.setOnAction(event -> refresh(refreshButton));
        return refreshButton;
    }

    private Button createPhoneButton() {
        Button phoneButton = new Button(null, new FontIcon(Material2MZ.SMARTPHONE));
        phoneButton.getStyleClass().addAll("button-icon", "flat");
        phoneButton.setTooltip(new Tooltip("手机视图浏览"));
        phoneButton.setOnAction(event -> viewModel.openRoleBoxInWebView());
        return phoneButton;
    }

    private void refresh(Button refreshButton) {
        if (refreshInProgress) {
            return;
        }

        refreshInProgress = true;
        refreshButton.setDisable(true);

        RotateTransition transition = new RotateTransition(
                Duration.millis(600),
                refreshButton.getGraphic()
        );
        transition.setByAngle(360);
        transition.setAxis(Rotate.Z_AXIS);
        transition.setOnFinished(event -> {
            refreshButton.setDisable(false);
            refreshInProgress = false;
        });
        transition.play();
        refreshCurrentTab();
    }

    private void refreshCurrentTab() {
        if (towerTab.isSelected()) {
            towerTab.ifLoaded(TowerViewModel::refresh);
        } else if (slashTab.isSelected()) {
            slashTab.ifLoaded(SlashViewModel::refresh);
        } else if (newTowerTab.isSelected()) {
            newTowerTab.ifLoaded(NewTowerViewModel::refresh);
        }
    }
}
