package cn.tealc.wutheringwavestool.base;

import cn.tealc.wutheringwavestool.base.config.AppBehaviorSetting;
import cn.tealc.wutheringwavestool.base.config.GachaSetting;
import cn.tealc.wutheringwavestool.base.config.GameSetting;
import cn.tealc.wutheringwavestool.base.config.LauncherSetting;
import cn.tealc.wutheringwavestool.base.config.ServerSetting;
import cn.tealc.wutheringwavestool.base.config.SignSetting;
import cn.tealc.wutheringwavestool.base.config.UiSetting;
import cn.tealc.wutheringwavestool.base.config.serializer.ObservableListDeserializer;
import cn.tealc.wutheringwavestool.base.config.serializer.ObservableListSerializer;
import cn.tealc.wutheringwavestool.model.SourceType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * 应用配置聚合容器。
 * <p>实际字段分散在 7 个分组配置类中（{@link UiSetting}、{@link GameSetting}、
 * {@link LauncherSetting}、{@link GachaSetting}、{@link SignSetting}、
 * {@link ServerSetting}、{@link AppBehaviorSetting}），本类通过 {@code @JsonUnwrapped}
 * 将它们打平为扁平 JSON，与历史 settings.json 格式保持兼容。
 * <p>所有 getter/setter/xxxProperty 方法为 delegate 转发，保证现有 152 处
 * {@code Config.setting().xxx()} 调用零改动。
 *
 * @program: WutheringWavesTool
 * @author: Leck
 * @create: 2024-07-03 00:38
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
public class Setting {

    @JsonUnwrapped private final UiSetting ui = new UiSetting();
    @JsonUnwrapped private final GameSetting game = new GameSetting();
    @JsonUnwrapped private final LauncherSetting launcher = new LauncherSetting();
    @JsonUnwrapped private final GachaSetting gacha = new GachaSetting();
    @JsonUnwrapped private final SignSetting sign = new SignSetting();
    @JsonUnwrapped private final ServerSetting server = new ServerSetting();
    @JsonUnwrapped private final AppBehaviorSetting behavior = new AppBehaviorSetting();

    /**
     * 启动参数列表。
     * <p>使用自定义 Jackson 序列化器，与历史 settings.json 格式保持一致。
     * <p>保留在 Setting 顶层而非 {@link LauncherSetting} 中，是因为
     * {@code @JsonUnwrapped} 场景下字段级 {@code @JsonDeserialize(using=...)}
     * 不生效，会导致 {@link ObservableList} 接口类型无法反序列化。
     */
    @JsonSerialize(using = ObservableListSerializer.class)
    @JsonDeserialize(using = ObservableListDeserializer.class)
    private ObservableList<String> startUpParams = FXCollections.observableArrayList();

    public Setting() {
        // noKuJieQu / resourceSource 的默认值依赖 language，与原字段声明处逻辑保持一致
        behavior.setNoKuJieQu(ui.getLanguage() != Locale.CHINA);
        behavior.setResourceSource(ui.getLanguage() == Locale.CHINA ? 1 : 0);
    }

    // ============ 分组访问器 ============
    public UiSetting getUi() { return ui; }
    public GameSetting getGame() { return game; }
    public LauncherSetting getLauncher() { return launcher; }
    public GachaSetting getGacha() { return gacha; }
    public SignSetting getSign() { return sign; }
    public ServerSetting getServer() { return server; }
    public AppBehaviorSetting getBehavior() { return behavior; }

    // ============ UiSetting delegate ============
    public Locale getLanguage() { return ui.getLanguage(); }
    public SimpleObjectProperty<Locale> languageProperty() { return ui.languageProperty(); }
    public void setLanguage(Locale language) { ui.setLanguage(language); }

    public double getAppWidth() { return ui.getAppWidth(); }
    public SimpleDoubleProperty appWidthProperty() { return ui.appWidthProperty(); }
    public void setAppWidth(double appWidth) { ui.setAppWidth(appWidth); }

