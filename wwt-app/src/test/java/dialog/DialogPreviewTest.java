package dialog;

import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.ui.component.dialog.NewAlter;
import cn.tealc.wutheringwavestool.ui.component.dialog.NewChoiceDialog;
import cn.tealc.wutheringwavestool.ui.component.dialog.NewDialog;
import cn.tealc.wutheringwavestool.ui.component.dialog.NewTextInputDialog;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.List;

/**
 * 手动预览 JavaFX 26 HeaderBar + shadcn 风格 Dialog。
 *
 * 运行方式：在 IDE 中直接运行本类的 main 方法，或运行测试源集中的此类。
 */
public final class DialogPreviewTest extends Application {
    private final Label resultLabel = new Label("点击按钮打开弹窗");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        String lightStylesheet = resourceUrl("css/light.css");
        Application.setUserAgentStylesheet(lightStylesheet);

        Label title = new Label("Dialog Preview");
        title.getStyleClass().add("title-2");

        Label description = new Label(
                "JavaFX 26.0.2 HeaderBar / shadcn 风格弹窗预览");
        description.getStyleClass().add("text-muted");

        Button customDialogButton = new Button("打开 NewDialog");
        customDialogButton.setMaxWidth(Double.MAX_VALUE);
        customDialogButton.setOnAction(event -> showCustomDialog(stage));

        Button alertButton = new Button("打开 NewAlter");
        alertButton.setMaxWidth(Double.MAX_VALUE);
        alertButton.setOnAction(event -> showAlert(stage));

        Button infoAlertButton = new Button("打开 info Alert");
        infoAlertButton.setMaxWidth(Double.MAX_VALUE);
        infoAlertButton.setOnAction(event -> showStyledAlert(stage, NewAlter.AlertStyle.INFO));

        Button warningAlertButton = new Button("打开 warning Alert");
        warningAlertButton.setMaxWidth(Double.MAX_VALUE);
        warningAlertButton.setOnAction(event -> showStyledAlert(stage, NewAlter.AlertStyle.WARNING));

        Button dangerAlertButton = new Button("打开 danger Alert");
        dangerAlertButton.setMaxWidth(Double.MAX_VALUE);
        dangerAlertButton.setOnAction(event -> showStyledAlert(stage, NewAlter.AlertStyle.DANGER));

        Button successAlertButton = new Button("打开 success Alert");
        successAlertButton.setMaxWidth(Double.MAX_VALUE);
        successAlertButton.setOnAction(event -> showStyledAlert(stage, NewAlter.AlertStyle.SUCCESS));

        Button defaultAlertButton = new Button("打开 default Alert");
        defaultAlertButton.setMaxWidth(Double.MAX_VALUE);
        defaultAlertButton.setOnAction(event -> showStyledAlert(stage, NewAlter.AlertStyle.DEFAULT));

        Button choiceButton = new Button("打开 NewChoiceDialog");
        choiceButton.setMaxWidth(Double.MAX_VALUE);
        choiceButton.setOnAction(event -> showChoiceDialog(stage));

        Button textInputButton = new Button("打开 NewTextInputDialog");
        textInputButton.setMaxWidth(Double.MAX_VALUE);
        textInputButton.setOnAction(event -> showTextInputDialog(stage));

        resultLabel.setWrapText(true);
        resultLabel.getStyleClass().add("text-muted");

        VBox root = new VBox(12, title, description, customDialogButton, alertButton,
                infoAlertButton, warningAlertButton, dangerAlertButton, successAlertButton,
                defaultAlertButton,
                choiceButton, textInputButton, resultLabel);
        root.setPadding(new Insets(28));
        root.setAlignment(Pos.TOP_LEFT);
        root.setPrefWidth(380);
        root.getStyleClass().add("bg-default");

        Scene scene = new Scene(root);
        scene.getStylesheets().add(resourceUrl("css/Default.css"));

        stage.initStyle(StageStyle.EXTENDED);
        stage.setTitle("Dialog Preview");
        stage.setMinWidth(380);
        stage.setMinHeight(570);
        stage.setScene(scene);
        stage.show();
    }

    private static String resourceUrl(String path) {
        URL resource = FXResourcesLoader.loadURL(path);
        if (resource == null) {
            throw new IllegalStateException(
                    "无法加载主应用资源：" + path
                            + "。请确认 IntelliJ 运行配置包含 wwt-app 的 main resources。");
        }
        return resource.toExternalForm();
    }

    private void showCustomDialog(Stage owner) {
        NewDialog<Void> dialog = new NewDialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("自定义弹窗");
        dialog.getDialogPane().setHeaderText("基于 DialogPane.setHeaderBar 的自定义弹窗");
        dialog.getDialogPane().setContent(new Label(
                "这里可以放置任意 JavaFX Node，例如表单、列表或业务组件。"));

        StackPane stackPane = new StackPane();
        stackPane.setPrefHeight(150);
        dialog.getDialogPane().setContent(stackPane);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait();
        resultLabel.setText("NewDialog 已关闭");
    }

    private void showAlert(Stage owner) {
        NewAlter alert = new NewAlter(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle("确认操作");
        //alert.setHeaderText("这是一个确认弹窗");
        alert.setContentText("Alert 的按钮、图标和内容区域都复用了统一样式。");
        alert.showAndWait().ifPresent(button ->
                resultLabel.setText("Alert 返回：" + button.getText()));
    }

    private void showStyledAlert(Stage owner, NewAlter.AlertStyle style) {
        String content = style.name().toLowerCase() + " 样式 Alert 预览";
        NewAlter alert = switch (style) {
            case INFO -> NewAlter.info(content);
            case WARNING -> NewAlter.warning(content);
            case DANGER -> NewAlter.danger(content);
            case SUCCESS -> NewAlter.success(content);
            case DEFAULT -> NewAlter.defaultAlert(content);
        };
        alert.initOwner(owner);
        alert.setTitle("NewAlter - " + style.name().toLowerCase());
        alert.showAndWait().ifPresent(button ->
                resultLabel.setText(style.name().toLowerCase() + " Alert 返回：" + button.getText()));
    }

    private void showChoiceDialog(Stage owner) {
        NewChoiceDialog<String> dialog = new NewChoiceDialog<>(
                "默认源", List.of("默认源", "镜像源", "Github 源"));
        dialog.initOwner(owner);
        dialog.setTitle("选择下载源");
        dialog.setHeaderText("ChoiceDialog 预览");
        dialog.setContentText("请选择一个下载源：");
        dialog.showAndWait().ifPresent(value -> resultLabel.setText("ChoiceDialog 返回：" + value));
    }

    private void showTextInputDialog(Stage owner) {
        NewTextInputDialog dialog = new NewTextInputDialog("WutheringWavesTool");
        dialog.initOwner(owner);
        dialog.setTitle("输入文本");
        dialog.setHeaderText("TextInputDialog 预览");
        dialog.setContentText("请输入名称：");
        dialog.showAndWait().ifPresent(value -> resultLabel.setText("TextInputDialog 返回：" + value));
    }
}
