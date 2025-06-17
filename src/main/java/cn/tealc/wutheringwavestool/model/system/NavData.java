package cn.tealc.wutheringwavestool.model.system;

/**
 * @description: 导航菜单数据
 * @author: Leck
 * @create: 2025-06-17 17:33
 */
public class NavData {
    private int id;
    private String title; //标题,key值
    private String icon; //ikonli
    private boolean visible;//可见性
    private int order; //顺序
    private boolean kujiequ;
    private String viewClass;
    private String viewModelClass;
    private boolean fxml;
    private boolean bottom;//是否在下方布局
    private boolean diy; //是否允许自定义
    private boolean showBg; //是否显示背景
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isKujiequ() {
        return kujiequ;
    }

    public void setKujiequ(boolean kujiequ) {
        this.kujiequ = kujiequ;
    }

    public String getViewClass() {
        return viewClass;
    }

    public void setViewClass(String viewClass) {
        this.viewClass = viewClass;
    }

    public String getViewModelClass() {
        return viewModelClass;
    }

    public void setViewModelClass(String viewModelClass) {
        this.viewModelClass = viewModelClass;
    }

    public boolean isFxml() {
        return fxml;
    }

    public void setFxml(boolean fxml) {
        this.fxml = fxml;
    }

    public boolean isBottom() {
        return bottom;
    }

    public void setBottom(boolean bottom) {
        this.bottom = bottom;
    }

    public boolean isShowBg() {
        return showBg;
    }

    public void setShowBg(boolean showBg) {
        this.showBg = showBg;
    }

    public boolean isDiy() {
        return diy;
    }

    public void setDiy(boolean diy) {
        this.diy = diy;
    }
}