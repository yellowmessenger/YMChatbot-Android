package com.yellowmessenger.ymchat.models;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.yellowmessenger.ymchat.YMChat;

public class JavaScriptInterface {
    protected Activity parentActivity;
    protected WebView mWebView;

    public JavaScriptInterface(Activity _activity, WebView _webView) {
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
        YMBotEventResponse incomingEvent;

        try {
            ObjectMapper mapper = new ObjectMapper();
            incomingEvent = mapper.readValue(s, YMBotEventResponse.class);
//            incomingEvent = new YMBotEventResponse("dummy code 38", "dummy data 38", false);
        } catch (Exception e) {
            incomingEvent = new YMBotEventResponse("reached exception", "dummy data", false);
        }

        if (incomingEvent.getCode() != null && ("close-bot".equals(incomingEvent.getCode()) || "upload-image".equals(incomingEvent.getCode()))) {
            incomingEvent.setInternal(true);
        }

        if (incomingEvent.isInternal()) {
            YMChat.getInstance().emitLocalEvent(incomingEvent);
        } else {
            YMChat.getInstance().emitEvent(incomingEvent);
        }
    }

}
