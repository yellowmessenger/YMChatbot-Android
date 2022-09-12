package com.yellowmessenger.ymchat.models;

public class YellowGenericResponseModel<T> {
    private T data;
    private boolean success;
    private String error;

    public T getData() {
        return data;
    }

    public boolean getSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }
}
