package cn.tealc.wutheringwavestool.ui.game;

import cn.tealc.wutheringwavestool.ui.component.TabbedViewLayout;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.JavaView;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class GameStatisticsGroupView extends TabbedViewLayout
        implements JavaView<GameStatisticsGroupViewModel>, Initializable {

    private final Tab<GameTimeViewModel> gameTimeTab;
    private final Tab<GameRecordViewModel> gameRecordTab;

    public GameStatisticsGroupView() {
        super(
                LanguageManager.getString("ui.game_statistics.title"),
                30
        );

        gameTimeTab = addTab(
                LanguageManager.getString("ui.game_statistics.nav.button03"),
                true,
                GameTimeView.class,
                TabAnimation.DEFERRED
        );
        gameRecordTab = addTab(
                LanguageManager.getString("ui.game_statistics.nav.button02"),
                false,
                GameRecordView.class,
                TabAnimation.DEFERRED
        );
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 原页面首次展示时间页时不播放切换动画。
        showTab(gameTimeTab, false);
    }
}
