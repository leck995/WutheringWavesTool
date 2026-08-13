package cn.tealc.wutheringwavestool.ui.game;

import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.service.GameTimeService;
import cn.tealc.wutheringwavestool.model.game.GameTime;
import cn.tealc.teafx.utils.message.MessageInfo;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class GameTimeAddView extends JFXDialogLayout {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GameTimeService gameTimeService = AppInjector.getInstance(GameTimeService.class);

    private final TextField roleIdField;
    private final TextField startTimeField;
    private final TextField endTimeField;
    private final Label errorLabel;
    private final Label dateHint;
    private final Label durationHint;
    private final Button okBtn;
    private final Button cancelBtn;

    public GameTimeAddView(String defaultRoleId) {
        Label title = new Label("添加游玩记录");
        title.getStyleClass().add(Styles.TITLE_3);

        roleIdField = new TextField(defaultRoleId != null ? defaultRoleId : "");
        roleIdField.setPromptText("例: 10001");

        startTimeField = new TextField();
        startTimeField.setPromptText("例: 2024-01-15 10:00:00");

        endTimeField = new TextField();
        endTimeField.setPromptText("例: 2024-01-15 12:30:00");

        errorLabel = new Label();
        errorLabel.getStyleClass().add(Styles.DANGER);
        errorLabel.setStyle("-fx-text-fill: -color-danger-fg;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(380);

        dateHint = new Label("游戏日期: —");
        dateHint.getStyleClass().add(Styles.TEXT_SUBTLE);
        durationHint = new Label("持续时长: —");
        durationHint.getStyleClass().add(Styles.TEXT_SUBTLE);

        startTimeField.textProperty().addListener(obs -> updateHints());
        endTimeField.textProperty().addListener(obs -> updateHints());

        okBtn = new Button("确定");
        okBtn.getStyleClass().addAll(Styles.ACCENT);
        okBtn.setDefaultButton(true);
        okBtn.setOnAction(this::onConfirm);

        cancelBtn = new Button("取消");
        cancelBtn.setCancelButton(true);

        // Grid: label | field,  properly aligned
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER_LEFT);

        ColumnConstraints colLabel = new ColumnConstraints();
        colLabel.setHalignment(HPos.RIGHT);
        colLabel.setPrefWidth(70);
        ColumnConstraints colField = new ColumnConstraints();
        colField.setHgrow(Priority.ALWAYS);
        colField.setFillWidth(true);
        grid.getColumnConstraints().addAll(colLabel, colField);

        Label roleLabel = new Label("角色ID");
        Label startLabel = new Label("开始时间");
        Label endLabel = new Label("结束时间");

        grid.add(roleLabel, 0, 0);
        grid.add(roleIdField, 1, 0);
        grid.add(startLabel, 0, 1);
        grid.add(startTimeField, 1, 1);
        grid.add(endLabel, 0, 2);
        grid.add(endTimeField, 1, 2);

        VBox body = new VBox(8);
        body.setPadding(new Insets(0, 8, 0, 8));
        body.getChildren().addAll(grid, errorLabel, dateHint, durationHint);

        setHeading(title);
        setBody(body);
        setActions(okBtn, cancelBtn);
        setPrefSize(420, 270);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setVisible(false);
    }

    private void updateHints() {
        clearError();
        try {
            LocalDateTime start = LocalDateTime.parse(startTimeField.getText().trim(), FMT);
            LocalDateTime end = LocalDateTime.parse(endTimeField.getText().trim(), FMT);
            dateHint.setText("游戏日期: " + start.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            long minutes = java.time.Duration.between(start, end).toMinutes();
            if (minutes > 0) {
                durationHint.setText("持续时长: " + minutes + " 分钟 (" + String.format("%.1f", minutes / 60.0) + " 小时)");
            } else {
                durationHint.setText("持续时长: 结束时间必须晚于开始时间");
            }
        } catch (DateTimeParseException e) {
            dateHint.setText("游戏日期: —");
            durationHint.setText("持续时长: —");
        }
    }

    private void onConfirm(ActionEvent event) {
        clearError();

        String roleId = roleIdField.getText().trim();
        String startStr = startTimeField.getText().trim();
        String endStr = endTimeField.getText().trim();

        if (roleId.isEmpty()) {
            showError("角色ID不能为空");
            return;
        }
        if (!roleId.matches("\\d+")) {
            showError("角色ID必须是数字");
            return;
        }

        if (startStr.isEmpty()) {
            showError("开始时间不能为空");
            return;
        }
        LocalDateTime startTime;
        try {
            startTime = LocalDateTime.parse(startStr, FMT);
        } catch (DateTimeParseException e) {
            showError("开始时间格式错误，应为 yyyy-MM-dd HH:mm:ss");
            return;
        }

        if (endStr.isEmpty()) {
            showError("结束时间不能为空");
            return;
        }
        LocalDateTime endTime;
        try {
            endTime = LocalDateTime.parse(endStr, FMT);
        } catch (DateTimeParseException e) {
            showError("结束时间格式错误，应为 yyyy-MM-dd HH:mm:ss");
            return;
        }

        if (!endTime.isAfter(startTime)) {
            showError("结束时间必须晚于开始时间");
            return;
        }

        if (java.time.Duration.between(startTime, endTime).toHours() > 24) {
            showError("你是超人吗？");
            return;
        }

        long startMillis = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMillis = endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        GameTime record = new GameTime();
        record.setRoleId(roleId);
        record.setGameDate(startTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        record.setStartTime(startMillis);
        record.setEndTime(endMillis);
        record.setDuration(endMillis - startMillis);

        if (gameTimeService.addTime(record) > 0) {
            NotificationManager.message(MessageInfo.success("添加游玩记录成功"));
            MvvmFX.getNotificationCenter().publish(NotificationKey.HOME_GAME_TIME_UPDATE);
            cancelBtn.fireEvent(new ActionEvent());
        } else {
            showError("添加游玩记录失败，请重试");
        }
    }
}
