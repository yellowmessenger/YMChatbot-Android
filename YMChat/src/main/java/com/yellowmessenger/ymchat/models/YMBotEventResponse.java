package com.yellowmessenger.ymchat.models;


public class YMBotEventResponse {

    private String code, data;
    private boolean internal;

    public YMBotEventResponse(String code, String data, boolean internal) {
        this.code = code;
        this.data = data;
        this.internal = internal;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    boolean isInternal() {
        return internal;
    }

    void setInternal(boolean internal) {
        this.internal = internal;
    }
}
