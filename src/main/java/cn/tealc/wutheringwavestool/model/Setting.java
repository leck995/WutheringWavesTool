package cn.tealc.wutheringwavestool.model;

import ch.qos.logback.classic.Level;
import cn.tealc.wutheringwavestool.base.Config;
import javafx.beans.property.*;

import java.util.Locale;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 00:38
 */
public class Setting {

    private SimpleObjectProperty<Locale> language = new SimpleObjectProperty<>(Locale.getDefault());
    private SimpleDoubleProperty appWidth=new SimpleDoubleProperty(1280.0);
    private SimpleDoubleProperty appHeight=new SimpleDoubleProperty(760.0);

    private SimpleBooleanProperty leftBarShow=new SimpleBooleanProperty(true); //左侧菜单栏是否关闭
    private SimpleBooleanProperty theme=new SimpleBooleanProperty(false); //主题，false为亮色
    private SimpleBooleanProperty support=new SimpleBooleanProperty(false);  //标志是否赞助

    private SimpleStringProperty homeViewIcon=new SimpleStringProperty();  //主页头像
    private SimpleStringProperty homeViewRole=new SimpleStringProperty(); //主页人物
    private SimpleStringProperty logLevel=new SimpleStringProperty("INFO"); //日志等级


    /*=================设置-首选===================*/
    private SimpleObjectProperty<SourceType> gameRootDirSource = new SimpleObjectProperty<>(SourceType.DEFAULT); //游戏来源类型
    private SimpleStringProperty gameRootDir=new SimpleStringProperty();//游戏根目录
    private SimpleStringProperty gameStarAppPath=new SimpleStringProperty("Wuthering Waves.exe");//游戏启动文件
    private SimpleBooleanProperty gameStartAppCustom = new SimpleBooleanProperty(false); //自定义启动程序

    /*=================设置-基础设置===================*/
    private SimpleBooleanProperty changeTitlebar=new SimpleBooleanProperty(true); //新标题栏
    private SimpleBooleanProperty firstViewWithPoolAnalysis=new SimpleBooleanProperty(false);//启动页设置为抽卡分析
    private SimpleBooleanProperty diyHomeBg=new SimpleBooleanProperty(false); //启用自定义背景
    private SimpleStringProperty diyHomeBgName=new SimpleStringProperty(); //自定义背景文件名称
    private SimpleBooleanProperty noKuJieQu = new SimpleBooleanProperty(getLanguage() != Locale.CHINA); //不使用库街区
    private SimpleIntegerProperty closeEvent = new SimpleIntegerProperty(0); //关闭主界面行为，0选择，1退出，2最小化
    /*=================设置-游戏行为===================*/
    private SimpleBooleanProperty exitWhenGameOver=new SimpleBooleanProperty(false); //检测到游戏关闭自动关闭程序
    private SimpleBooleanProperty hideWhenGameStart=new SimpleBooleanProperty(false); //检测到游戏启动自动隐藏程序至托盘

    /*=================设置-其他设置===================*/
    private SimpleBooleanProperty checkNewVersion=new SimpleBooleanProperty(true); //检查更新

    /*=============资源库=============*/
    private SimpleIntegerProperty resourceSource = new SimpleIntegerProperty(getLanguage() == Locale.CHINA ? 1 : 0); //0代表Github，1代表码云

    /*=================高级启动相关===================*/
    private SimpleBooleanProperty userAdvanceGameSettings=new SimpleBooleanProperty(false); //使用高级启动
    private SimpleStringProperty  appParams = new SimpleStringProperty(); //启动参数

    /*=================签到相关===================*/
    private SimpleBooleanProperty autoKujieQuSign=new SimpleBooleanProperty(false); //使用高级启动


    public Locale getLanguage() {
        return language.get();
    }

    public SimpleObjectProperty<Locale> languageProperty() {
        return language;
    }

    public void setLanguage(Locale language) {
        this.language.set(language);
    }

    public String getGameRootDir() {
        return gameRootDir.get();
    }

    public SimpleStringProperty gameRootDirProperty() {
        return gameRootDir;
    }

    public void setGameRootDir(String gameRootDir) {
        this.gameRootDir.set(gameRootDir);
    }

    public boolean isFirstViewWithPoolAnalysis() {
        return firstViewWithPoolAnalysis.get();
    }

    public SimpleBooleanProperty firstViewWithPoolAnalysisProperty() {
        return firstViewWithPoolAnalysis;
    }

    public void setFirstViewWithPoolAnalysis(boolean firstViewWithPoolAnalysis) {
        this.firstViewWithPoolAnalysis.set(firstViewWithPoolAnalysis);
    }


    public boolean isChangeTitlebar() {
        return changeTitlebar.get();
    }

    public SimpleBooleanProperty changeTitlebarProperty() {
        return changeTitlebar;
    }

    public void setChangeTitlebar(boolean changeTitlebar) {
        this.changeTitlebar.set(changeTitlebar);
    }

    public boolean isTheme() {
        return theme.get();
    }

    public SimpleBooleanProperty themeProperty() {
        return theme;
    }

    public void setTheme(boolean theme) {
        this.theme.set(theme);
    }

    public String getHomeViewIcon() {
        return homeViewIcon.get();
    }

    public SimpleStringProperty homeViewIconProperty() {
        return homeViewIcon;
    }

    public void setHomeViewIcon(String homeViewIcon) {
        this.homeViewIcon.set(homeViewIcon);
    }

    public String getHomeViewRole() {
        return homeViewRole.get();
    }

    public SimpleStringProperty homeViewRoleProperty() {
        return homeViewRole;
    }

