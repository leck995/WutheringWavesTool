package cn.tealc.wutheringwavestool;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-09-28 21:48
 */
public class TrayIconWindow extends Popup {
    public TrayIconWindow() {
        Button show = new Button("显示");
        Button exit = new Button("退出");
        VBox vbox = new VBox(show, exit);
        getContent().add(vbox);
    }
}