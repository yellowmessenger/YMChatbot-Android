package com.yellowmessenger.ymchat.models;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.yellowmessenger.ymchat.BotWebView;
import com.yellowmessenger.ymchat.YMChat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;

public class JavaScriptInterface {
    protected BotWebView parentActivity;
    protected WebView mWebView;

    public JavaScriptInterface(BotWebView _activity, WebView _webView)  {
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
    public void  receiveMessage(String s) {
        BotEventsModel incomingEvent = new Gson().fromJson(s, BotEventsModel.class);


        // Pass-through events (Bot will not close)
        Map<String, Object> retMap = new Gson().fromJson(
                incomingEvent.data, new TypeToken<HashMap<String, Object>>() {}.getType());
        Boolean isYmAction = retMap.containsKey("ym-action");

        Log.d("Event from Bot", "receiveMessage: "+incomingEvent.code);
        if(!incomingEvent.code.equals("Message Received") && !incomingEvent.code.equals("start-mic") && !isYmAction) {
            parentActivity.runOnUiThread(() -> parentActivity.closeBot());
            parentActivity.finish();
        }
        else {
            if(incomingEvent.code.equals("start-mic"))
            parentActivity.runOnUiThread(() -> parentActivity.startMic(Long.parseLong(incomingEvent.data) * 1000));
        }
        YMChat.getInstance().emitEvent(incomingEvent);
    }

}
