package cn.tealc.wutheringwavestool.ui.kujiequ.tower;

import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import com.kuro.kujiequ.model.sign.UserInfo;
import de.saxsys.mvvmfx.ViewModel;

public class TowerGroupViewModel extends BaseViewModel {
    @Inject
    private UserInfoDao userInfoDao;
    public void init() {
        UserInfo userInfo = userInfoDao.getMain();
        if (userInfo == null) {
            publish("EMPTY");
        }
    }
}