package cn.tealc.wutheringwavestool.ui.kujiequ.other;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.service.WebKujiequManager;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.teafx.utils.message.MessageType;
import com.kuro.kujiequ.KujiequManager;
import com.kuro.kujiequ.model.resourcebriefing.Briefing;
import com.kuro.kujiequ.model.resourcebriefing.Item;
import com.kuro.kujiequ.model.resourcebriefing.Record;
import com.kuro.kujiequ.model.resourcebriefing.Title;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.model.ResponseBody;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceBriefingViewModel extends BaseViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceBriefingViewModel.class);
    public static final String EVENT_SELECT_BOX = "selectBox";

    @Inject
    private UserInfoDao userInfoDao;

    @Inject
    private WebKujiequManager webKujiequManager;

    @Inject
    private KujiequManager kujiequManager;

    private ObservableList<Item> starList = FXCollections.observableArrayList();
    private ObservableList<Item> coinList = FXCollections.observableArrayList();
    private SimpleLongProperty starNum = new SimpleLongProperty();
    private SimpleLongProperty coinNum = new SimpleLongProperty();
    private ObservableList<Title>  recordTypeList = FXCollections.observableArrayList();
    private Briefing briefing;
    private UserInfo userInfo;
    private KujiequManager.BriefingType currentType = KujiequManager.BriefingType.MONTH;
    public void init() {
        userInfo = userInfoDao.getMain();
        if (userInfo != null){
            initList();
        }else {
            publish("EMPTY");
        }
    }

    public void toMonth(){
        recordTypeList.setAll(briefing.getMonths());
        currentType = KujiequManager.BriefingType.MONTH;
        publish(EVENT_SELECT_BOX);

    }

    public void toWeek(){
        recordTypeList.setAll(briefing.getWeeks());
        currentType = KujiequManager.BriefingType.WEEK;
        publish(EVENT_SELECT_BOX);
    }

    public void toVersion(){
        recordTypeList.setAll(briefing.getVersions());
        currentType = KujiequManager.BriefingType.VERSION;
        publish(EVENT_SELECT_BOX);
    }


    public void refresh(Title title){
        refresh(title.getIndex());
    }

    private void initList(){
        Thread.startVirtualThread(() -> {
            try {
                ResponseBody<Briefing> value = kujiequManager.getBriefingList(userInfo);
                Platform.runLater(() -> {
                    if (value.getCode() == 200){
                        briefing = value.getData();
                        toMonth();
                    }else{
                        NotificationManager.message(MessageInfo.warning(value.getMsg()));
                    }
                });
            } catch (Exception e) {
                LOG.error("获取资源简报列表失败", e);
                Platform.runLater(() ->
                        NotificationManager.message(MessageInfo.error(e.getMessage())));
            }
        });
    }


    private void refresh(int index){
        Thread.startVirtualThread(() -> {
            try {
                ResponseBody<Record> value = kujiequManager.getBriefingDetail(userInfo, String.valueOf(index), currentType);
                Platform.runLater(() -> {
                    if (value.getCode() == 200){
                        update(value.getData());
                    }else{

                    }
                });
            } catch (Exception e) {
                LOG.error("获取资源简报详情失败", e);
            }
        });
    }


    /**
     * 在内嵌 WebView 手机窗中打开资源简报
     */
    public void openResourceBriefingInWebView() {
        if (userInfo != null) {
            webKujiequManager.setUserInfo(userInfo);
        }
        webKujiequManager.openResourceBriefing();
    }


    private void update(Record record){
        starNum.set(record.getTotalStar());
        coinNum.set(record.getTotalCoin());
        coinList.setAll(record.getCoinList());
        starList.setAll(record.getStarList());

    }

    public ObservableList<Item> getStarList() {
        return starList;
    }

    public ObservableList<Item> getCoinList() {
        return coinList;
    }

    public long getStarNum() {
        return starNum.get();
    }

    public SimpleLongProperty starNumProperty() {
        return starNum;
    }

    public long getCoinNum() {
        return coinNum.get();
    }

    public SimpleLongProperty coinNumProperty() {
        return coinNum;
    }

    public ObservableList<Title> getRecordTypeList() {
        return recordTypeList;
    }
}