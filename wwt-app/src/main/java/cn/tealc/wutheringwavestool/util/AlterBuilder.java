package cn.tealc.wutheringwavestool.util;

import atlantafx.base.theme.Styles;
import com.jfoenixN.controls.JFXDialogLayout;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.util.ArrayList;
import java.util.List;


public class AlterBuilder {

    /** AccessibleText 哨兵：标记按钮未显式指定样式，需在 build() 时按 AlertType 兜底着色 */
    private static final String STYLE_PENDING = "alter-style-pending";

    /** 弹窗类型：决定 body 图标、图标着色与哨兵按钮兜底着色 */
    public enum AlertType {
        BASIC(Material2AL.HELP_OUTLINE, null, Styles.ACCENT),
        INFO(Material2AL.INFO, Styles.ACCENT, Styles.ACCENT),
        WARNING(Material2MZ.WARNING, Styles.WARNING, Styles.WARNING),
        DANGER(Material2AL.HIGHLIGHT_OFF, Styles.DANGER, Styles.DANGER),
        SUCCESS(Material2AL.CHECK_CIRCLE, Styles.SUCCESS, Styles.SUCCESS);

        private final Ikon icon;
        private final String styleClass;
        private final String buttonStyleClass;

        AlertType(Ikon icon, String styleClass, String buttonStyleClass) {
            this.icon = icon;
            this.styleClass = styleClass;
            this.buttonStyleClass = buttonStyleClass;
        }

        public Ikon icon() {
            return icon;
        }

        /** 作用于 layout/图标的样式类，null 表示无 */
        public String styleClass() {
            return styleClass;
        }

        /** 该类型对应按钮的着色样式类，供 build() 兜底使用 */
        public String buttonStyleClass() {
            return buttonStyleClass;
        }
    }

    /** 按钮样式：对应 AtlantaFX 的 accent/success/warning/danger 四种色调 */
    public enum ButtonStyle {
        ACCENT(Styles.ACCENT),
        SUCCESS(Styles.SUCCESS),
        WARNING(Styles.WARNING),
        DANGER(Styles.DANGER);

        private final String styleClass;

        ButtonStyle(String styleClass) {
            this.styleClass = styleClass;
        }

        public String styleClass() {
            return styleClass;
        }
    }

    private AlertType type = AlertType.BASIC;
    private String title;
    private String message;
    private final List<Button> buttonList = new ArrayList<>();

    private AlterBuilder() {}

    public static AlterBuilder create() {
        return new AlterBuilder();
    }

    // ========== 类型设置（无参）==========

    /** 基本类型 */
    public AlterBuilder basic() {
        this.type = AlertType.BASIC;
        return this;
    }

    /** 信息类型 */
    public AlterBuilder info() {
        this.type = AlertType.INFO;
        return this;
    }

    /** 警告类型 */
    public AlterBuilder warning() {
        this.type = AlertType.WARNING;
        return this;
    }

    /** 危险类型 */
    public AlterBuilder danger() {
        this.type = AlertType.DANGER;
        return this;
    }

    /** 成功类型 */
    public AlterBuilder success() {
        this.type = AlertType.SUCCESS;
        return this;
    }

    // ========== 内容设置 ==========

    public AlterBuilder title(String title) {
        this.title = title;
        return this;
    }

    public AlterBuilder message(String message) {
        this.message = message;
        return this;
    }



    // ========== 按钮多态添加 ==========

    /**
     * 最简形式：仅文本，无样式、默认取消、无动作。
     */
    public AlterBuilder button(String text) {
        return button(text, null, null, true, null);
    }

    /**
     * 文本 + 动作，无样式、默认取消按钮。
     */
    public AlterBuilder button(String text, EventHandler<ActionEvent> action) {
        return button(text, null, null, true, action);
    }

    /**
     * 文本 + 是否取消 + 动作，无样式。取消按钮会被 OverlayLayer 统一包装自动关闭逻辑。
     */
    public AlterBuilder button(String text, boolean isCancel, EventHandler<ActionEvent> action) {
        return button(text, null, null, isCancel, action);
    }

    /**
     * 文本 + 图标 + 动作，默认取消、无样式。
     */
    public AlterBuilder button(String text, Node graphic, EventHandler<ActionEvent> action) {
        return button(text, graphic, null, true, action);
    }

    /**
     * 文本 + 样式 + 动作，默认取消、无图标。
     */
    public AlterBuilder button(String text, ButtonStyle style, EventHandler<ActionEvent> action) {
        return button(text, null, style, true, action);
    }

    /**
     * 文本 + 图标 + 是否取消 + 动作，无样式。
     */
    public AlterBuilder button(String text, Node graphic, boolean isCancel, EventHandler<ActionEvent> action) {
        return button(text, graphic, null, isCancel, action);
    }