    public double getAppHeight() { return ui.getAppHeight(); }
    public SimpleDoubleProperty appHeightProperty() { return ui.appHeightProperty(); }
    public void setAppHeight(double appHeight) { ui.setAppHeight(appHeight); }

    public int getUiScale() { return ui.getUiScale(); }
    public SimpleIntegerProperty uiScaleProperty() { return ui.uiScaleProperty(); }
    public void setUiScale(int uiScale) { ui.setUiScale(uiScale); }

    public boolean isLeftBarShow() { return ui.isLeftBarShow(); }
    public SimpleBooleanProperty leftBarShowProperty() { return ui.leftBarShowProperty(); }
    public void setLeftBarShow(boolean leftBarShow) { ui.setLeftBarShow(leftBarShow); }

    public boolean isTheme() { return ui.isTheme(); }
    public SimpleBooleanProperty themeProperty() { return ui.themeProperty(); }
    public void setTheme(boolean theme) { ui.setTheme(theme); }

    public boolean isChangeTitlebar() { return ui.isChangeTitlebar(); }
    public SimpleBooleanProperty changeTitlebarProperty() { return ui.changeTitlebarProperty(); }
    public void setChangeTitlebar(boolean changeTitlebar) { ui.setChangeTitlebar(changeTitlebar); }

    public boolean isFirstViewWithPoolAnalysis() { return ui.isFirstViewWithPoolAnalysis(); }
    public SimpleBooleanProperty firstViewWithPoolAnalysisProperty() { return ui.firstViewWithPoolAnalysisProperty(); }
    public void setFirstViewWithPoolAnalysis(boolean firstViewWithPoolAnalysis) { ui.setFirstViewWithPoolAnalysis(firstViewWithPoolAnalysis); }

    public boolean isDiyHomeBg() { return ui.isDiyHomeBg(); }
    public SimpleBooleanProperty diyHomeBgProperty() { return ui.diyHomeBgProperty(); }
    public void setDiyHomeBg(boolean diyHomeBg) { ui.setDiyHomeBg(diyHomeBg); }

    public String getDiyHomeBgName() { return ui.getDiyHomeBgName(); }
    public SimpleStringProperty diyHomeBgNameProperty() { return ui.diyHomeBgNameProperty(); }
    public void setDiyHomeBgName(String diyHomeBgName) { ui.setDiyHomeBgName(diyHomeBgName); }

    public int getDiyHomeBgType() { return ui.getDiyHomeBgType(); }
    public SimpleIntegerProperty diyHomeBgTypeProperty() { return ui.diyHomeBgTypeProperty(); }
    public void setDiyHomeBgType(int diyHomeBgType) { ui.setDiyHomeBgType(diyHomeBgType); }

    public String getDiyHomeBgDir() { return ui.getDiyHomeBgDir(); }
    public SimpleStringProperty diyHomeBgDirProperty() { return ui.diyHomeBgDirProperty(); }
    public void setDiyHomeBgDir(String diyHomeBgDir) { ui.setDiyHomeBgDir(diyHomeBgDir); }

    // ============ GameSetting delegate ============
    public SourceType getGameRootDirSource() { return game.getGameRootDirSource(); }
    public SimpleObjectProperty<SourceType> gameRootDirSourceProperty() { return game.gameRootDirSourceProperty(); }
    public void setGameRootDirSource(SourceType gameRootDirSource) { game.setGameRootDirSource(gameRootDirSource); }

    public String getGameRootDir() { return game.getGameRootDir(); }
    public SimpleStringProperty gameRootDirProperty() { return game.gameRootDirProperty(); }
    public void setGameRootDir(String gameRootDir) { game.setGameRootDir(gameRootDir); }

    public String getGameStarAppPath() { return game.getGameStarAppPath(); }
    public SimpleStringProperty gameStarAppPathProperty() { return game.gameStarAppPathProperty(); }
    public void setGameStarAppPath(String gameStarAppPath) { game.setGameStarAppPath(gameStarAppPath); }

