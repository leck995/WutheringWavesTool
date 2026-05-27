package cn.tealc.wutheringwavestool.ui.item;

import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import com.jfoenixN.controls.JFXDialogLayout;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;

import java.io.File;

/**
 * @description:
 * @author: Leck
 * @create: 2025-02-18 00:26
 */
public class HeaderImageSelectView extends JFXDialogLayout {
    private final Button cancelBtn;

    public HeaderImageSelectView() {
        Label title = new Label("设置头像");
        title.getStyleClass().add(Styles.TITLE_2);

        cancelBtn = new Button("取消");
        cancelBtn.setCancelButton(true);


        FlowPane flowPane = new FlowPane();
        flowPane.setVgap(5);
        flowPane.setHgap(5);
        ScrollPane scrollPane = new ScrollPane(flowPane);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        File dir = new File("assets/header");
        File[] headers = dir.listFiles(header -> {
            if (header.getName().endsWith(".jpg") || header.getName().endsWith(".png")) {
                return header.getName().length() < 10;
            }
            return false;
        });

        if (headers != null) {
            for (File header : headers) {
                ImageView imageView = new ImageView(new Image(header.toURI().toString(), 60, 60, true, true, true));
                imageView.setOnMouseClicked(event -> setImage(header));
                flowPane.getChildren().add(imageView);
            }
        } else {
            Label label = new Label("未找到其他头像，请更新助手");
            flowPane.getChildren().add(label);
        }
        setHeading(title);
        setBody(scrollPane);
        setActions(cancelBtn);
        setPrefSize(600, 420);
    }

    private void setImage(File file) {
        Config.setting().setHomeViewIcon(file.getName());
        NotificationManager.publish(NotificationKey.CHANGE_HEADER);
        cancelBtn.fireEvent(new ActionEvent());
    }
}