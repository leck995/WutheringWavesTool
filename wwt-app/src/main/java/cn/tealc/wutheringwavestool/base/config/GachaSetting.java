package cn.tealc.wutheringwavestool.base.config;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * 抽卡分析相关配置：当前玩家 ID、抽卡界面显示模式。
 * <p>对应原 Setting 的"抽卡分析相关"分段。
 */
public class GachaSetting {
    private SimpleStringProperty gachaCurrentPlayerId = new SimpleStringProperty(); //当前玩家
    private SimpleBooleanProperty gachaListModel = new SimpleBooleanProperty(false); //抽卡界面显示模式

    // ---------- gachaCurrentPlayerId ----------
    public String getGachaCurrentPlayerId() { return gachaCurrentPlayerId.get(); }
    public SimpleStringProperty gachaCurrentPlayerIdProperty() { return gachaCurrentPlayerId; }
    public void setGachaCurrentPlayerId(String gachaCurrentPlayerId) { this.gachaCurrentPlayerId.set(gachaCurrentPlayerId); }

    // ---------- gachaListModel ----------
    public boolean isGachaListModel() { return gachaListModel.get(); }
    public SimpleBooleanProperty gachaListModelProperty() { return gachaListModel; }
    public void setGachaListModel(boolean gachaListModel) { this.gachaListModel.set(gachaListModel); }
}
