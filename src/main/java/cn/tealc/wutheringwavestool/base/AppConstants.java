package cn.tealc.wutheringwavestool.base;

public final class AppConstants {
    private AppConstants() {}

    public static final String VERSION = "1.3.4";
    public static final String APP_AUTHOR = "Leck";

    public static final String API_DECRYPT_KEY = "XSNLFgNCth8j8oJI3cNIdw==";

    public static final String URL_SUPPORT_LIST = "https://www.yuque.com/chashuisuipian/sm05lg/ag7ct2or8ecz98cp";
    public static final String URL_PHANTOM_GUIDE = "https://wave.999758.xyz/pages/advance/phantom.html";
    public static final String URL_APP_UPDATE = "https://wwt.999758.xyz/release.json";
    public static final String URL_APP_UPDATE_DEV = "https://wwt.999758.xyz/release-dev.json";
    //public static final String URL_HOST_SERVER = "http://gamemaid.999758.xyz";
    public static final String URL_HOST_SERVER = "http://127.0.0.1:8080";
    public static final String URL_REDEMPTION_CODES = URL_HOST_SERVER + "/api/redemption-codes/mc";
    public static final String URL_ANNOUNCEMENTS = URL_HOST_SERVER +  "/api/announcements/game";
    public static final String URL_AUTH_LOGIN = URL_HOST_SERVER +  "/api/auth/login";
    public static final String URL_AUTH_VERIFY = URL_HOST_SERVER +  "/api/auth/verify";
    public static final String URL_FILE_UPLOAD = URL_HOST_SERVER +  "/api/mc/gacha/uploadByUser";
    public static final String URL_GACHA_FILE_LIST = URL_HOST_SERVER + "/api/mc/gacha/listByUser";
    public static final String URL_GACHA_FILE_DELETE = URL_HOST_SERVER + "/api/mc/gacha/deleteByUser/";
}
