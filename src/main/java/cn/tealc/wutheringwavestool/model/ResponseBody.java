package cn.tealc.wutheringwavestool.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-08 15:42
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseBody<T> {
    private Integer code;
    private String msg;
    private T data;
    private Boolean success;


    public ResponseBody() {
    }

    public ResponseBody(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
        this.success = false;
    }

    public ResponseBody(Integer code, String msg, Boolean success) {
        this.code = code;
        this.msg = msg;
        this.success = success;
    }




    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }
}