package com.yellowmessenger.ymchat.models;


import androidx.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class YMBotEventResponse {

    private @Nullable
    String code, data;
    private boolean internal;

    public YMBotEventResponse(@JsonProperty("code") String code, @JsonProperty("data") String data, @JsonProperty("internal") boolean internal) {
        this.code = code;
        this.data = data;
        this.internal = internal;
    }

    public YMBotEventResponse() {
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
