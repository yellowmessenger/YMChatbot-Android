package com.yellowmessenger.ymchat.models;

public interface YellowDataCallback {
    <T> void success(T data);

    void failure(String message);
}
