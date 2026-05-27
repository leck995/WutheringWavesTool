package cn.tealc.wutheringwavestool.base;

import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class Config {
    private static volatile Setting setting;

    public static ResourceBundle language;
    public static String appTitle;

    static {
        language = ResourceBundle.getBundle("cn/tealc/wutheringwavestool/language/local", Locale.SIMPLIFIED_CHINESE);
        appTitle = LanguageManager.getString("app.title");
    }

    /** 获取 Guice 管理的 Setting 单例 */
    public static Setting setting() {
        if (setting == null) {
            synchronized (Config.class) {
                if (setting == null) {
                    setting = AppInjector.getInstance(Setting.class);
                }
            }
        }
        return setting;
    }

    public static void save() {
        File file = new File("settings.json");
        ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, setting());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
