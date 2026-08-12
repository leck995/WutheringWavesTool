package cn.tealc.wutheringwavestool.base.config;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.Locale;

/**
 * UI/界面相关配置：语言、主题、窗口尺寸、缩放、左侧栏、标题栏、启动页、自定义背景。
 * <p>对应原 Setting 的顶部 UI 字段与"基础设置"分段。
 */
public class UiSetting {
    private SimpleObjectProperty<Locale> language = new SimpleObjectProperty<>(Locale.CHINA);
    private SimpleDoubleProperty appWidth = new SimpleDoubleProperty(1280.0);
    private SimpleDoubleProperty appHeight = new SimpleDoubleProperty(760.0);
    private SimpleIntegerProperty uiScale = new SimpleIntegerProperty(100);
    private SimpleBooleanProperty leftBarShow = new SimpleBooleanProperty(true); //左侧菜单栏是否关闭
    private SimpleBooleanProperty theme = new SimpleBooleanProperty(false); //主题，false为亮色
    private SimpleBooleanProperty changeTitlebar = new SimpleBooleanProperty(true); //新标题栏
    private SimpleBooleanProperty firstViewWithPoolAnalysis = new SimpleBooleanProperty(false);//启动页设置为抽卡分析
    private SimpleBooleanProperty diyHomeBg = new SimpleBooleanProperty(false); //启用自定义背景
    private SimpleStringProperty diyHomeBgName = new SimpleStringProperty(); //自定义背景文件名称
    private SimpleIntegerProperty diyHomeBgType = new SimpleIntegerProperty(); // 0为默认，1为指定背景，2为背景文件夹
    private SimpleStringProperty diyHomeBgDir = new SimpleStringProperty();

    // ---------- language ----------
    public Locale getLanguage() { return language.get(); }
    public SimpleObjectProperty<Locale> languageProperty() { return language; }
    public void setLanguage(Locale language) { this.language.set(language); }

    // ---------- appWidth ----------
    public double getAppWidth() { return appWidth.get(); }
    public SimpleDoubleProperty appWidthProperty() { return appWidth; }
    public void setAppWidth(double appWidth) { this.appWidth.set(appWidth); }

    // ---------- appHeight ----------
    public double getAppHeight() { return appHeight.get(); }
    public SimpleDoubleProperty appHeightProperty() { return appHeight; }
    public void setAppHeight(double appHeight) { this.appHeight.set(appHeight); }

    // ---------- uiScale ----------
    public int getUiScale() { return uiScale.get(); }
    public SimpleIntegerProperty uiScaleProperty() { return uiScale; }
    public void setUiScale(int uiScale) { this.uiScale.set(uiScale); }

    // ---------- leftBarShow ----------
    public boolean isLeftBarShow() { return leftBarShow.get(); }
    public SimpleBooleanProperty leftBarShowProperty() { return leftBarShow; }
    public void setLeftBarShow(boolean leftBarShow) { this.leftBarShow.set(leftBarShow); }

    // ---------- theme ----------
    public boolean isTheme() { return theme.get(); }
    public SimpleBooleanProperty themeProperty() { return theme; }
    public void setTheme(boolean theme) { this.theme.set(theme); }

    // ---------- changeTitlebar ----------
    public boolean isChangeTitlebar() { return changeTitlebar.get(); }
    public SimpleBooleanProperty changeTitlebarProperty() { return changeTitlebar; }
    public void setChangeTitlebar(boolean changeTitlebar) { this.changeTitlebar.set(changeTitlebar); }

    // ---------- firstViewWithPoolAnalysis ----------
    public boolean isFirstViewWithPoolAnalysis() { return firstViewWithPoolAnalysis.get(); }
    public SimpleBooleanProperty firstViewWithPoolAnalysisProperty() { return firstViewWithPoolAnalysis; }
    public void setFirstViewWithPoolAnalysis(boolean firstViewWithPoolAnalysis) { this.firstViewWithPoolAnalysis.set(firstViewWithPoolAnalysis); }

    // ---------- diyHomeBg ----------
    public boolean isDiyHomeBg() { return diyHomeBg.get(); }
    public SimpleBooleanProperty diyHomeBgProperty() { return diyHomeBg; }
    public void setDiyHomeBg(boolean diyHomeBg) { this.diyHomeBg.set(diyHomeBg); }

    // ---------- diyHomeBgName ----------
    public String getDiyHomeBgName() { return diyHomeBgName.get(); }
    public SimpleStringProperty diyHomeBgNameProperty() { return diyHomeBgName; }
    public void setDiyHomeBgName(String diyHomeBgName) { this.diyHomeBgName.set(diyHomeBgName); }

    // ---------- diyHomeBgType ----------
    public int getDiyHomeBgType() { return diyHomeBgType.get(); }
    public SimpleIntegerProperty diyHomeBgTypeProperty() { return diyHomeBgType; }
    public void setDiyHomeBgType(int diyHomeBgType) { this.diyHomeBgType.set(diyHomeBgType); }

    // ---------- diyHomeBgDir ----------
    public String getDiyHomeBgDir() { return diyHomeBgDir.get(); }
    public SimpleStringProperty diyHomeBgDirProperty() { return diyHomeBgDir; }
    public void setDiyHomeBgDir(String diyHomeBgDir) { this.diyHomeBgDir.set(diyHomeBgDir); }
}
