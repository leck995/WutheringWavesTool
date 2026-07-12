package test;

import atlantafx.base.util.Animations;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-12-09 19:48
 */
public class SnapTest extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FlowPane pane = new FlowPane();
        for (int i = 0; i < 300; i++) {
            pane.getChildren().add(new Button("Hello World"));
        }
        ImageView iv = new ImageView(new Image(new File("C:\\Leck\\User\\Picture\\壁纸\\xlmk2l.png").toURI().toString()));
        pane.getChildren().add(iv);
        Scene scene = new Scene(pane, 1920, 1080);

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.K){
                System.out.println(System.currentTimeMillis());
                SnapshotParameters params = new SnapshotParameters();
                params.setFill(javafx.scene.paint.Color.TRANSPARENT);

                WritableImage image = pane.snapshot(params, null);
                ImageView imageView = new ImageView(image);
                System.out.println(System.currentTimeMillis());
                Node oldNode = pane.getChildren().get(0);
                pane.getChildren().add(imageView);
                Timeline my_timeline = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(pane.scaleXProperty(), 0.9, Animations.EASE),
                                new KeyValue(pane.scaleYProperty(), 0.9, Animations.EASE)));
                my_timeline.setOnFinished(event2 -> {
                    pane.setScaleX(1);
                    pane.setScaleY(1);
                    pane.getChildren().remove(imageView);
                });
                my_timeline.play();

            }
        });
        stage.setScene(scene);
        stage.show();
    }
}