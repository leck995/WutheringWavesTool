package cn.tealc.wutheringwavestool;

import cn.tealc.wutheringwavestool.model.Setting;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.SimpleStringProperty;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 00:37
 */
public class Config {
    public static final String version="1.1.2";
    public static final String appTitle="鸣潮助手";
    public static Setting setting;
    public static Map<String, String> headers = new HashMap<>();
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

        File headerFile = new File("assets/data/header.json");
        if (headerFile.exists()){
            try {
                headers=mapper.readValue(headerFile, new TypeReference<HashMap<String, String>>() {});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (headers == null){
            headers=new HashMap<>();
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