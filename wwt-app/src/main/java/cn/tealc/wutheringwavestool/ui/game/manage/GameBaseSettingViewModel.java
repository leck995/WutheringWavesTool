package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.thread.system.CheckGameConfigTask;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.SceneLifecycle;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-08 23:11
 */
public class GameBaseSettingViewModel extends BaseViewModel implements SceneLifecycle {
    private SimpleStringProperty gameDir=new SimpleStringProperty();
    private SimpleStringProperty gameAppStartPath=new SimpleStringProperty();
    private SimpleBooleanProperty gameAppStartCustom=new SimpleBooleanProperty();
    private final SimpleStringProperty currentServer = new SimpleStringProperty("-");


    private final ObservableList<String> startUpParams;

    public GameBaseSettingViewModel() {
        startUpParams = Config.setting().getStartUpParams();
    }



    public void init() {
        gameDir.bindBidirectional(Config.setting().gameRootDirProperty());
        gameAppStartPath.bindBidirectional(Config.setting().gameStarAppPathProperty());
        gameAppStartCustom.bindBidirectional(Config.setting().gameStartAppCustomProperty());

        currentServer.set(serverDisplayName(Config.setting().getGameRootDirSource()));
        Config.setting().gameRootDirSourceProperty().addListener(
                (observable, oldSource, newSource) -> currentServer.set(serverDisplayName(newSource)));
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














    @Override
    public void onViewAdded() {
        // 安装页仅负责安装配置，服务器切换由资源管理页负责。
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

    private String serverDisplayName(SourceType source) {
        if (source == null) {
            return "-";
        }
        return switch (source) {
            case DEFAULT -> LanguageManager.getString("ui.game_manager.base.server_switch.mainland");
            case BILIBILI -> LanguageManager.getString("ui.game_manager.base.server_switch.bilibili");
            case GLOBAL -> LanguageManager.getString("ui.game_manager.asset.server_global");
            case WE_GAME -> "WeGame";
        };
    }

    public SimpleStringProperty currentServerProperty() {
        return currentServer;
    }

    public String getGameDir() {
        return gameDir.get();
    }

    public SimpleStringProperty gameDirProperty() {
        return gameDir;
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
