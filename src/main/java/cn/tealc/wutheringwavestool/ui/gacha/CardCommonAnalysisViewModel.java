package cn.tealc.wutheringwavestool.ui.gacha;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.analysis.AnalysisData;
import cn.tealc.wutheringwavestool.model.analysis.SsrData;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-22 22:25
 */
public class CardCommonAnalysisViewModel implements ViewModel {
    private static final Logger LOG= LoggerFactory.getLogger(CardCommonAnalysisViewModel.class);

    private SimpleStringProperty gameRootDir = new SimpleStringProperty();
    private SimpleStringProperty player=new SimpleStringProperty();
    private ObservableList<String> playerList= FXCollections.observableArrayList();

    private SimpleStringProperty roleBaseTitleLabel = new SimpleStringProperty();
    private SimpleStringProperty roleBaseSrCountLabel = new SimpleStringProperty();
    private SimpleStringProperty roleBaseSrNoUpLabel = new SimpleStringProperty();
    private SimpleStringProperty roleBaseSsrAvgLabel = new SimpleStringProperty();
    private SimpleStringProperty roleBaseSsrCountLabel = new SimpleStringProperty();
    private SimpleStringProperty roleBaseSsrNoUpLabel = new SimpleStringProperty();
    private SimpleStringProperty roleBaseTimeLabel = new SimpleStringProperty();
    private SimpleStringProperty roleBaseTotalTimeLabel = new SimpleStringProperty();
    private ObservableList<SsrData> roleBaseSsrList = FXCollections.observableArrayList();

    private SimpleStringProperty roleEventTitleLabel = new SimpleStringProperty();
    private SimpleStringProperty roleEventSrCountLabel = new SimpleStringProperty();
    private SimpleStringProperty roleEventSrNoUpLabel = new SimpleStringProperty();
    private SimpleStringProperty roleEventSsrAvgLabel = new SimpleStringProperty();
    private SimpleStringProperty roleEventSsrCountLabel = new SimpleStringProperty();
    private SimpleStringProperty roleEventSsrNoUpLabel = new SimpleStringProperty();
    private SimpleStringProperty roleEventTimeLabel = new SimpleStringProperty();
    private SimpleStringProperty roleEventTotalTimeLabel = new SimpleStringProperty();
    private ObservableList<SsrData> roleEventSsrList = FXCollections.observableArrayList();

    private SimpleStringProperty weaponBaseTitleLabel = new SimpleStringProperty();
    private SimpleStringProperty weaponBaseSrCountLabel = new SimpleStringProperty();
    private SimpleStringProperty weaponBaseSrNoUpLabel = new SimpleStringProperty();
    private SimpleStringProperty weaponBaseSsrAvgLabel = new SimpleStringProperty();
    private SimpleStringProperty weaponBaseSsrCountLabel = new SimpleStringProperty();
    private SimpleStringProperty weaponBaseSsrNoUpLabel = new SimpleStringProperty();
    private SimpleStringProperty weaponBaseTimeLabel = new SimpleStringProperty();
    private SimpleStringProperty weaponBaseTotalTimeLabel = new SimpleStringProperty();
    private ObservableList<SsrData> weaponBaseSsrList = FXCollections.observableArrayList();

    private SimpleStringProperty weaponEventTitleLabel = new SimpleStringProperty();
    private SimpleStringProperty weaponEventSrCountLabel = new SimpleStringProperty();
    private SimpleStringProperty weaponEventSrNoUpLabel = new SimpleStringProperty();
    private SimpleStringProperty weaponEventSsrAvgLabel = new SimpleStringProperty();
    private SimpleStringProperty weaponEventSsrCountLabel = new SimpleStringProperty();
    private SimpleStringProperty weaponEventSsrNoUpLabel = new SimpleStringProperty();
    private SimpleStringProperty weaponEventTimeLabel = new SimpleStringProperty();
    private SimpleStringProperty weaponEventTotalTimeLabel = new SimpleStringProperty();
    private ObservableList<SsrData> weaponEventSsrList = FXCollections.observableArrayList();

