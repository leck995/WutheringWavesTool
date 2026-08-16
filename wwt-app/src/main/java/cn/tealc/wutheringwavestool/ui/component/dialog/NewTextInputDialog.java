package cn.tealc.wutheringwavestool.ui.component.dialog;

import javafx.scene.control.TextInputDialog;

/** TextInputDialog with the application's shared HeaderBar and shadcn-style skin. */
public class NewTextInputDialog extends TextInputDialog {
    public NewTextInputDialog() {
        super();
        DialogStyler.apply(this);
    }

    public NewTextInputDialog(String defaultValue) {
        super(defaultValue);
        DialogStyler.apply(this);
    }
}