    /**
     * 文本 + 图标 + 样式 + 动作，默认取消。
     */
    public AlterBuilder button(String text, Node graphic, ButtonStyle style, EventHandler<ActionEvent> action) {
        return button(text, graphic, style, true, action);
    }

    /**
     * 全参数形式：文本 + 图标 + 样式 + 是否取消 + 动作。
     * 样式为 {@link ButtonStyle} 时，将对应 AtlantaFX 色调类加入按钮 styleClass。
     */
    public AlterBuilder button(String text, Node graphic, ButtonStyle style, boolean isCancel, EventHandler<ActionEvent> action) {
        Button button = new Button(text);
        if (graphic != null) {
            button.setGraphic(graphic);
        }
        if (style != null) {
            button.getStyleClass().add(style.styleClass());
        }
        button.setCancelButton(isCancel);
        if (action != null) {
            button.setOnAction(action);
        }
        buttonList.add(button);
        return this;
    }

    /**
     * 批量添加现成 Button，适用于调用方已构造好按钮的场景。
     */
    public AlterBuilder buttons(Button... buttons) {
        buttonList.addAll(List.of(buttons));
        return this;
    }

    // ========== 便捷的确认按钮快捷方法 ==========

    /**
     * 默认文本"确定"的确认按钮，颜色在 build() 时按 AlertType 兜底着色。
     */
    public AlterBuilder ok() {
        return ok("确定", null, null);
    }

    /**
     * 自定义文本的确认按钮，颜色在 build() 时按 AlertType 兜底着色。
     */
    public AlterBuilder ok(String text) {
        return ok(text, null, null);
    }
    public AlterBuilder ok(EventHandler<ActionEvent> action) {
        return ok("确定", null, action);
    }
    /**
     * 自定义文本 + 动作的确认按钮，颜色在 build() 时按 AlertType 兜底着色。
     */
    public AlterBuilder ok(String text, EventHandler<ActionEvent> action) {
        return ok(text, null, action);
    }

    /**
     * 自定义文本 + 图标 + 动作的确认按钮，颜色在 build() 时按 AlertType 兜底着色。
     */
    public AlterBuilder ok(String text, Node graphic, EventHandler<ActionEvent> action) {
        button(text, graphic, null, true, action);
        // 仅 ok() 标记哨兵：build() 时按当前 AlertType 着色
        buttonList.getLast().setAccessibleText(STYLE_PENDING);
        return this;
    }

    // ========== 便捷的取消按钮快捷方法 ==========

    /**
     * 默认文本"取消"的取消按钮。
     */
    public AlterBuilder cancel() {
        return button("取消", null, null, true, null);
    }

    /**
     * 自定义文本的取消按钮。
     */
    public AlterBuilder cancel(String text) {
        return button(text, null, null, true, null);
    }

    /**
     * 自定义文本 + 图标的取消按钮。
     */
    public AlterBuilder cancel(String text, Node graphic) {
        return button(text, graphic, null, true, null);
    }

    /**
     * 自定义文本 + 动作的取消按钮。
     */
    public AlterBuilder cancel(String text, EventHandler<ActionEvent> action) {
        return button(text, null, null, true, action);
    }

    /**
     * 自定义文本 + 样式的取消按钮。
     */
    public AlterBuilder cancel(String text, ButtonStyle style) {
        return button(text, null, style, true, null);
    }

    /**
     * 自定义文本 + 图标 + 样式的取消按钮。
     */
    public AlterBuilder cancel(String text, Node graphic, ButtonStyle style) {
        return button(text, graphic, style, true, null);
    }

    /**
     * 自定义文本 + 图标 + 动作的取消按钮。
     */
    public AlterBuilder cancel(String text, Node graphic, EventHandler<ActionEvent> action) {
        return button(text, graphic, null, true, action);
    }

    // ========== 构建 ==========

    public JFXDialogLayout build() {
        JFXDialogLayout layout = new JFXDialogLayout();
        layout.getStyleClass().add("jfx-alter");

        if (title != null) {
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("title");
            layout.setHeading(titleLabel);
        }

        if (message != null) {
            Label messageLabel = new Label(message);
            messageLabel.setWrapText(true);
            HBox.setHgrow(messageLabel, Priority.ALWAYS);

            FontIcon icon = new FontIcon(type.icon());
            icon.getStyleClass().add("alter-builder-icon");

            HBox body = new HBox(10.0, icon, messageLabel);
            body.setAlignment(Pos.CENTER_LEFT);

            layout.setBody(body);
        }

        if (type.styleClass() != null) {
            layout.getStyleClass().add(type.styleClass());
        }

        // 兜底：未显式指定样式的按钮，按当前 AlertType 着色
        if (!buttonList.isEmpty()) {
            for (Button button : buttonList) {
                if (STYLE_PENDING.equals(button.getAccessibleText())) {
                    button.getStyleClass().add(type.buttonStyleClass());
                    button.setAccessibleText(null);
                }
            }
            layout.setActions(buttonList);
        }
        return layout;
    }
}
