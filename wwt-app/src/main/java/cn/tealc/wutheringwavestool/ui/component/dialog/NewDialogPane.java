package cn.tealc.wutheringwavestool.ui.component.dialog;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderButtonType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedAL;

/**
 * DialogPane with a client-area header bar and a compact shadcn-like layout.
 */
public final class NewDialogPane extends javafx.scene.control.DialogPane {
    private final HeaderBar headerBar = new HeaderBar();
    private final Label titleLabel = new Label();
    private final Button closeButton = new Button(null, new FontIcon(Material2OutlinedAL.CLOSE));
    private final HBox headerRight = new HBox();

    public NewDialogPane() {
        getStyleClass().add("shadcn-dialog-pane");

        titleLabel.getStyleClass().add("shadcn-dialog-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        HBox left = new HBox(titleLabel);
        left.getStyleClass().add("shadcn-dialog-header-left");

        closeButton.getStyleClass().addAll("shadcn-dialog-close", "button-icon");
        closeButton.setFocusTraversable(false);
        closeButton.setTooltip(new javafx.scene.control.Tooltip("关闭"));
        HeaderBar.setButtonType(closeButton, HeaderButtonType.CLOSE);

        headerRight.getChildren().add(closeButton);
        headerRight.getStyleClass().add("shadcn-dialog-header-right");

        headerBar.getStyleClass().add("shadcn-dialog-headerbar");
        headerBar.setLeft(left);
        headerBar.setRight(headerRight);
        headerBar.setLeftSystemPadding(false);
        headerBar.setRightSystemPadding(false);
        setHeaderBar(headerBar);
    }

    /** Sets an optional node before the close button in the HeaderBar's right area. */
    public void setHeaderRightGraphic(Node graphic) {
        headerRight.getChildren().removeIf(node -> node != closeButton);
        if (graphic != null) {
            headerRight.getChildren().add(0, graphic);
        }
    }

    void attach(Dialog<?> dialog) {
        titleLabel.textProperty().unbind();
        titleLabel.textProperty().bind(dialog.titleProperty());
        closeButton.setOnAction(event -> dialog.close());
    }
}
