package cn.tealc.wutheringwavestool.model.roleData;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-30 16:23
 */
public class Phantom {

    private PhantomProp phantomProp;
    private int cost;
    private int quality;
    private int level;
    private FetterDetail fetterDetail;

    public PhantomProp getPhantomProp() {
        return phantomProp;
    }

    public void setPhantomProp(PhantomProp phantomProp) {
        this.phantomProp = phantomProp;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public FetterDetail getFetterDetail() {
        return fetterDetail;
    }

    public void setFetterDetail(FetterDetail fetterDetail) {
        this.fetterDetail = fetterDetail;
    }
}