package cn.tealc.wutheringwavestool.ui.kujiequ.other;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.service.WebKujiequManager;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.teafx.utils.message.MessageType;
import com.kuro.kujiequ.model.resourcebriefing.Briefing;
import com.kuro.kujiequ.model.resourcebriefing.Item;
import com.kuro.kujiequ.model.resourcebriefing.Record;
import com.kuro.kujiequ.model.resourcebriefing.Title;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.resourcebriefing.BriefingDetailGetTask;
import com.kuro.kujiequ.thread.resourcebriefing.BriefingListGetTask;
import de.saxsys.mvvmfx.MvvmFX;
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

    private ObservableList<Item> starList = FXCollections.observableArrayList();
    private ObservableList<Item> coinList = FXCollections.observableArrayList();
    private SimpleLongProperty starNum = new SimpleLongProperty();
    private SimpleLongProperty coinNum = new SimpleLongProperty();
    private ObservableList<Title>  recordTypeList = FXCollections.observableArrayList();
    private Briefing briefing;
    private UserInfo userInfo;
    private BriefingDetailGetTask.Type currentType = BriefingDetailGetTask.Type.MONTH;
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
        currentType = BriefingDetailGetTask.Type.MONTH;
        publish(EVENT_SELECT_BOX);

    }

    public void toWeek(){
        recordTypeList.setAll(briefing.getWeeks());
        currentType = BriefingDetailGetTask.Type.WEEK;
        publish(EVENT_SELECT_BOX);
    }

    public void toVersion(){
        recordTypeList.setAll(briefing.getVersions());
        currentType = BriefingDetailGetTask.Type.VERSION;
        publish(EVENT_SELECT_BOX);
    }


    public void refresh(Title title){
        refresh(title.getIndex());
    }

    private void initList(){
        BriefingListGetTask task = new BriefingListGetTask(userInfo);
        task.setOnSucceeded(workerStateEvent -> {
            ResponseBody<Briefing> value = task.getValue();
            if (value.getCode() == 200){
                briefing = value.getData();
                toMonth();
            }else{
                NotificationManager.message(MessageInfo.warning(value.getMsg()));
            }
        });
        task.setOnFailed(workerStateEvent -> {
            NotificationManager.message(MessageInfo.error(task.getException().getMessage()));
        });
        Thread.startVirtualThread(task);
    }


    private void refresh(int index){
        BriefingDetailGetTask detailGetTask = new BriefingDetailGetTask(userInfo,String.valueOf(index),currentType);

        detailGetTask.setOnSucceeded(workerStateEvent -> {
            ResponseBody<Record> value = detailGetTask.getValue();
            if (value.getCode() == 200){
                update(value.getData());
            }else{

            }
        });
   /*     detailGetTask.setOnFailed(workerStateEvent -> {
            NotificationManager.message(MessageInfo.error(workerStateEvent.getSource().getMessage()));
        });*/
        Thread.startVirtualThread(detailGetTask);
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