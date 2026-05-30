package cn.tealc.wutheringwavestool;

import cn.tealc.wutheringwavestool.base.Config;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-04 00:32
 */
public class Launcher {
    public static void main(String[] args) {
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("LcdFontSmoothing", "true");
        System.setProperty("prism.text", "t2k");

        if (Config.setting().getUiScale() != 100){
            System.setProperty("glass.win.uiScale", Config.setting().getUiScale() + "%");
        }

        javafx.application.Application.launch(WwtApp.class,args);
    }

}