    public boolean isGameStartAppCustom() { return game.isGameStartAppCustom(); }
    public SimpleBooleanProperty gameStartAppCustomProperty() { return game.gameStartAppCustomProperty(); }
    public void setGameStartAppCustom(boolean gameStartAppCustom) { game.setGameStartAppCustom(gameStartAppCustom); }

    public String getGameOfficialLauncherDir() { return game.getGameOfficialLauncherDir(); }
    public SimpleStringProperty gameOfficialLauncherDirProperty() { return game.gameOfficialLauncherDirProperty(); }
    public void setGameOfficialLauncherDir(String gameOfficialLauncherDir) { game.setGameOfficialLauncherDir(gameOfficialLauncherDir); }

    public boolean isExitWhenGameOver() { return game.isExitWhenGameOver(); }
    public SimpleBooleanProperty exitWhenGameOverProperty() { return game.exitWhenGameOverProperty(); }
    public void setExitWhenGameOver(boolean exitWhenGameOver) { game.setExitWhenGameOver(exitWhenGameOver); }

    public boolean isHideWhenGameStart() { return game.isHideWhenGameStart(); }
    public SimpleBooleanProperty hideWhenGameStartProperty() { return game.hideWhenGameStartProperty(); }
    public void setHideWhenGameStart(boolean hideWhenGameStart) { game.setHideWhenGameStart(hideWhenGameStart); }

    public boolean isAutoStartGame() { return game.isAutoStartGame(); }
    public SimpleBooleanProperty autoStartGameProperty() { return game.autoStartGameProperty(); }
    public void setAutoStartGame(boolean autoStartGame) { game.setAutoStartGame(autoStartGame); }

    // ============ LauncherSetting delegate ============
    public boolean isUserAdvanceGameSettings() { return launcher.isUserAdvanceGameSettings(); }
    public SimpleBooleanProperty userAdvanceGameSettingsProperty() { return launcher.userAdvanceGameSettingsProperty(); }
    public void setUserAdvanceGameSettings(boolean userAdvanceGameSettings) { launcher.setUserAdvanceGameSettings(userAdvanceGameSettings); }

    public String getAppParams() { return launcher.getAppParams(); }
    public SimpleStringProperty appParamsProperty() { return launcher.appParamsProperty(); }
    public void setAppParams(String appParams) { launcher.setAppParams(appParams); }

    public ObservableList<String> getStartUpParams() { return startUpParams; }
    public void setStartUpParams(ObservableList<String> startUpParams) { this.startUpParams = startUpParams; }

    // ============ GachaSetting delegate ============
    public String getGachaCurrentPlayerId() { return gacha.getGachaCurrentPlayerId(); }
    public SimpleStringProperty gachaCurrentPlayerIdProperty() { return gacha.gachaCurrentPlayerIdProperty(); }
    public void setGachaCurrentPlayerId(String gachaCurrentPlayerId) { gacha.setGachaCurrentPlayerId(gachaCurrentPlayerId); }

    public boolean isGachaListModel() { return gacha.isGachaListModel(); }
    public SimpleBooleanProperty gachaListModelProperty() { return gacha.gachaListModelProperty(); }
    public void setGachaListModel(boolean gachaListModel) { gacha.setGachaListModel(gachaListModel); }

    // ============ SignSetting delegate ============
    public boolean isAutoKujieQuSign() { return sign.isAutoKujieQuSign(); }
    public SimpleBooleanProperty autoKujieQuSignProperty() { return sign.autoKujieQuSignProperty(); }
    public void setAutoKujieQuSign(boolean autoKujieQuSign) { sign.setAutoKujieQuSign(autoKujieQuSign); }

    public int getLastKujiequSignTime() { return sign.getLastKujiequSignTime(); }
    public SimpleIntegerProperty lastKujiequSignTimeProperty() { return sign.lastKujiequSignTimeProperty(); }
    public void setLastKujiequSignTime(int lastKujiequSignTime) { sign.setLastKujiequSignTime(lastKujiequSignTime); }

    // ============ ServerSetting delegate ============
    public String getServerUsername() { return server.getServerUsername(); }
    public SimpleStringProperty serverUsernameProperty() { return server.serverUsernameProperty(); }
    public void setServerUsername(String serverUsername) { server.setServerUsername(serverUsername); }

