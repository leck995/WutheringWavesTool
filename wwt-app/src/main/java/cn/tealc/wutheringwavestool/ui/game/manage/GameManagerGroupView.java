package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.ui.component.TabbedViewLayout;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;

import java.net.URL;
import java.util.ResourceBundle;

public class GameManagerGroupView extends TabbedViewLayout
        implements JavaView<GameManagerGroupViewModel>, Initializable {

    @InjectViewModel
    private GameManagerGroupViewModel viewModel;

    private final Tab<GameBaseSettingViewModel> baseSettingTab;
    private final Tab<GameAdvanceSettingViewModel> advanceSettingTab;
    private final Tab<GameAssetViewModel> gameAssetTab;

    public GameManagerGroupView() {
        super(
                LanguageManager.getString("ui.game_manager.base.title"),
                35
        );

        configureRootLayout(
                1280,
                720,
                new Insets(10),
                "game-manager-group-view",
                FXResourcesLoader.load("css/game/manage/GameManagerGroup.css")
        );
        configurePageLayout(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE, Insets.EMPTY);
        configureHeaderLayout(0, 0);
        configureContentLayout(0, 35);
        contentPane().setPadding(new Insets(10, 0, 0, 0));

        baseSettingTab = addTab(
                LanguageManager.getString("ui.game_manager.base.tab.title01"),
                true,
                GameBaseSettingView.class
        );
        advanceSettingTab = addTab(
                LanguageManager.getString("ui.game_manager.base.tab.title02"),
                false,
                GameAdvanceSettingView.class
        );
        gameAssetTab = addTab(
                LanguageManager.getString("ui.game_manager.base.tab.title05"),
                false,
                GameAssetView.class
        );
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 未安装时先展示目录选择页，安装完成后再启用页签导航。
        if (!viewModel.installed()) {
            showDirectoryChooser();
            setHeaderDisabled(true);
        } else {
            showTab(baseSettingTab, false);
        }

        NotificationManager.subscribe(NotificationKey.GAME_MANAGER_TO_BASE, (key, payload) -> {
            showTab(baseSettingTab, false);
            setHeaderDisabled(false);
        });

        NotificationManager.subscribe(NotificationKey.GAME_MANAGE_TO_CHOOSE, (key, payload) -> {
            showDirectoryChooser();
            setHeaderDisabled(true);
        });

        NotificationManager.subscribe(NotificationKey.GAME_MANAGER_TO_ASSET, (key, payload) -> {
            selectTab(gameAssetTab, false);
        });
    }

    private void showDirectoryChooser() {
        ViewTuple<GameDirChooseView, GameDirChooseViewModel> viewTuple =
                FluentViewLoader.fxmlView(GameDirChooseView.class).load();
        showContent(viewTuple.getView());
    }
}
