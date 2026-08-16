package cn.tealc.wutheringwavestool.ui.component.dialog;

import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.HeaderBar;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.Objects;

/** Installs the shared HeaderBar and shadcn-inspired styling on JavaFX dialogs. */
public final class DialogStyler {
    private static final String DIALOG_STYLESHEET =
            "/cn/tealc/wutheringwavestool/css/Dialog.css";

    private DialogStyler() {
    }

    public static <R> Dialog<R> apply(Dialog<R> dialog) {
        Objects.requireNonNull(dialog, "dialog");

        DialogPane original = dialog.getDialogPane();
        NewDialogPane styledPane;
        if (original instanceof NewDialogPane existing) {
            styledPane = existing;
        } else {
            styledPane = new NewDialogPane();
            copyPaneState(original, styledPane);
            dialog.setDialogPane(styledPane);
        }

        URL stylesheet = DialogStyler.class.getResource(DIALOG_STYLESHEET);
        if (stylesheet != null && !styledPane.getStylesheets().contains(stylesheet.toExternalForm())) {
            styledPane.getStylesheets().add(stylesheet.toExternalForm());
        }

        styledPane.attach(dialog);
        dialog.initStyle(StageStyle.EXTENDED);
        dialog.addEventHandler(DialogEvent.DIALOG_SHOWN, event -> hideSystemButtons(dialog));
        return dialog;
    }

    private static void copyPaneState(DialogPane source, NewDialogPane target) {
        for (String styleClass : source.getStyleClass()) {
            if (!target.getStyleClass().contains(styleClass)) {
                target.getStyleClass().add(styleClass);
            }
        }

        target.getButtonTypes().setAll(source.getButtonTypes());
        target.setGraphic(source.getGraphic());
        target.setHeaderText(source.getHeaderText());
        target.setContentText(source.getContentText());
        if (source.getHeaderText() == null && source.getHeader() != null) {
            target.setHeader(source.getHeader());
        }
        if (source.getContentText() == null && source.getContent() != null) {
            target.setContent(source.getContent());
        }
        target.setExpandableContent(source.getExpandableContent());
        target.setExpanded(source.isExpanded());
    }

    private static void hideSystemButtons(Dialog<?> dialog) {
        Scene scene = dialog.getDialogPane().getScene();
        if (scene != null && scene.getWindow() instanceof Stage stage) {
            HeaderBar.setPrefButtonHeight(stage, 0);
        }
    }
}
