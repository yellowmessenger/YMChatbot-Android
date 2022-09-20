package com.yellowmessenger.ymchat.models;

public class YellowGenericResponseModel<T> {
    private T data;
    private boolean success;
    private String message;

    public T getData() {
        return data;
    }

    public boolean getSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
