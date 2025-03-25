package cn.tealc.wutheringwavestool.util;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;

import java.util.ArrayList;
import java.util.List;

public class ButtonBuilder {
    private Boolean cancel;
    private Boolean ok;
    private String title;
    private final List<String> styleClassList = new ArrayList<>();
    private EventHandler<ActionEvent> action;

    // 私有构造函数
    private ButtonBuilder() {}

    // 静态方法返回一个新的 ButtonBuilder 实例
    public static ButtonBuilder create() {
        return new ButtonBuilder();
    }


    public ButtonBuilder cancel() {
        this.cancel = true;
        return this;
    }

    public ButtonBuilder ok() {
        this.ok = true;
        return this;
    }

    // 设置标题
    public ButtonBuilder title(String title) {
        this.title = title;
        return this;
    }

    // 设置样式类
    public ButtonBuilder styleClass(String styleClass) {
        styleClassList.add(styleClass);
        return this;
    }

    // 设置动作
    public ButtonBuilder action(EventHandler<ActionEvent> action) {
        this.action = action;
        return this;
    }

    // 构建 Button 对象
    public Button build() {
        Button button = new Button(title);
        if (ok != null){
            button.setDefaultButton(true);
        }
        if (cancel != null){
            button.setCancelButton(true);
        }

        if (!styleClassList.isEmpty()) {
            button.getStyleClass().addAll(styleClassList);
        }

        if (action != null) {
            button.setOnAction(action);
        }
        return button;
    }
}
