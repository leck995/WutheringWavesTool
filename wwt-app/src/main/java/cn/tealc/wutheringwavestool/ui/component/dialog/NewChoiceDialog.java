package cn.tealc.wutheringwavestool.ui.component.dialog;

import javafx.scene.control.ChoiceDialog;

import java.util.Collection;

/** ChoiceDialog with the application's shared HeaderBar and shadcn-style skin. */
public class NewChoiceDialog<T> extends ChoiceDialog<T> {
    public NewChoiceDialog() {
        super();
        DialogStyler.apply(this);
    }

    @SafeVarargs
    public NewChoiceDialog(T defaultChoice, T... choices) {
        super(defaultChoice, choices);
        DialogStyler.apply(this);
    }

    public NewChoiceDialog(T defaultChoice, Collection<T> choices) {
        super(defaultChoice, choices);
        DialogStyler.apply(this);
    }
}
