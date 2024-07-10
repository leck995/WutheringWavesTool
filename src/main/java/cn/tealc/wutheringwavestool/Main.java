package cn.tealc.wutheringwavestool;

import cn.tealc.wutheringwavestool.jna.GameAppListener;
import com.sun.jna.platform.win32.User32;
import javafx.application.Application;

import java.time.LocalDate;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-04 00:32
 */
public class Main {
    public static void main(String[] args) {
        Application.launch(MainApplication.class,args);
    }
}