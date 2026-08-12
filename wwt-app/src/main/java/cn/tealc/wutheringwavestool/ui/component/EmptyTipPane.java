package cn.tealc.wutheringwavestool.ui.component;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

/**
 * 当内容空白时，空白提示
 *
 */
public class EmptyTipPane extends VBox {
    public EmptyTipPane(@NonNull String title) {
        this(title, null, null);
    }

    public EmptyTipPane(@NonNull String title, @Nullable String message) {
        this(title, message, null);
    }

    public EmptyTipPane(@NonNull String title, @Nullable String message, @Nullable Ikon icon) {
        setSpacing(5.0);
        setAlignment(Pos.CENTER);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add(Styles.TITLE_3);
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add(Styles.TEXT_SUBTLE);
        FontIcon fontIcon = new FontIcon();
        fontIcon.setStyle("-fx-icon-size: 25px;");
        if (icon == null) {
            fontIcon.setIconCode(Material2MZ.WARNING);
        } else {
            fontIcon.setIconCode(icon);
        }
        getChildren().addAll(fontIcon, titleLabel, messageLabel);
    }
}
