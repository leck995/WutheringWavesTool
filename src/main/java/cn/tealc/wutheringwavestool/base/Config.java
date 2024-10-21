package cn.tealc.wutheringwavestool.base;

import cn.tealc.wutheringwavestool.model.Setting;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 00:37
 */
public class Config {
    public static final String version="1.1.9";
    public static final String appTitle="鸣潮助手";
    public static final String appAuthor="Leck";
    public static final String apiDecryptKey = "XSNLFgNCth8j8oJI3cNIdw==";
    public static final String URL_SUPPORT_LIST="https://www.yuque.com/chashuisuipian/sm05lg/ag7ct2or8ecz98cp";

    public static ResourceBundle language;
    public static Setting setting;
    public static String currentRoleId;

    static {
        ObjectMapper mapper=new ObjectMapper();
        File settingFile = new File("settings.json");
        if (settingFile.exists()){
            try {
                setting=mapper.readValue(settingFile, Setting.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (setting == null){
            setting=new Setting();
        }
    }


    public static void save() {
        File file = new File("settings.json");
        ObjectMapper mapper=new ObjectMapper();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file,setting);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}