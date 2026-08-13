package cn.tealc.wutheringwavestool.ui.kujiequ.tower;

import cn.tealc.wutheringwavestool.service.UserInfoService;
import cn.tealc.wutheringwavestool.service.WebKujiequManager;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import com.kuro.kujiequ.model.sign.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TowerGroupViewModel extends BaseViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(TowerGroupViewModel.class);

    @Inject
    private UserInfoService userInfoService;

    @Inject
    private WebKujiequManager webKujiequManager;

    private UserInfo userInfo;

    public void init() {
        userInfo = userInfoService.getMainUser();
        if (userInfo == null) {
            publish("EMPTY");
        }
    }

    /**
     * 在内嵌 WebView 手机窗中打开数据终端
     */
    public void openRoleBoxInWebView() {
        if (userInfo != null) {
            webKujiequManager.setUserInfo(userInfo);
        }
        webKujiequManager.openRoleBox();
    }
}