    public String getServerPassword() { return server.getServerPassword(); }
    public SimpleStringProperty serverPasswordProperty() { return server.serverPasswordProperty(); }
    public void setServerPassword(String serverPassword) { server.setServerPassword(serverPassword); }

    // ============ AppBehaviorSetting delegate ============
    public boolean isDevModel() { return behavior.isDevModel(); }
    public SimpleBooleanProperty devModelProperty() { return behavior.devModelProperty(); }
    public void setDevModel(boolean devModel) { behavior.setDevModel(devModel); }

    public boolean isSupport() { return behavior.isSupport(); }
    public SimpleBooleanProperty supportProperty() { return behavior.supportProperty(); }
    public void setSupport(boolean support) { behavior.setSupport(support); }

    public String getSkipVersion() { return behavior.getSkipVersion(); }
    public SimpleStringProperty skipVersionProperty() { return behavior.skipVersionProperty(); }
    public void setSkipVersion(String skipVersion) { behavior.setSkipVersion(skipVersion); }

    public boolean isCheckNewVersion() { return behavior.isCheckNewVersion(); }
    public SimpleBooleanProperty checkNewVersionProperty() { return behavior.checkNewVersionProperty(); }
    public void setCheckNewVersion(boolean checkNewVersion) { behavior.setCheckNewVersion(checkNewVersion); }

    public boolean isAutoStart() { return behavior.isAutoStart(); }
    public SimpleBooleanProperty autoStartProperty() { return behavior.autoStartProperty(); }
    public void setAutoStart(boolean autoStart) { behavior.setAutoStart(autoStart); }

    public boolean isSilentStart() { return behavior.isSilentStart(); }
    public SimpleBooleanProperty silentStartProperty() { return behavior.silentStartProperty(); }
    public void setSilentStart(boolean silentStart) { behavior.setSilentStart(silentStart); }

    public int getCloseEvent() { return behavior.getCloseEvent(); }
    public SimpleIntegerProperty closeEventProperty() { return behavior.closeEventProperty(); }
    public void setCloseEvent(int closeEvent) { behavior.setCloseEvent(closeEvent); }

    public int getResourceSource() { return behavior.getResourceSource(); }
    public SimpleIntegerProperty resourceSourceProperty() { return behavior.resourceSourceProperty(); }
    public void setResourceSource(int resourceSource) { behavior.setResourceSource(resourceSource); }

    public boolean isNoKuJieQu() { return behavior.isNoKuJieQu(); }
    public SimpleBooleanProperty noKuJieQuProperty() { return behavior.noKuJieQuProperty(); }
    public void setNoKuJieQu(boolean noKuJieQu) { behavior.setNoKuJieQu(noKuJieQu); }

    public boolean isUseLocalCacheUser() { return behavior.isUseLocalCacheUser(); }
    public SimpleBooleanProperty useLocalCacheUserProperty() { return behavior.useLocalCacheUserProperty(); }
    public void setUseLocalCacheUser(boolean useLocalCacheUser) { behavior.setUseLocalCacheUser(useLocalCacheUser); }

    public String getHomeViewIcon() { return behavior.getHomeViewIcon(); }
    public SimpleStringProperty homeViewIconProperty() { return behavior.homeViewIconProperty(); }
    public void setHomeViewIcon(String homeViewIcon) { behavior.setHomeViewIcon(homeViewIcon); }

    public String getHomeViewRole() { return behavior.getHomeViewRole(); }
    public SimpleStringProperty homeViewRoleProperty() { return behavior.homeViewRoleProperty(); }
    public void setHomeViewRole(String homeViewRole) { behavior.setHomeViewRole(homeViewRole); }

    public String getLogLevel() { return behavior.getLogLevel(); }
    public SimpleStringProperty logLevelProperty() { return behavior.logLevelProperty(); }
    public void setLogLevel(String logLevel) { behavior.setLogLevel(logLevel); }

    /** 将当前设置持久化到 settings.json */
    public void save() {
        ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("settings.json"), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
