package cn.tealc.wutheringwavestool.ui.component;

import atlantafx.base.controls.Message;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import com.jfoenixN.controls.JFXDialog;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

/**
 * 应用级覆盖层：统一承载 dialog / message / alert 三类全局展示。
 * 作为 {@code root} StackPane 的最顶层子节点；JFXDialog 仍挂载在 root 上以保持原有遮罩/居中行为。
 * 透明区域通过 {@code pickOnBounds=false} 透传点击，仅 message 实际占用区域拾取事件。
 *
 * @author Leck
 */
public class OverlayLayer extends AnchorPane {

    private final StackPane root;
    private final VBox messagePane = new VBox(5.0);

    public OverlayLayer(StackPane root) {
        this.root = root;
        setPickOnBounds(false);

        AnchorPane.setTopAnchor(messagePane, 60.0);
        AnchorPane.setRightAnchor(messagePane, 10.0);
        messagePane.setPickOnBounds(false);
        getChildren().add(messagePane);

        initGlobalEvents();
    }

    private void initGlobalEvents() {
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.MESSAGE, ((s, objects) -> {
            showMessage((MessageInfo) objects[0]);
        }));
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.DIALOG, ((s, objects) -> {
            if (objects[0] instanceof JFXDialogLayout node) {
                showDialog(node);
            } else {
                Pane panes = (Pane) objects[0];
                if (objects[1] != null && objects[1] instanceof BaseDialog dialog) {
                    showDialog(panes, dialog);
                } else {
                    showDialog(panes);
                }
            }
        }));
        // ALERT
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.ALERT, ((s, objects) -> {
            if (objects[0] instanceof JFXDialogLayout node) {
                showDialog(node);
            }
        }));
    }


    private void showDialog(JFXDialogLayout container) {
        JFXDialog dialog = new JFXDialog(root, container, JFXDialog.DialogTransition.CENTER);
        for (Node action : container.getActions()) {
            if (action instanceof Button button) {
                if (button.isCancelButton()) {
                    EventHandler<ActionEvent> onAction = button.getOnAction();

                    button.setOnAction(event -> {
                        dialog.close();
                        if (onAction != null) {
                            onAction.handle(event);
                        }
                    });
                }
            }
        }
        dialog.show();
    }

    private void showDialog(Pane pane) {
        JFXDialog dialog = new JFXDialog(root, pane, JFXDialog.DialogTransition.CENTER);
        dialog.show();
    }

    private void showDialog(Pane pane, BaseDialog baseDialog) {
        JFXDialog dialog = new JFXDialog(root, pane, JFXDialog.DialogTransition.CENTER);
        baseDialog.setDialog(dialog);
        dialog.show();
    }


    private void showMessage(MessageInfo info) {
        if (messagePane.getChildren().size() > 7) {
            messagePane.getChildren().removeFirst();
        }
        Message message = createMessage(info);
        message.setOnClose(e -> {
            var out = Animations.slideOutRight(message, Duration.millis(250));
            out.setOnFinished(f -> messagePane.getChildren().remove(message));
            out.playFromStart();
        });
        Platform.runLater(() -> {
            messagePane.getChildren().add(message);
            message.setTranslateX(300);
            Timeline timeline = new Timeline(new KeyFrame(Duration.millis(250), new KeyValue(message.translateXProperty(), 0)));
            timeline.play();
        });
        if (info.getAutoClose()) {
            Timeline fiveSecondsWonder = new Timeline(new KeyFrame(info.getShowTime(), new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    var out = Animations.slideOutRight(message, Duration.millis(250));
                    out.setOnFinished(f -> messagePane.getChildren().remove(message));
                    out.playFromStart();
                }
            }));
            fiveSecondsWonder.play();
        }
    }


    private Message createMessage(MessageInfo messageInfo) {
        Message message = null;
        switch (messageInfo.getType()) {
            case SUCCESS -> {
                message = new Message(
                        messageInfo.getTitle().equals(MessageInfo.SUCCESS) ? null : messageInfo.getTitle(),
                        messageInfo.getMessage(),
                        new FontIcon(Material2AL.CHECK_CIRCLE)
                );
                message.getStyleClass().addAll(Styles.SUCCESS, "glass-message");
            }
            case WARNING -> {
                message = new Message(
                        messageInfo.getTitle().equals(MessageInfo.WARNING) ? null : messageInfo.getTitle(),
                        messageInfo.getMessage(),
                        new FontIcon(Material2MZ.WARNING)
                );
                message.getStyleClass().addAll(Styles.WARNING, "glass-message");
            }
            case INFO -> {
                message = new Message(
                        messageInfo.getTitle().equals(MessageInfo.INFO) ? null : messageInfo.getTitle(),
                        messageInfo.getMessage(),

                        new FontIcon(Material2AL.INFO)
                );
                message.getStyleClass().addAll(Styles.ACCENT, "glass-message");
            }
            case ERROR -> {
                message = new Message(
                        messageInfo.getTitle().equals(MessageInfo.ERROR) ? null : messageInfo.getTitle(),
                        messageInfo.getMessage(),
                        new FontIcon(Material2AL.HIGHLIGHT_OFF)
                );
                message.getStyleClass().addAll(Styles.DANGER, "glass-message");
            }
        }

        message.setPrefSize(300.0, 60.0);
        message.setMaxSize(300.0, 80.0);
        return message;
    }





}
