package cn.tealc.wutheringwavestool.ui.kujiequ;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import com.kuro.kujiequ.model.resourcebriefing.Briefing;
import com.kuro.kujiequ.model.resourcebriefing.Item;
import com.kuro.kujiequ.model.resourcebriefing.Record;
import com.kuro.kujiequ.model.resourcebriefing.Title;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.resourcebriefing.BriefingDetailGetTask;
import com.kuro.kujiequ.thread.resourcebriefing.BriefingListGetTask;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ResourceBriefingViewModel implements ViewModel {
    public static final String EVENT_SELECT_BOX = "selectBox";
    private ObservableList<Item> starList = FXCollections.observableArrayList();
    private ObservableList<Item> coinList = FXCollections.observableArrayList();
    private SimpleLongProperty starNum = new SimpleLongProperty();
    private SimpleLongProperty coinNum = new SimpleLongProperty();
    private ObservableList<Title>  recordTypeList = FXCollections.observableArrayList();
    private Briefing briefing;
    private UserInfo userInfo;
    private BriefingDetailGetTask.Type currentType = BriefingDetailGetTask.Type.MONTH;
    public ResourceBriefingViewModel() {
        UserInfoDao dao = new UserInfoDao();
        userInfo = dao.getMain();
        initList();
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
        if(userInfo != null){
            BriefingListGetTask task = new BriefingListGetTask(userInfo);
            task.setOnSucceeded(workerStateEvent -> {
                ResponseBody<Briefing> value = task.getValue();
                if (value.getCode() == 200){
                    briefing = value.getData();
                    toMonth();
                }else{

                }
            });
            Thread.startVirtualThread(task);
        }else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    new MessageInfo(MessageType.WARNING,"当前不存在主用户信息，无法获取，请在账号界面添加用户信息"),false);
        }

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
        Thread.startVirtualThread(detailGetTask);
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