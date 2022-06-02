package com.yellowmessenger.ymchat.models;

import android.app.Activity;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.google.gson.Gson;
import com.yellowmessenger.ymchat.BotWebView;
import com.yellowmessenger.ymchat.YMChat;

public class JavaScriptInterface {
    protected Activity parentActivity;
    protected WebView mWebView;
    private String key;

    public JavaScriptInterface(Activity _activity, WebView _webView, String ymAuthKey) {
        parentActivity = _activity;
        mWebView = _webView;
        key = ymAuthKey;

    }

    @JavascriptInterface
    public void loadURL(String url) {
        final String u = url;

        parentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl(u);
            }
        });
    }

    @JavascriptInterface
    public void receiveMessage(String s) {
        YMBotEventResponse incomingEvent;

        try {
            incomingEvent = new Gson().fromJson(s, YMBotEventResponse.class);
        } catch (Exception e) {
            incomingEvent = new YMBotEventResponse(null, null, false);
        }

        if (incomingEvent.getCode() != null && incomingEvent.getCode().equals("start-mic")) {
            // For lambda expression only final variables is allowed
            final YMBotEventResponse finalIncomingEvent = incomingEvent;
           // parentActivity.runOnUiThread(() -> parentActivity.startMic(Long.parseLong(finalIncomingEvent.getCode()) * 1000));
        }

        if (incomingEvent.getCode() != null && ("close-bot".equals(incomingEvent.getCode()) || "upload-image".equals(incomingEvent.getCode()))) {
            incomingEvent.setInternal(true);
        }

        if (incomingEvent.isInternal()) {
            YMChat.getInstance(key).emitLocalEvent(incomingEvent);
        } else {
            YMChat.getInstance(key).emitEvent(incomingEvent);
        }
    }

}
