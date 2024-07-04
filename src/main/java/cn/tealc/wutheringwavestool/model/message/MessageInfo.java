package cn.tealc.wutheringwavestool.model.message;

import javafx.util.Duration;

public class MessageInfo {
    private MessageType type;
    private String message;
    private Boolean autoClose; //自动关闭
    private Duration showTime;//显示时间
    public static final Duration DEFAULT=Duration.seconds(3.0);
    public static final Duration LONG=Duration.seconds(5.0);
    public static final Duration LONG_PLUS=Duration.seconds(7.0);

    public MessageInfo(MessageType type, String message) {
        this.type = type;
        this.message = message;
        this.autoClose = true;
        this.showTime=DEFAULT;
    }
    public MessageInfo(MessageType type, String message, Boolean autoClose) {
        this.type = type;
        this.message = message;
        this.autoClose = autoClose;
        this.showTime=DEFAULT;
    }

    public MessageInfo(MessageType type, String message, Duration showTime) {
        this.type = type;
        this.message = message;
        this.autoClose = true;
        this.showTime = showTime;
    }

    public MessageInfo(MessageType type, String message, Boolean autoClose, Duration showTime) {
        this.type = type;
        this.message = message;
        this.autoClose = autoClose;
        this.showTime = showTime;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    public Boolean getAutoClose() {
        return autoClose;
    }

    public void setAutoClose(Boolean autoClose) {
        this.autoClose = autoClose;
    }

    public Duration getShowTime() {
        return showTime;
    }

    public void setShowTime(Duration showTime) {
        this.showTime = showTime;
    }
}
