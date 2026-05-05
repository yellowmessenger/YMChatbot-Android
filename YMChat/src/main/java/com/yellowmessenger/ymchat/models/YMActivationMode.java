package com.yellowmessenger.ymchat.models;

public enum YMActivationMode {
    CHAT("chat"),
    VOICE("voice");

    private final String value;

    YMActivationMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