    private SimpleBooleanProperty empty = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty loading = new SimpleBooleanProperty(true);
    public CardCommonAnalysisViewModel(boolean isEmpty) {
        empty.set(isEmpty);
        loading.set(!isEmpty);
        gameRootDir.bindBidirectional(Config.setting().gameRootDirProperty());
        player.bindBidirectional(Config.setting().gachaCurrentPlayerIdProperty());
        NotificationManager.subscribe(NotificationKey.CARD_POOL_USER_UPDATE,(s, objects) -> {
            @SuppressWarnings("unchecked")
            List<AnalysisData> list = (List<AnalysisData>) objects[0];
            updatePlayer(list);
            loading.set(false);
            empty.set(false);
        });

        NotificationManager.subscribe(NotificationKey.CARD_POOL_USER_EMPTY,(s, objects) -> {
            empty.set(true);
            loading.set(false);
            reset();
        });
    }



    private void reset(){
        roleBaseTitleLabel.set(null);
        roleBaseSrCountLabel.set(null);
        roleBaseSrNoUpLabel.set(null);
        roleBaseSsrAvgLabel.set(null);
        roleBaseSsrCountLabel.set(null);
        roleBaseSsrNoUpLabel.set(null);
        roleBaseTimeLabel.set(null);
        roleBaseTotalTimeLabel.set(null);
        roleEventTitleLabel.set(null);
        roleEventSrCountLabel.set(null);
        roleEventSrNoUpLabel.set(null);
        roleEventSsrAvgLabel.set(null);
        roleEventSsrCountLabel.set(null);
        roleEventSsrNoUpLabel.set(null);
        roleEventTimeLabel.set(null);
        roleEventTotalTimeLabel.set(null);
        weaponBaseTitleLabel.set(null);
        weaponBaseSrCountLabel.set(null);
        weaponBaseSrNoUpLabel.set(null);
        weaponBaseSsrAvgLabel.set(null);
        weaponBaseSsrCountLabel.set(null);
        weaponBaseSsrNoUpLabel.set(null);
        weaponBaseTimeLabel.set(null);
        weaponBaseTotalTimeLabel.set(null);
        weaponEventTitleLabel.set(null);
        weaponEventSrCountLabel.set(null);
        weaponEventSrNoUpLabel.set(null);
        weaponEventSsrAvgLabel.set(null);
        weaponEventSsrCountLabel.set(null);
        weaponEventSsrNoUpLabel.set(null);
        weaponEventTimeLabel.set(null);
        weaponEventTotalTimeLabel.set(null);


        roleEventSsrList.clear();
        roleBaseSsrList.clear();
        weaponBaseSsrList.clear();
        weaponEventSsrList.clear();
    }

    private void updatePlayer(List<AnalysisData> list) {
        if (list.size() > 0) {
            updateRoleEvent(list.get(0));
        }
        if (list.size() > 1) {
            updateWeaponEvent(list.get(1));
        }
        if (list.size() > 2) {
            updateRoleBase(list.get(2));
        }
        if (list.size() > 3) {
            updateWeaponBase(list.get(3));
        }
    }

