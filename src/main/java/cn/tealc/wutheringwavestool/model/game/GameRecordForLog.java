package cn.tealc.wutheringwavestool.model.game;


public class GameRecordForLog extends GameRecord {
    private Long startTime;
    private Long closeTime; //下线时间


    public Long getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(Long closeTime) {
        this.closeTime = closeTime;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }
}