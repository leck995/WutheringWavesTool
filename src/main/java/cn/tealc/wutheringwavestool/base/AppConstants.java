package cn.tealc.wutheringwavestool.base;

public final class AppConstants {
    private AppConstants() {}

    public static final String VERSION = "1.5.1";
    public static final String APP_AUTHOR = "Leck";

    public static final String API_DECRYPT_KEY = "XSNLFgNCth8j8oJI3cNIdw==";

    public static final String URL_SUPPORT_LIST = "https://www.yuque.com/chashuisuipian/sm05lg/ag7ct2or8ecz98cp";
    public static final String URL_GITHUB = "https://github.com/leck995/WutheringWavesTool";
    public static final String URL_GITHUB_ISSUES = URL_GITHUB + "/issues";
    public static final String URL_WIKI = "https://wwt-doc.999758.xyz";
    public static final String URL_PHANTOM_GUIDE = URL_WIKI + "/pages/advance/phantom.html";
    public static final String URL_TOKEN_GUIDE = URL_WIKI + "/pages/advance/token.html";
    public static final String URL_APP_UPDATE = "https://release.999758.xyz/wwt/release.json";
    public static final String URL_APP_UPDATE_DEV = "https://release.999758.xyz/wwt/release-dev.json";
    public static final String URL_APP_UPDATE_2 = "https://cdn.999758.xyz/wwt/release.json";
    public static final String URL_APP_UPDATE_DEV_2 = "https://cdn.999758.xyz/wwt/release-dev.json";
    public static final String URL_HOST_SERVER = "https://api.999758.xyz:20141";
    //public static final String URL_HOST_SERVER = "http://127.0.0.1:8080";
    public static final String URL_REDEMPTION_CODES = URL_HOST_SERVER + "/api/redemption-codes/mc";
    public static final String URL_ANNOUNCEMENTS = URL_HOST_SERVER +  "/api/announcements/game";
    public static final String URL_AUTH_LOGIN = URL_HOST_SERVER +  "/api/auth/login";
    public static final String URL_AUTH_VERIFY = URL_HOST_SERVER +  "/api/auth/verify";
    public static final String URL_FILE_UPLOAD = URL_HOST_SERVER +  "/api/mc/gacha/uploadByUser";
    public static final String URL_GACHA_FILE_LIST = URL_HOST_SERVER + "/api/mc/gacha/listByUser";
    public static final String URL_GACHA_FILE_DELETE = URL_HOST_SERVER + "/api/mc/gacha/deleteByUser/";
}
