package cn.tealc.wutheringwavestool.base;

/**
 * @program: AsmrPlayer-web
 * @description:
 * @author: Leck
 * @create: 2024-01-18 16:34
 */
public class NotificationKey {
    public static final String MESSAGE="MESSAGE";
    public static final String DIALOG="DIALOG";
    public static final String CHANGE_BG="CHANGE_BG";
    public static final String CHANGE_HEADER="CHANGE_HEADER";
    public static final String CHANGE_NAV="CHANGE_NAV";
    public static final String SIGN_USER_DELETE="SIGN_USER_DELETE";
    public static final String SIGN_USER_UPDATE="SIGN_USER_UPDATE";

    public static final String HOME_GAME_TIME_UPDATE="HOME_GAME_TIME_UPDATE";
    public static final String HOME_ROLE_DATA_REFRESH="HOME_ROLE_DATA_REFRESH";
    public static final String HOME_ROLE_LOCAL_CHANGE="HOME_ROLE_LOCAL_CHANGE";

    public static final String NOTIFICATION_SHOW_UPDATE= "SHOW_UPDATE"; //显示升级界面

    public static final String CARD_POOL_USER_UPDATE="CARD_POOL_USER_UPDATE"; //当抽卡页面选中用户切换时使用
    public static final String CARD_POOL_USER_EMPTY="CARD_POOL_USER_EMPTY"; //当抽卡页面选中无用户时使用


    public static final String GAME_MANAGER_TO_BASE="GAME_MANAGE_TO_BASE"; // 通知GameManagerView显示GameBaseSettingView
    public static final String GAME_MANAGE_TO_CHOOSE="GAME_MANAGE_TO_CHOOSE"; // 通知GameManagerView显示GameDirChooseView



    public static final String ACCOUNT_UPDATE="ACCOUNT_UPDATE"; // 更新库街区账号



}