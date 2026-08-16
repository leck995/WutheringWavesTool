package cn.tealc.wutheringwavestool.ui.system.account;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.ui.component.TabbedViewLayout;
import cn.tealc.wutheringwavestool.ui.kujiequ.account.AccountView;
import cn.tealc.wutheringwavestool.ui.kujiequ.account.AccountViewModel;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.JavaView;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class AccountGroupView extends TabbedViewLayout
        implements JavaView<AccountGroupViewModel>, Initializable {

    private final Tab<AppAccountViewModel> appAccountTab;
    private final Tab<AccountViewModel> kujiequAccountTab;

    public AccountGroupView() {
        super(
                LanguageManager.getString("ui.account.title"),
                50
        );

        appAccountTab = addTab(
                LanguageManager.getString("ui.account.app_account"),
                false,
                AppAccountView.class
        );
        kujiequAccountTab = addTab(
                LanguageManager.getString("ui.account.kujiequ"),
                true,
                AccountView.class
        );
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (Config.setting().isNoKuJieQu()) {
            kujiequAccountTab.setVisible(false);
            kujiequAccountTab.setSelected(false);
            kujiequAccountTab.setDisabled(true);
            // 保留原页面的行为：无库街区账号时显示 App 账号，但不强行选中隐藏页签。
            showTab(appAccountTab);
        } else {
            showTab(kujiequAccountTab);
        }
    }
}
