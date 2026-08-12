package cn.tealc.wutheringwavestool.ui.item;

import atlantafx.base.layout.InputGroup;
import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.dao.GameTimeDao;
import cn.tealc.wutheringwavestool.model.game.GameTime;
import cn.tealc.teafx.utils.message.MessageInfo;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @description: 修改游戏时间的View
 */
public class PlayTimeAlertItemView extends JFXDialogLayout {
    private final GameTimeDao dao = AppInjector.getInstance(GameTimeDao.class);
    private final Button okBtn;
    private final Button cancelBtn;
    private final String date;
    private ChoiceBox<String> roleChoiceBox;
    private TextField minuteField;

    public PlayTimeAlertItemView() {
        date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        List<GameTime> times = dao.getTimeListByData(date);
        ObservableList<String> roleList = FXCollections.observableList(getDistinctRoles(times));

        Label title = new Label("修改今日时长");
        title.getStyleClass().add(Styles.TITLE_3);

        okBtn = createButton("修改", Styles.DANGER);
        cancelBtn = createButton("取消", null);
        cancelBtn.setCancelButton(true);
        Button resetBtn = createButton("今日时长归零", Styles.FLAT, Styles.DANGER);

        VBox view = createView(roleList, resetBtn);
        setHeading(title);
        setBody(view);
        setActions(resetBtn, okBtn, cancelBtn);
        setPrefSize(300, 160);
    }

    /**
     * @description: 获取所有玩家
     * @param:	times
     * @return  java.util.List<java.lang.String>
     * @date:   2025/2/17
     */
    private List<String> getDistinctRoles(List<GameTime> times) {
        return times.stream()
                .map(GameTime::getRoleId)
                .distinct()
                .toList();
    }

    private Button createButton(String text, String... styles) {
        Button button = new Button(text);
        if (styles != null) {
            button.getStyleClass().addAll(styles);
        }
        return button;
    }

    private VBox createView(ObservableList<String> roleList, Button resetBtn) {
        VBox view = new VBox(5);
        view.setAlignment(Pos.CENTER);

        if (!roleList.isEmpty()) {
            roleChoiceBox = new ChoiceBox<>(roleList);
            roleChoiceBox.setPrefWidth(150);
            roleChoiceBox.getSelectionModel().selectFirst();

            minuteField = new TextField();
            minuteField.setPrefWidth(150);

            resetBtn.setOnAction(event -> {
                resetTime();
                cancelBtn.fireEvent(event);
            });

            okBtn.setOnAction(event -> {
                GameTime gameTime = check();
                if (gameTime != null) {
                    alert(gameTime);
                }
            });

            view.getChildren().addAll(
                    new InputGroup(new Label("角色"), roleChoiceBox),
                    new InputGroup(new Label("分钟"), minuteField)
            );
        } else {
            view.getChildren().add(new Label("今日没有游玩数据"));
            okBtn.setDisable(true);
            resetBtn.setDisable(true);
        }
        return view;
    }

    private void resetTime() {
        dao.deleteTimeByData(date);
        NotificationManager.message(MessageInfo.success("今日时长重置完成"));
        MvvmFX.getNotificationCenter().publish(NotificationKey.HOME_GAME_TIME_UPDATE);
    }

    private void alert(GameTime gameTime) {
        dao.deleteTimeByDataAndRoleId(gameTime.getGameDate(), gameTime.getRoleId());
        if (dao.addTime(gameTime) > 0) {
            NotificationManager.message(MessageInfo.success("修改时长成功"));
        } else {
            NotificationManager.message(MessageInfo.error("修改时长失败"));
        }
        MvvmFX.getNotificationCenter().publish(NotificationKey.HOME_GAME_TIME_UPDATE);
    }

    private GameTime check() {
        try {
            int minute = Integer.parseInt(minuteField.getText());
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
            LocalDateTime endTime = startOfDay.plusMinutes(minute);

            if (minute > 1440) {
                notifyWarning("输入的分钟数超过一天的时长。");
            } else if (endTime.isBefore(now)) {
                return createGameTime(minute, startOfDay, endTime);
            } else {
                notifyWarning("输入的分钟数不在当日0点与当前时间的范围内。");
            }
        } catch (NumberFormatException e) {
            notifyWarning("请输入有效的分钟数。");
        }
        return null;
    }

    private void notifyWarning(String message) {
        System.out.println(message);
        NotificationManager.message(MessageInfo.warning(message));
    }

    private GameTime createGameTime(int minute, LocalDateTime startOfDay, LocalDateTime endTime) {
        long startOfDayMillis = startOfDay.toInstant(ZoneOffset.UTC).toEpochMilli();
        long endTimeMillis = endTime.toInstant(ZoneOffset.UTC).toEpochMilli();

        GameTime gameTime = new GameTime();
        gameTime.setGameDate(date);
        gameTime.setRoleId(roleChoiceBox.getSelectionModel().getSelectedItem());
        gameTime.setDuration(minute * 60 * 1000L);
        gameTime.setStartTime(startOfDayMillis);
        gameTime.setEndTime(endTimeMillis);
        return gameTime;
    }
}
