package cn.tealc.wutheringwavestool;

import cn.tealc.wutheringwavestool.base.Config;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;

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

        int uiScale = Config.setting().getUiScale();
        double systemScale = getSystemScale();
        double finalScale = systemScale * (uiScale / 100.0);
        System.setProperty("glass.win.uiScale", (int) Math.round(finalScale * 100) + "%");

        javafx.application.Application.launch(WwtApp.class, args);
    }

    /**
     * 通过 AWT 读取系统缩放比例（Windows DPI 缩放）。
     * 必须用 AWT 而非 JavaFX Screen，因为此方法在 JavaFX 启动前调用。
     *
     * @return 系统缩放比例（1.0=100%, 2.0=200%），读取失败时返回 1.0
     */
    private static double getSystemScale() {
        try {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            return gc.getDefaultTransform().getScaleX();
        } catch (Exception e) {
            return 1.0;
        }
    }
}