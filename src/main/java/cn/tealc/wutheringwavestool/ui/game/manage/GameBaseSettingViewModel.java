package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.thread.system.CheckGameConfigTask;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.SceneLifecycle;
import de.saxsys.mvvmfx.ViewModel;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;

import java.io.File;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-08 23:11
 */
public class GameBaseSettingViewModel implements ViewModel, SceneLifecycle {
    private SimpleObjectProperty<SourceType> gameSourceType = new SimpleObjectProperty<>();
    private SimpleStringProperty gameDir=new SimpleStringProperty();
    private SimpleStringProperty gameAppStartPath=new SimpleStringProperty();
    private SimpleBooleanProperty gameAppStartCustom=new SimpleBooleanProperty();
    private SimpleBooleanProperty sourceTypeDisabled01=new SimpleBooleanProperty(true);
    private SimpleBooleanProperty sourceTypeDisabled02=new SimpleBooleanProperty(true);
    private SimpleBooleanProperty sourceTypeDisabled03=new SimpleBooleanProperty(true);
    private SimpleBooleanProperty sourceTypeDisabled04=new SimpleBooleanProperty(true);


    private final ObservableList<String> startUpParams;
    public GameBaseSettingViewModel() {
        startUpParams = Config.setting().getStartUpParams();
    }



    public void init() {
        gameSourceType.bindBidirectional(Config.setting().gameRootDirSourceProperty());
        gameDir.bindBidirectional(Config.setting().gameRootDirProperty());
        gameAppStartPath.bindBidirectional(Config.setting().gameStarAppPathProperty());
        gameAppStartCustom.bindBidirectional(Config.setting().gameStartAppCustomProperty());

        checkServerExist();
    }








    /**
     * 删除指定启动参数
     * @param index
     */
    public void deleteParam(int index) {
        startUpParams.remove(index);
    }

    /**
     * 添加启动参数
     * @param param
     */
    public void addParam(String param) {
        startUpParams.add(param);
    }

    public boolean isDx11(){
        return startUpParams.contains("-dx11");
    }
    public boolean isDx12(){
        return startUpParams.contains("-dx12");
    }

    /**
     * 启动参数中添加dx11
     */
    public void addDx11(){
        int index = startUpParams.indexOf("-dx12");
        if (index != -1){
            startUpParams.set(index,"-dx11");
        }else {
            startUpParams.add("-dx11");
        }
    }

    /**
     * 启动参数中添加dx12
     */
    public void addDx12(){
        int index = startUpParams.indexOf("-dx11");
        if (index != -1){
            startUpParams.set(index,"-dx12");
        }else {
            startUpParams.add("-dx12");
        }
    }

    public void replaceParam(String param1, String param2) {

    }












    private void checkServerExist(){
        sourceTypeDisabled01.set(gameSourceType.get() != SourceType.DEFAULT);
        sourceTypeDisabled02.set(gameSourceType.get() != SourceType.BILIBILI);
        sourceTypeDisabled03.set(gameSourceType.get() != SourceType.WE_GAME);
        sourceTypeDisabled04.set(gameSourceType.get() != SourceType.GLOBAL);

        File dir = new File(gameDir.get() + File.separator + "servers");
        File defaultServerFile = new File(dir,"default");
        File bilibiliServerFile = new File(dir,"bilibili");
        File wegameServerFile = new File(dir,"wegame");
        File globalServerFile = new File(dir,"global");
        if (!sourceTypeDisabled01.get() && defaultServerFile.exists()) {
            sourceTypeDisabled01.set(false);
        }
        if (!sourceTypeDisabled02.get() && bilibiliServerFile.exists()) {
            sourceTypeDisabled02.set(false);
        }
        if (!sourceTypeDisabled03.get() && wegameServerFile.exists()) {
            sourceTypeDisabled03.set(false);
        }
        if (!sourceTypeDisabled04.get() && globalServerFile.exists()) {
            sourceTypeDisabled04.set(false);
        }
    }

    @Override
    public void onViewAdded() {

    }

    @Override
    public void onViewRemoved() {
        checkGameLogOpen();
        Config.setting().save();
    }
    /**
     * description: 检测游戏日志是否被关闭
     */
    private void checkGameLogOpen() {
        CheckGameConfigTask task = new CheckGameConfigTask();
        task.setOnSucceeded(workerStateEvent -> {
            Boolean value = task.getValue();
            if (!value) { //游戏日志可能被关闭了
                Platform.runLater(() -> {
                    NotificationManager.message(MessageInfo.success(LanguageManager.getString("ui.main.sync.message.log.close")));
                });
            }
        });
        task.setOnFailed(workerStateEvent -> {
            System.err.println("检测游戏日志状态失败: " + workerStateEvent.getSource().getException());
        });
        Thread.startVirtualThread(task);
    }

    public boolean changeServer(SourceType sourceType) {
        return false;
    }



    public SourceType getGameSourceType() {
        return gameSourceType.get();
    }

    public SimpleObjectProperty<SourceType> gameSourceTypeProperty() {
        return gameSourceType;
    }

    public String getGameDir() {
        return gameDir.get();
    }

    public SimpleStringProperty gameDirProperty() {
        return gameDir;
    }

    public boolean isSourceTypeDisabled01() {
        return sourceTypeDisabled01.get();
    }

    public SimpleBooleanProperty sourceTypeDisabled01Property() {
        return sourceTypeDisabled01;
    }

    public boolean isSourceTypeDisabled02() {
        return sourceTypeDisabled02.get();
    }

    public SimpleBooleanProperty sourceTypeDisabled02Property() {
        return sourceTypeDisabled02;
    }

    public boolean isSourceTypeDisabled03() {
        return sourceTypeDisabled03.get();
    }

    public SimpleBooleanProperty sourceTypeDisabled03Property() {
        return sourceTypeDisabled03;
    }

    public boolean isSourceTypeDisabled04() {
        return sourceTypeDisabled04.get();
    }

    public SimpleBooleanProperty sourceTypeDisabled04Property() {
        return sourceTypeDisabled04;
    }

    public String getGameAppStartPath() {
        return gameAppStartPath.get();
    }

    public SimpleStringProperty gameAppStartPathProperty() {
        return gameAppStartPath;
    }

    public boolean isGameAppStartCustom() {
        return gameAppStartCustom.get();
    }

    public SimpleBooleanProperty gameAppStartCustomProperty() {
        return gameAppStartCustom;
    }

    public ObservableList<String> getStartUpParams() {
        return startUpParams;
    }




}