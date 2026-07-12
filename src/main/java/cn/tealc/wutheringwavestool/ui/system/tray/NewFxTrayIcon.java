package cn.tealc.wutheringwavestool.ui.system.tray;

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
                        // 转换为 JavaFX 逻辑坐标
                        Screen primary = Screen.getPrimary();
                        double[] logical = convertToLogical(e.getX(), e.getY(), scale, primary);
                        double logicalX = logical[0];
                        double logicalY = logical[1];

                        stage.setX(logicalX - 5);
                        stage.setY(logicalY - stage.getHeight() - 5);
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
     * @description: 获取系统缩放比例（物理像素 / 逻辑像素）
     * 使用 GraphicsDevice.getDisplayMode() 获取物理分辨率，
     * Screen.getBounds() 获取逻辑分辨率，两者相除得到真实缩放比。
     * @date:   2025/2/21
     */
    private Point2D getScale() {
        try {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();
            DisplayMode dm = gd.getDisplayMode();
            Screen primary = Screen.getPrimary();
            double scaleX = (double) dm.getWidth() / primary.getBounds().getWidth();
            double scaleY = (double) dm.getHeight() / primary.getBounds().getHeight();
            return new Point2D(scaleX, scaleY);
        } catch (Exception e) {
            // Fallback
            double scaleX = Screen.getPrimary().getOutputScaleX();
            double scaleY = Screen.getPrimary().getOutputScaleY();
            return new Point2D(scaleX, scaleY);
        }
    }


    /**
     * @description: 将 AWT 屏幕坐标（可能是物理像素或逻辑像素）转换为 JavaFX 逻辑坐标
     * 通过判断坐标值是否超出逻辑屏幕范围来区分物理/逻辑像素
     */
    private double[] convertToLogical(double awtX, double awtY, Point2D scale, Screen screen) {
        double boundsWidth = screen.getBounds().getWidth();
        double boundsHeight = screen.getBounds().getHeight();

        // 如果坐标值超过了逻辑屏幕的宽高，说明 AWT 上报的是物理像素，需要除以缩放比
        boolean awtIsPhysical = (awtX > boundsWidth) || (awtY > boundsHeight);

        if (awtIsPhysical) {
            return new double[]{awtX / scale.getX(), awtY / scale.getY()};
        } else {
            return new double[]{awtX, awtY};
        }
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