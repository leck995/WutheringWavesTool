package cn.tealc.wutheringwavestool.util;

import cn.tealc.wutheringwavestool.base.Config;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-20 22:19
 */
public class LanguageManager {

    private static final String separate = "#";
    public static String getString(String key){
        return Config.language.getString(key);
    }

    public static String[] getStringArray(String key){
        String string = getString(key);
        return string.split(separate);
    }


}