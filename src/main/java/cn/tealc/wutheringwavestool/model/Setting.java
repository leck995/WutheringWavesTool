package cn.tealc.wutheringwavestool.model;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 00:38
 */
public class Setting {
    private SimpleBooleanProperty theme=new SimpleBooleanProperty(false); //主题，false为亮色
    private SimpleStringProperty homeViewIcon=new SimpleStringProperty();  //主页头像
    private SimpleStringProperty homeViewRole=new SimpleStringProperty(); //主页人物
    private SimpleBooleanProperty exitWhenGameOver=new SimpleBooleanProperty(false); //检测到游戏关闭自动关闭程序
    public SimpleStringProperty gameRootDir=new SimpleStringProperty();//游戏根目录
    private SimpleBooleanProperty firstViewWithPoolAnalysis=new SimpleBooleanProperty(false);//启动页设置为抽卡分析
    private SimpleBooleanProperty hideWhenGameStart=new SimpleBooleanProperty(false); //检测到游戏启动自动隐藏程序至托盘
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
}