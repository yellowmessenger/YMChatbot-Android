package com.yellowmessenger.ymchat.models;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.google.gson.Gson;
import com.yellowmessenger.ymchat.BotWebView;
import com.yellowmessenger.ymchat.YMChat;

public class JavaScriptInterface {
    protected BotWebView parentActivity;
    protected WebView mWebView;

    public JavaScriptInterface(BotWebView _activity, WebView _webView) {
        parentActivity = _activity;
        mWebView = _webView;

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
        YMBotEventResponse incomingEvent = new Gson().fromJson(s, YMBotEventResponse.class);
        Log.d("Event from Bot", "receiveMessage: " + incomingEvent.getCode());
        if (incomingEvent.getCode().equals("start-mic")) {
            parentActivity.runOnUiThread(() -> parentActivity.startMic(Long.parseLong(incomingEvent.getCode()) * 1000));
        }

        if ("close-bot".equals(incomingEvent.getCode()) || "upload-image".equals(incomingEvent.getCode())) {
            incomingEvent.setInternal(true);
        }

        if (incomingEvent.isInternal()) {
            YMChat.getInstance().emitLocalEvent(incomingEvent);
        } else {
            YMChat.getInstance().emitEvent(incomingEvent);
        }
    }

}
