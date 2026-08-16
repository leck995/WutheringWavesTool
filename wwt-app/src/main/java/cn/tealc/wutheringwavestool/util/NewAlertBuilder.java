package cn.tealc.wutheringwavestool.util;

import cn.tealc.wutheringwavestool.ui.component.dialog.NewAlter;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fluent builder for the application's styled alert.
 *
 * <p>The builder only creates the alert. The caller decides whether to use
 * {@link NewAlter#show()} or {@link NewAlter#showAndWait()} so that both
 * asynchronous and result-based flows remain available.</p>
 */
public final class NewAlertBuilder {
    private NewAlter.AlertStyle style = NewAlter.AlertStyle.DEFAULT;
    private String title;
    private String contentText;
    private Window owner;
    private final List<ButtonType> buttonTypes = new ArrayList<>();

    private NewAlertBuilder() {
    }

    public static NewAlertBuilder create() {
        return new NewAlertBuilder();
    }

    public NewAlertBuilder style(NewAlter.AlertStyle style) {
        this.style = style;
        return this;
    }

    public NewAlertBuilder title(String title) {
        this.title = title;
        return this;
    }

    public NewAlertBuilder content(String contentText) {
        this.contentText = contentText;
        return this;
    }

    public NewAlertBuilder owner(Window owner) {
        this.owner = owner;
        return this;
    }

    public NewAlertBuilder button(ButtonType buttonType) {
        buttonTypes.add(buttonType);
        return this;
    }

    public NewAlertBuilder buttons(ButtonType... buttonTypes) {
        this.buttonTypes.addAll(Arrays.asList(buttonTypes));
        return this;
    }

    public NewAlertBuilder button(String text, ButtonBar.ButtonData buttonData) {
        return button(new ButtonType(text, buttonData));
    }

    public NewAlertBuilder replaceButtons(ButtonType... buttonTypes) {
        this.buttonTypes.clear();
        return buttons(buttonTypes);
    }

    public NewAlter build() {
        ButtonType[] buttons = buttonTypes.isEmpty()
                ? new ButtonType[]{ButtonType.OK}
                : buttonTypes.toArray(ButtonType[]::new);
        NewAlter alert = new NewAlter(style, contentText, buttons);
        if (title != null) {
            alert.setTitle(title);
        }
        if (owner != null) {
            alert.initOwner(owner);
        }
        return alert;
    }
}