    private static final String AVG_TEMPLATE = "%.0f";
    private static final String PERCENT_TEMPLATE = "%d  [%05.2f%%]";
    private void updateRoleEvent(AnalysisData data){
        roleEventTitleLabel.set(data.getPoolName());
        roleEventTotalTimeLabel.set(String.valueOf(data.getTotalCount()));
        if (data.isEmpty()){
            roleEventTimeLabel.set("No Data");
            roleEventSsrNoUpLabel.set("0");
            roleEventSrNoUpLabel.set("0");
            roleEventSsrAvgLabel.set("0");
            roleEventSsrCountLabel.set("0");
            roleEventSrCountLabel.set("0");
            roleEventSsrList.clear();
        }else {
            roleEventTimeLabel.set(data.getStartDate() +" - "+ data.getEndDate());
            roleEventSsrNoUpLabel.set(String.valueOf(data.getNoUpSsrCount()));
            roleEventSrNoUpLabel.set(String.valueOf(data.getNoUpSrCount()));
            roleEventSsrAvgLabel.set(String.format(AVG_TEMPLATE,data.getSsrAvg()));
            roleEventSsrCountLabel.set(String.format(PERCENT_TEMPLATE,data.getSsrCount(),(double)data.getSsrCount() / data.getTotalCount() * 100.0));
            roleEventSrCountLabel.set(String.format(PERCENT_TEMPLATE,data.getSrCount(),(double)data.getSrCount() / data.getTotalCount() * 100));
            roleEventSsrList.setAll(data.getSsrDataList());
        }
    }
    private void updateRoleBase(AnalysisData data){
        roleBaseTitleLabel.set(data.getPoolName());
        roleBaseTotalTimeLabel.set(String.valueOf(data.getTotalCount()));
        if (data.isEmpty()){
            roleBaseTimeLabel.set("No Data");
            roleBaseSsrNoUpLabel.set("0");
            roleBaseSrNoUpLabel.set("0");
            roleBaseSsrAvgLabel.set("0");
            roleBaseSsrCountLabel.set("0");
            roleBaseSrCountLabel.set("0");
            roleBaseSsrList.clear();
        }else {
            roleBaseTimeLabel.set(data.getStartDate() +" - "+ data.getEndDate());
            roleBaseSsrNoUpLabel.set(String.valueOf(data.getNoUpSsrCount()));
            roleBaseSrNoUpLabel.set(String.valueOf(data.getNoUpSrCount()));
            roleBaseSsrAvgLabel.set(String.format(AVG_TEMPLATE,data.getSsrAvg()));
            roleBaseSsrCountLabel.set(String.format(PERCENT_TEMPLATE,data.getSsrCount(),(double)data.getSsrCount() / data.getTotalCount() * 100));
            roleBaseSrCountLabel.set(String.format(PERCENT_TEMPLATE,data.getSrCount(),(double)data.getSrCount() / data.getTotalCount() * 100));
            roleBaseSsrList.setAll(data.getSsrDataList());
        }

    }
    private void updateWeaponEvent(AnalysisData data){
        weaponEventTitleLabel.set(data.getPoolName());
        weaponEventTotalTimeLabel.set(String.valueOf(data.getTotalCount()));
        if (data.isEmpty()){
            weaponEventTimeLabel.set("No Data");
            weaponEventSsrNoUpLabel.set("0");
            weaponEventSrNoUpLabel.set("0");
            weaponEventSsrAvgLabel.set("0");
            weaponEventSsrCountLabel.set("0");
            weaponEventSrCountLabel.set("0");
            weaponEventSsrList.clear();
        }else {
            weaponEventTimeLabel.set(data.getStartDate() +" - "+ data.getEndDate());
            weaponEventSsrNoUpLabel.set(String.valueOf(data.getNoUpSsrCount()));
            weaponEventSrNoUpLabel.set(String.valueOf(data.getNoUpSrCount()));
            weaponEventSsrAvgLabel.set(String.format(AVG_TEMPLATE,data.getSsrAvg()));
            weaponEventSsrCountLabel.set(String.format(PERCENT_TEMPLATE,data.getSsrCount(),(double)data.getSsrCount() / data.getTotalCount() * 100));
            weaponEventSrCountLabel.set(String.format(PERCENT_TEMPLATE,data.getSrCount(),(double)data.getSrCount() / data.getTotalCount() * 100));
            weaponEventSsrList.setAll(data.getSsrDataList());
        }

    }
    private void updateWeaponBase(AnalysisData data){
        weaponBaseTitleLabel.set(data.getPoolName());
        weaponBaseTotalTimeLabel.set(String.valueOf(data.getTotalCount()));
        if (data.isEmpty()){
            weaponBaseTimeLabel.set("No Data");
            weaponBaseTotalTimeLabel.set("0");
            weaponBaseSsrNoUpLabel.set("0");
            weaponBaseSrNoUpLabel.set("0");
            weaponBaseSsrAvgLabel.set("0");
            weaponBaseSsrCountLabel.set("0");
            weaponBaseSrCountLabel.set("0");
            weaponBaseSsrList.clear();
        }else {
            weaponBaseTimeLabel.set(data.getStartDate() +" - "+ data.getEndDate());
            weaponBaseTotalTimeLabel.set(String.valueOf(data.getTotalCount()));
            weaponBaseSsrNoUpLabel.set(String.valueOf(data.getNoUpSsrCount()));
            weaponBaseSrNoUpLabel.set(String.valueOf(data.getNoUpSrCount()));
            weaponBaseSsrAvgLabel.set(String.format(AVG_TEMPLATE,data.getSsrAvg()));
            weaponBaseSsrCountLabel.set(String.format(PERCENT_TEMPLATE,data.getSsrCount(),(double)data.getSsrCount() / data.getTotalCount() * 100));
            weaponBaseSrCountLabel.set(String.format(PERCENT_TEMPLATE,data.getSrCount(),(double)data.getSrCount() / data.getTotalCount() * 100));
            weaponBaseSsrList.setAll(data.getSsrDataList());
        }
    }



