package com.yellowmessenger.ymchat.models;

import android.app.Activity;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.google.gson.Gson;
import com.yellowmessenger.ymchat.YMChat;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class JavaScriptInterface {
    private static final List<String> ALLOWED_URL_SCHEMES = Arrays.asList("http", "https");

    protected Activity parentActivity;
    protected WebView mWebView;
    private TextToSpeech tts;
    private volatile boolean isTtsReady = false;

    public JavaScriptInterface(Activity _activity, WebView _webView) {
        parentActivity = _activity;
        mWebView = _webView;

        tts = new TextToSpeech(parentActivity, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());
                isTtsReady = true;
            }
        });
    }

    @JavascriptInterface
    public void loadURL(String url) {
        if (url == null) {
            return;
        }
        String scheme = Uri.parse(url).getScheme();
        if (scheme == null || !ALLOWED_URL_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            return;
        }
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

        if (incomingEvent.getCode() != null && ("close-bot".equals(incomingEvent.getCode()) || "upload-image".equals(incomingEvent.getCode()) || "pwa-loaded".equals(incomingEvent.getCode()))) {
            incomingEvent.setInternal(true);
        }

        if (incomingEvent.isInternal()) {
            YMChat.getInstance().emitLocalEvent(incomingEvent);
        } else {
            YMChat.getInstance().emitEvent(incomingEvent);
        }
    }

    /**
     * Native text-to-speech fallback for widget JS running inside the WebView, since
     * android.webkit.WebView does not implement window.speechSynthesis.
     */
    @JavascriptInterface
    public void speakText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        final String utteranceId = "ymchat-tts-" + System.currentTimeMillis();
        parentActivity.runOnUiThread(() -> {
            if (isTtsReady) {
                tts.stop();
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
            }
        });
    }

    @JavascriptInterface
    public void stopSpeaking() {
        parentActivity.runOnUiThread(() -> {
            if (isTtsReady) {
                tts.stop();
            }
        });
    }

    public void destroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

}
