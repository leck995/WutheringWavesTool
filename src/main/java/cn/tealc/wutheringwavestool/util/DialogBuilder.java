package cn.tealc.wutheringwavestool.util;

import atlantafx.base.theme.Styles;
import com.jfoenixN.controls.JFXDialogLayout;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.ArrayList;
import java.util.List;

public class DialogBuilder {
    private Boolean cancel;
    private Boolean ok;
    private String title;
    private String message;
    private final List<String> styleClassList = new ArrayList<>();
    private final List<Button> buttonList = new ArrayList<>();

    // 私有构造函数
    private DialogBuilder() {}

    // 静态方法返回一个新的 ButtonBuilder 实例
    public static DialogBuilder create() {
        return new DialogBuilder();
    }


/*
    public DialogBuilder cancel() {
        this.cancel = true;
        return this;
    }

    public DialogBuilder ok() {
        this.ok = true;
        return this;
    }
*/

    // 设置标题
    public DialogBuilder title(String title) {
        this.title = title;
        return this;
    }
    public DialogBuilder message(String message) {
        this.message = message;
        return this;
    }
    // 设置样式类
    public DialogBuilder styleClass(String styleClass) {
        styleClassList.add(styleClass);
        return this;
    }


    public DialogBuilder button(String name,EventHandler<ActionEvent> action) {
        Button button = new Button(name);
        button.setOnAction(action);
        buttonList.add(button);
        return this;
    }

    public DialogBuilder button(String name, Node graphic,boolean isCancel,EventHandler<ActionEvent> action) {
        Button button = new Button(name);
        button.setGraphic(graphic);
        button.setOnAction(action);
        button.setCancelButton(isCancel);
        buttonList.add(button);
        return this;
    }
    public DialogBuilder buttons(Button... buttons) {
        buttonList.addAll(List.of(buttons));
        return this;
    }

    public DialogBuilder cancel(String name) {
        return button(name, null, true, null);
    }
    public DialogBuilder cancel() {
        return button("取消", null, true, null);
    }
    public DialogBuilder cancel(String name, Node graphic) {
        return button(name, graphic, true, null);
    }

    // 构建 Button 对象
    public JFXDialogLayout build() {
        JFXDialogLayout layout = new JFXDialogLayout();
        if (title != null) {
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add(Styles.TITLE_3);
            layout.setHeading(titleLabel);
        }
        if (message != null) {
            Label label = new Label(message);
            layout.setBody(label);
        }

        if (!styleClassList.isEmpty()) {
            layout.getStyleClass().addAll(styleClassList);
        }
        if (!buttonList.isEmpty()) {
            layout.setActions(buttonList);
        }
        return layout;
    }
}
