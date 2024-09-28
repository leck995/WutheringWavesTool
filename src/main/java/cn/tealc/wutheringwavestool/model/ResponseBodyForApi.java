package cn.tealc.wutheringwavestool.model;

import cn.tealc.wutheringwavestool.util.ApiUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-08 15:42
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseBodyForApi{
    private Integer code;
    private String msg;
    private String data;
    private Boolean success;


    public ResponseBodyForApi() {
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

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }
}