package cn.tealc.wutheringwavestool.ui.base;

import cn.tealc.wutheringwavestool.base.AppInjector;
import de.saxsys.mvvmfx.ViewModel;

public abstract class BaseViewModel implements ViewModel {
    protected BaseViewModel() {
        AppInjector.getInjector().injectMembers(this);
    }
}