    public void setHomeViewRole(String homeViewRole) {
        this.homeViewRole.set(homeViewRole);
    }

    public boolean isExitWhenGameOver() {
        return exitWhenGameOver.get();
    }

    public SimpleBooleanProperty exitWhenGameOverProperty() {
        return exitWhenGameOver;
    }

    public void setExitWhenGameOver(boolean exitWhenGameOver) {
        this.exitWhenGameOver.set(exitWhenGameOver);
    }

    public boolean isHideWhenGameStart() {
        return hideWhenGameStart.get();
    }

    public SimpleBooleanProperty hideWhenGameStartProperty() {
        return hideWhenGameStart;
    }

    public void setHideWhenGameStart(boolean hideWhenGameStart) {
        this.hideWhenGameStart.set(hideWhenGameStart);
    }

    public boolean isDiyHomeBg() {
        return diyHomeBg.get();
    }

    public SimpleBooleanProperty diyHomeBgProperty() {
        return diyHomeBg;
    }

    public void setDiyHomeBg(boolean diyHomeBg) {
        this.diyHomeBg.set(diyHomeBg);
    }

    public String getDiyHomeBgName() {
        return diyHomeBgName.get();
    }

    public SimpleStringProperty diyHomeBgNameProperty() {
        return diyHomeBgName;
    }

    public void setDiyHomeBgName(String diyHomeBgName) {
        this.diyHomeBgName.set(diyHomeBgName);
    }

    public double getAppWidth() {
        return appWidth.get();
    }

    public SimpleDoubleProperty appWidthProperty() {
        return appWidth;
    }

    public void setAppWidth(double appWidth) {
        this.appWidth.set(appWidth);
    }

    public double getAppHeight() {
        return appHeight.get();
    }

    public SimpleDoubleProperty appHeightProperty() {
        return appHeight;
    }

    public void setAppHeight(double appHeight) {
        this.appHeight.set(appHeight);
    }

    public SourceType getGameRootDirSource() {
        return gameRootDirSource.get();
    }

    public SimpleObjectProperty<SourceType> gameRootDirSourceProperty() {
        return gameRootDirSource;
    }

    public void setGameRootDirSource(SourceType gameRootDirSource) {
        this.gameRootDirSource.set(gameRootDirSource);
    }

    public String getLogLevel() {
        return logLevel.get();
    }

    public SimpleStringProperty logLevelProperty() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel.set(logLevel);
    }

    public boolean isCheckNewVersion() {
        return checkNewVersion.get();
    }

    public SimpleBooleanProperty checkNewVersionProperty() {
        return checkNewVersion;
    }

    public void setCheckNewVersion(boolean checkNewVersion) {
        this.checkNewVersion.set(checkNewVersion);
    }

    public boolean isLeftBarShow() {
        return leftBarShow.get();
    }

    public SimpleBooleanProperty leftBarShowProperty() {
        return leftBarShow;
    }

    public void setLeftBarShow(boolean leftBarShow) {
        this.leftBarShow.set(leftBarShow);
    }

    public int getResourceSource() {
        return resourceSource.get();
    }

    public SimpleIntegerProperty resourceSourceProperty() {
        return resourceSource;
    }

    public void setResourceSource(int resourceSource) {
        this.resourceSource.set(resourceSource);
    }

    public String getGameStarAppPath() {
        return gameStarAppPath.get();
    }

    public SimpleStringProperty gameStarAppPathProperty() {
        return gameStarAppPath;
    }

    public void setGameStarAppPath(String gameStarAppPath) {
        this.gameStarAppPath.set(gameStarAppPath);
    }

    public boolean isGameStartAppCustom() {
        return gameStartAppCustom.get();
    }

    public SimpleBooleanProperty gameStartAppCustomProperty() {
        return gameStartAppCustom;
    }

    public void setGameStartAppCustom(boolean gameStartAppCustom) {
        this.gameStartAppCustom.set(gameStartAppCustom);
    }

    public boolean isNoKuJieQu() {
        return noKuJieQu.get();
    }

    public SimpleBooleanProperty noKuJieQuProperty() {
        return noKuJieQu;
    }

    public void setNoKuJieQu(boolean noKuJieQu) {
        this.noKuJieQu.set(noKuJieQu);
    }

    public boolean isSupport() {
        return support.get();
    }

    public SimpleBooleanProperty supportProperty() {
        return support;
    }

    public void setSupport(boolean support) {
        this.support.set(support);
    }

    public boolean isUserAdvanceGameSettings() {
        return userAdvanceGameSettings.get();
    }

    public SimpleBooleanProperty userAdvanceGameSettingsProperty() {
        return userAdvanceGameSettings;
    }

    public void setUserAdvanceGameSettings(boolean userAdvanceGameSettings) {
        this.userAdvanceGameSettings.set(userAdvanceGameSettings);
    }

    public String getAppParams() {
        return appParams.get();
    }

    public SimpleStringProperty appParamsProperty() {
        return appParams;
    }

    public void setAppParams(String appParams) {
        this.appParams.set(appParams);
    }

    public int getCloseEvent() {
        return closeEvent.get();
    }

    public SimpleIntegerProperty closeEventProperty() {
        return closeEvent;
    }

    public void setCloseEvent(int closeEvent) {
        this.closeEvent.set(closeEvent);
    }

    public boolean isAutoKujieQuSign() {
        return autoKujieQuSign.get();
    }

    public SimpleBooleanProperty autoKujieQuSignProperty() {
        return autoKujieQuSign;
    }

    public void setAutoKujieQuSign(boolean autoKujieQuSign) {
        this.autoKujieQuSign.set(autoKujieQuSign);
    }
}