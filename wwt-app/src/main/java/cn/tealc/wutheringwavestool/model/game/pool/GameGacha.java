package cn.tealc.wutheringwavestool.model.game.pool;

/**
 * 抽卡记录实体，对应 game_gacha 表
 *
 * @author leck
 * @date 2026/05/31
 */
public class GameGacha {
    private int id;
    private String playerId;
    private String gachaName;
    private String cardPoolType;
    private int resourceId;
    private int qualityLevel;
    private String resourceType;
    private String name;
    private int count;
    private long time;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getGachaName() {
        return gachaName;
    }

    public void setGachaName(String gachaName) {
        this.gachaName = gachaName;
    }

    public String getCardPoolType() {
        return cardPoolType;
    }

    public void setCardPoolType(String cardPoolType) {
        this.cardPoolType = cardPoolType;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getQualityLevel() {
        return qualityLevel;
    }

    public void setQualityLevel(int qualityLevel) {
        this.qualityLevel = qualityLevel;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
