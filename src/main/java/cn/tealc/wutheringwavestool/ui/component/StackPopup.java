package cn.tealc.wutheringwavestool.ui.component;

import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.PopupControl;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * @description:
 * @author: Leck
 * @create: 2025-02-17 19:33
 */
public class StackPopup extends PopupControl {
    private StackPane root;
    public StackPopup() {
        root = new StackPane();
        getScene().setRoot(root);
        root.getStyleClass().add("stack-popup");
    }


    public void addChild(Node content) {
        root.getChildren().add(content);
    }

    public void show(Node source){
        Point2D local = source.localToScreen(0, 0);
        show(source.getScene().getWindow(), local.getX(), local.getY());
    }
    public void show(Node source,Side side){
        Point2D local = source.localToScreen(0, 0);

        show(source.getScene().getWindow(), local.getX(), local.getY());
    }




}