    public String getPlayer() {
        return player.get();
    }

    public SimpleStringProperty playerProperty() {
        return player;
    }

    public ObservableList<SsrData> getWeaponEventSsrList() {
        return weaponEventSsrList;
    }

    public String getWeaponEventTotalTimeLabel() {
        return weaponEventTotalTimeLabel.get();
    }

    public SimpleStringProperty weaponEventTotalTimeLabelProperty() {
        return weaponEventTotalTimeLabel;
    }

    public String getWeaponEventTimeLabel() {
        return weaponEventTimeLabel.get();
    }

    public SimpleStringProperty weaponEventTimeLabelProperty() {
        return weaponEventTimeLabel;
    }

    public String getWeaponEventSsrNoUpLabel() {
        return weaponEventSsrNoUpLabel.get();
    }

    public SimpleStringProperty weaponEventSsrNoUpLabelProperty() {
        return weaponEventSsrNoUpLabel;
    }

    public String getWeaponEventSsrCountLabel() {
        return weaponEventSsrCountLabel.get();
    }

    public SimpleStringProperty weaponEventSsrCountLabelProperty() {
        return weaponEventSsrCountLabel;
    }

    public String getWeaponEventSsrAvgLabel() {
        return weaponEventSsrAvgLabel.get();
    }

    public SimpleStringProperty weaponEventSsrAvgLabelProperty() {
        return weaponEventSsrAvgLabel;
    }

    public String getWeaponEventSrNoUpLabel() {
        return weaponEventSrNoUpLabel.get();
    }

    public SimpleStringProperty weaponEventSrNoUpLabelProperty() {
        return weaponEventSrNoUpLabel;
    }

    public String getWeaponEventSrCountLabel() {
        return weaponEventSrCountLabel.get();
    }

    public SimpleStringProperty weaponEventSrCountLabelProperty() {
        return weaponEventSrCountLabel;
    }

    public String getWeaponEventTitleLabel() {
        return weaponEventTitleLabel.get();
    }

    public SimpleStringProperty weaponEventTitleLabelProperty() {
        return weaponEventTitleLabel;
    }

    public ObservableList<SsrData> getWeaponBaseSsrList() {
        return weaponBaseSsrList;
    }

    public String getWeaponBaseTotalTimeLabel() {
        return weaponBaseTotalTimeLabel.get();
    }

    public SimpleStringProperty weaponBaseTotalTimeLabelProperty() {
        return weaponBaseTotalTimeLabel;
    }

    public String getWeaponBaseTimeLabel() {
        return weaponBaseTimeLabel.get();
    }

    public SimpleStringProperty weaponBaseTimeLabelProperty() {
        return weaponBaseTimeLabel;
    }

    public String getWeaponBaseSsrNoUpLabel() {
        return weaponBaseSsrNoUpLabel.get();
    }

    public SimpleStringProperty weaponBaseSsrNoUpLabelProperty() {
        return weaponBaseSsrNoUpLabel;
    }

    public String getWeaponBaseSsrCountLabel() {
        return weaponBaseSsrCountLabel.get();
    }

    public SimpleStringProperty weaponBaseSsrCountLabelProperty() {
        return weaponBaseSsrCountLabel;
    }

    public String getWeaponBaseSsrAvgLabel() {
        return weaponBaseSsrAvgLabel.get();
    }

    public SimpleStringProperty weaponBaseSsrAvgLabelProperty() {
        return weaponBaseSsrAvgLabel;
    }

    public String getWeaponBaseSrNoUpLabel() {
        return weaponBaseSrNoUpLabel.get();
    }

    public SimpleStringProperty weaponBaseSrNoUpLabelProperty() {
        return weaponBaseSrNoUpLabel;
    }

    public String getWeaponBaseSrCountLabel() {
        return weaponBaseSrCountLabel.get();
    }

    public SimpleStringProperty weaponBaseSrCountLabelProperty() {
        return weaponBaseSrCountLabel;
    }

