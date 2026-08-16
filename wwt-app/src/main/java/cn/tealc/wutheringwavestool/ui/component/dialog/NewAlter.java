package cn.tealc.wutheringwavestool.ui.component.dialog;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedAL;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;

import java.util.Objects;

/** Alert with the application's shared HeaderBar and shadcn-style skin. */
public class NewAlter extends Alert {
    public enum AlertStyle {
        INFO("shadcn-alert-info", Material2OutlinedAL.INFO, AlertType.INFORMATION),
        WARNING("shadcn-alert-warning", Material2OutlinedMZ.WARNING, AlertType.WARNING),
        DANGER("shadcn-alert-danger", Material2OutlinedAL.ERROR_OUTLINE, AlertType.ERROR),
        SUCCESS("shadcn-alert-success", Material2OutlinedAL.CHECK_CIRCLE, AlertType.INFORMATION),
        DEFAULT("shadcn-alert-default", Material2OutlinedAL.HELP_OUTLINE, AlertType.NONE);

        private final String styleClass;
        private final Ikon icon;
        private final AlertType alertType;

        AlertStyle(String styleClass, Ikon icon, AlertType alertType) {
            this.styleClass = styleClass;
            this.icon = icon;
            this.alertType = alertType;
        }

        String styleClass() {
            return styleClass;
        }

        Ikon icon() {
            return icon;
        }

        AlertType alertType() {
            return alertType;
        }

        static AlertStyle from(AlertType alertType) {
            return switch (alertType) {
                case INFORMATION -> INFO;
                case WARNING -> WARNING;
                case ERROR -> DANGER;
                case CONFIRMATION, NONE -> DEFAULT;
            };
        }
    }

    private AlertStyle alertStyle;
    private final HBox alertContent = new HBox();
    private final Label contentLabel = new Label();
    private FontIcon alertIcon;

    public NewAlter(AlertType alertType) {
        super(alertType);
        DialogStyler.apply(this);
        installAlertContent();
        setAlertStyle(AlertStyle.from(alertType));
    }

    public NewAlter(AlertType alertType, String contentText, ButtonType... buttons) {
        super(alertType, contentText, buttons);
        DialogStyler.apply(this);
        installAlertContent();
        setAlertStyle(AlertStyle.from(alertType));
    }

    public NewAlter(AlertStyle style) {
        super(Objects.requireNonNull(style, "style").alertType());
        DialogStyler.apply(this);
        installAlertContent();
        setAlertStyle(style);
    }

    public NewAlter(AlertStyle style, String contentText, ButtonType... buttons) {
        super(Objects.requireNonNull(style, "style").alertType(), contentText, buttons);
        DialogStyler.apply(this);
        installAlertContent();
        setAlertStyle(style);
    }

    public static NewAlter info(String contentText) {
        return new NewAlter(AlertStyle.INFO, contentText, ButtonType.OK);
    }

    public static NewAlter warning(String contentText) {
        return new NewAlter(AlertStyle.WARNING, contentText, ButtonType.OK);
    }

    public static NewAlter success(String contentText) {
        return new NewAlter(AlertStyle.SUCCESS, contentText, ButtonType.OK);
    }

    public static NewAlter danger(String contentText) {
        return new NewAlter(AlertStyle.DANGER, contentText, ButtonType.OK);
    }

    public static NewAlter defaultAlert(String contentText) {
        return new NewAlter(AlertStyle.DEFAULT, contentText, ButtonType.OK);
    }

    public AlertStyle getAlertStyle() {
        return alertStyle;
    }

    public final void setAlertStyle(AlertStyle style) {
        this.alertStyle = Objects.requireNonNull(style, "style");

        if (!(getDialogPane() instanceof NewDialogPane pane)) {
            return;
        }

        pane.getStyleClass().removeIf(cssClass ->
                cssClass.startsWith("shadcn-alert-") && !cssClass.equals("shadcn-alert-pane"));
        pane.getStyleClass().add(style.styleClass());
        pane.setGraphic(null);
        pane.setHeader(null);
        pane.setHeaderText(null);
        pane.setHeaderRightGraphic(null);

        if (alertIcon != null) {
            alertContent.getChildren().remove(alertIcon);
        }
        alertIcon = new FontIcon(style.icon());
        alertIcon.getStyleClass().add("shadcn-alert-icon");
        alertContent.getChildren().addFirst(alertIcon);
    }

    private void installAlertContent() {
        if (!(getDialogPane() instanceof NewDialogPane pane)) {
            return;
        }

        pane.getStyleClass().add("shadcn-alert-pane");
        pane.setHeader(null);
        pane.setHeaderText(null);
        pane.setGraphic(null);

        contentLabel.getStyleClass().add("shadcn-alert-content-label");
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(contentLabel, Priority.ALWAYS);

        alertContent.getStyleClass().add("shadcn-alert-content");
        alertContent.setMaxWidth(Double.MAX_VALUE);
        alertContent.setAlignment(Pos.TOP_LEFT);
        alertContent.getChildren().add(contentLabel);

        pane.contentTextProperty().addListener((observable, oldValue, newValue) ->
                contentLabel.setText(newValue == null ? "" : newValue));
        contentLabel.setText(pane.getContentText() == null ? "" : pane.getContentText());
        pane.setContent(alertContent);
    }
}
