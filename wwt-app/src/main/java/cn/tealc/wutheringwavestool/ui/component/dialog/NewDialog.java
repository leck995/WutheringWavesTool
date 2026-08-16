package cn.tealc.wutheringwavestool.ui.component.dialog;

import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

/** A generic JavaFX Dialog configured with the application's shadcn-style pane. */
public class NewDialog<R> extends Dialog<R> {
    public NewDialog() {
        DialogStyler.apply(this);
    }

    public NewDialog(String title, Node content, ButtonType... buttonTypes) {
        this();
        setTitle(title);
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().setAll(buttonTypes);
    }
}
