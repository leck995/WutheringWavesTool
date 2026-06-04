package cn.tealc.wutheringwavestool.ui.system.redemptionCode;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.model.RedemptionCodeItem;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.thread.system.RedemptionCodeGetTask;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class RedemptionCodeViewModel extends BaseViewModel {
    private static final String MC1001 = "mc1001";
    private static final String MC1002 = "mc1002";

    private final ObservableList<RedemptionCodeItem> cnCodeList = FXCollections.observableArrayList();
    private final ObservableList<RedemptionCodeItem> globalCodeList = FXCollections.observableArrayList();
    private final SimpleBooleanProperty loading = new SimpleBooleanProperty(false);

    public void initialize() {
        loadRedemptionCodes();
    }

    public void loadRedemptionCodes() {
        loading.set(true);
        RedemptionCodeGetTask task = new RedemptionCodeGetTask();
        task.setOnSucceeded(workerStateEvent -> {
            ResponseBody<Map<String, List<RedemptionCodeItem>>> value = task.getValue();
            if (value.getCode() == 200) {
                Map<String, List<RedemptionCodeItem>> data = value.getData();
                if (data != null) {
                    List<RedemptionCodeItem> cnList = data.get(MC1001);
                    List<RedemptionCodeItem> globalList = data.get(MC1002);
                    if (cnList != null) {
                        cnList.sort(Comparator.comparing(RedemptionCodeItem::isValid).reversed());
                        cnCodeList.setAll(cnList);
                    }
                    if (globalList != null) {
                        globalList.sort(Comparator.comparing(RedemptionCodeItem::isValid).reversed());
                        globalCodeList.setAll(globalList);
                    }
                }
            } else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.WARNING, value.getMsg()), false);
            }
            loading.set(false);
        });
        task.setOnFailed(workerStateEvent -> {
            loading.set(false);
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    new MessageInfo(MessageType.ERROR, "获取兑换码失败"), false);
        });
        Thread.startVirtualThread(task);
    }

    public ObservableList<RedemptionCodeItem> getCnCodeList() {
        return cnCodeList;
    }

    public ObservableList<RedemptionCodeItem> getGlobalCodeList() {
        return globalCodeList;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public SimpleBooleanProperty loadingProperty() {
        return loading;
    }
}
