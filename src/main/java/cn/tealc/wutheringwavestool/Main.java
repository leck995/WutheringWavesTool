package cn.tealc.wutheringwavestool;

import cn.tealc.wutheringwavestool.base.Config;
import javafx.application.Application;

import java.io.File;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-04 00:32
 */
public class Main {
    public static void main(String[] args) {
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("LcdFontSmoothing", "true");
        System.setProperty("prism.text", "t2k");

        if (Config.setting.getUiScale() != 100){
            System.setProperty("glass.win.uiScale", Config.setting.getUiScale() + "%");
        }

        Application.launch(MainApplication.class,args);
    }

    private static void checkUpdate(){
        File file = new File(".");
        assert file.exists();
        if (file.getName().equals("update")){
            File[] files = file.getParentFile().listFiles((dir, name) -> name.equals("WutheringWavesTool.exe"));
            if (files != null && files.length > 0){

            }
        }

    }

    private static void move(){

    }
}