    public String getWeaponBaseTitleLabel() {
        return weaponBaseTitleLabel.get();
    }

    public SimpleStringProperty weaponBaseTitleLabelProperty() {
        return weaponBaseTitleLabel;
    }

    public ObservableList<SsrData> getRoleEventSsrList() {
        return roleEventSsrList;
    }

    public String getRoleEventTotalTimeLabel() {
        return roleEventTotalTimeLabel.get();
    }

    public SimpleStringProperty roleEventTotalTimeLabelProperty() {
        return roleEventTotalTimeLabel;
    }

    public String getRoleEventTimeLabel() {
        return roleEventTimeLabel.get();
    }

    public SimpleStringProperty roleEventTimeLabelProperty() {
        return roleEventTimeLabel;
    }

    public String getRoleEventSsrNoUpLabel() {
        return roleEventSsrNoUpLabel.get();
    }

    public SimpleStringProperty roleEventSsrNoUpLabelProperty() {
        return roleEventSsrNoUpLabel;
    }

    public String getRoleEventSsrCountLabel() {
        return roleEventSsrCountLabel.get();
    }

    public SimpleStringProperty roleEventSsrCountLabelProperty() {
        return roleEventSsrCountLabel;
    }

    public String getRoleEventSsrAvgLabel() {
        return roleEventSsrAvgLabel.get();
    }

    public SimpleStringProperty roleEventSsrAvgLabelProperty() {
        return roleEventSsrAvgLabel;
    }

    public String getRoleEventSrNoUpLabel() {
        return roleEventSrNoUpLabel.get();
    }

    public SimpleStringProperty roleEventSrNoUpLabelProperty() {
        return roleEventSrNoUpLabel;
    }

    public String getRoleEventSrCountLabel() {
        return roleEventSrCountLabel.get();
    }

    public SimpleStringProperty roleEventSrCountLabelProperty() {
        return roleEventSrCountLabel;
    }

    public String getRoleEventTitleLabel() {
        return roleEventTitleLabel.get();
    }

    public SimpleStringProperty roleEventTitleLabelProperty() {
        return roleEventTitleLabel;
    }

    public ObservableList<SsrData> getRoleBaseSsrList() {
        return roleBaseSsrList;
    }

    public String getRoleBaseTotalTimeLabel() {
        return roleBaseTotalTimeLabel.get();
    }

    public SimpleStringProperty roleBaseTotalTimeLabelProperty() {
        return roleBaseTotalTimeLabel;
    }

    public String getRoleBaseTimeLabel() {
        return roleBaseTimeLabel.get();
    }

    public SimpleStringProperty roleBaseTimeLabelProperty() {
        return roleBaseTimeLabel;
    }

    public String getRoleBaseSsrNoUpLabel() {
        return roleBaseSsrNoUpLabel.get();
    }

    public SimpleStringProperty roleBaseSsrNoUpLabelProperty() {
        return roleBaseSsrNoUpLabel;
    }

    public String getRoleBaseSsrCountLabel() {
        return roleBaseSsrCountLabel.get();
    }

    public SimpleStringProperty roleBaseSsrCountLabelProperty() {
        return roleBaseSsrCountLabel;
    }

    public String getRoleBaseSsrAvgLabel() {
        return roleBaseSsrAvgLabel.get();
    }

    public SimpleStringProperty roleBaseSsrAvgLabelProperty() {
        return roleBaseSsrAvgLabel;
    }

    public String getRoleBaseSrNoUpLabel() {
        return roleBaseSrNoUpLabel.get();
    }

    public SimpleStringProperty roleBaseSrNoUpLabelProperty() {
        return roleBaseSrNoUpLabel;
    }

    public String getRoleBaseSrCountLabel() {
        return roleBaseSrCountLabel.get();
    }

    public SimpleStringProperty roleBaseSrCountLabelProperty() {
        return roleBaseSrCountLabel;
    }

    public String getRoleBaseTitleLabel() {
        return roleBaseTitleLabel.get();
    }

    public SimpleStringProperty roleBaseTitleLabelProperty() {
        return roleBaseTitleLabel;
    }

    public boolean isEmpty() {
        return empty.get();
    }

    public SimpleBooleanProperty emptyProperty() {
        return empty;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public SimpleBooleanProperty loadingProperty() {
        return loading;
    }
}