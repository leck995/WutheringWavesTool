package cn.tealc.wutheringwavestool.ui.tray;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * 自定义系统托盘
 * 注意传递的menu必须设置preSize，不然无法设置托盘窗口大小
 */
public class NewFxTrayIcon extends TrayIcon {
    private final Stage stage = new Stage();
    private final StackPane pane = new StackPane();

    public NewFxTrayIcon(Image image, String tooltip,Region menu) {
        super(image, tooltip);
        //设置系统托盘图标为自适应
        this.setImageAutoSize(true);

        initStage();
        //添加组件到面板中
        pane.getChildren().add(menu);
        //设置面板的宽高
        stage.setWidth(menu.getPrefWidth());
        stage.setHeight(menu.getPrefHeight());
        //添加鼠标事件
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                //getButton() 1左键 2中键 3右键
                if (e.getButton() == 3) {
                    Platform.runLater(() -> {
                        stage.setX(e.getX());
                        stage.setY(e.getY() - stage.getHeight());
                        if (!stage.isShowing()) {
                            stage.show();
                        } else {
                            stage.hide();
                        }
                    });
                }
            }
        });
    }


    private void initStage(){
        Stage parent = new Stage();
        parent.setWidth(1.0);
        parent.setHeight(1.0);
        parent.initStyle(StageStyle.UTILITY);
        parent.setOpacity(0.0);
        parent.show();
        Scene scene = new Scene(pane);
        pane.setStyle("-fx-background-color: transparent;");
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initOwner(parent);
        stage.setAlwaysOnTop(true); //设置为顶层，否则在windows系统中会被底部任务栏遮挡
        stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                stage.hide();
            }
        });
    }
}