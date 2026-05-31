package cn.tealc.wutheringwavestool.ui.tray;

import cn.tealc.wutheringwavestool.FXResourcesLoader;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
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
    private Stage parent;

    public NewFxTrayIcon(Image image, String tooltip, Region menu) {
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
                        Point2D scale = getScale();
                        stage.setX(e.getX() / scale.getY() - 5);
                        stage.setY((e.getY() - stage.getHeight()) / scale.getY() - 5);
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


    /**
     * @description: 获取系统缩放比例
     * @param:
     * @return  javafx.geometry.Point2D
     * @date:   2025/2/21
     */
    private Point2D getScale() {
        double scaleX = Screen.getPrimary().getOutputScaleX();
        double scaleY = Screen.getPrimary().getOutputScaleY();
        return new Point2D(scaleX, scaleY);
    }

    private void initStage() {
        parent = new Stage();
        parent.setTitle("鸣潮助手 Tray Parent");
        stage.getIcons().add(new javafx.scene.image.Image(FXResourcesLoader.load("image/icon.png"), 45, 45, true, true));
        parent.setWidth(1.0);
        parent.setHeight(1.0);
        parent.initStyle(StageStyle.UTILITY);
        parent.setOpacity(0.0);
        parent.show();
        Scene scene = new Scene(pane);
        pane.setStyle("-fx-background-color: transparent;");
        scene.setFill(Color.TRANSPARENT);
        stage.setTitle("鸣潮助手 Tray");

        stage.setScene(scene);
        stage.getIcons().add(new javafx.scene.image.Image(FXResourcesLoader.load("image/icon.png"), 45, 45, true, true));
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