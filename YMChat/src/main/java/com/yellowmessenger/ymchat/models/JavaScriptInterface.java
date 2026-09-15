package com.yellowmessenger.ymchat.models;

import android.app.Activity;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
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
    private volatile boolean isSpeaking = false;
    /** Utterance the widget currently believes is playing; guards against late callbacks from flushed ones. */
    private volatile String currentUtteranceId = null;

    public JavaScriptInterface(Activity _activity, WebView _webView) {
        parentActivity = _activity;
        mWebView = _webView;

        tts = new TextToSpeech(parentActivity, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        if (isCurrent(utteranceId)) {
                            isSpeaking = true;
                        }
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        clearIfCurrent(utteranceId);
                    }

                    @Override
                    @Deprecated
                    public void onError(String utteranceId) {
                        clearIfCurrent(utteranceId);
                    }

                    @Override
                    public void onError(String utteranceId, int errorCode) {
                        clearIfCurrent(utteranceId);
                    }

                    @Override
                    public void onStop(String utteranceId, boolean interrupted) {
                        clearIfCurrent(utteranceId);
                    }
                });
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

    private boolean isCurrent(String utteranceId) {
        return utteranceId != null && utteranceId.equals(currentUtteranceId);
    }

    private void clearIfCurrent(String utteranceId) {
        if (isCurrent(utteranceId)) {
            currentUtteranceId = null;
            isSpeaking = false;
        }
    }

    /**
     * Native text-to-speech fallback for widget JS running inside the WebView, since
     * android.webkit.WebView does not implement window.speechSynthesis.
     */
    @JavascriptInterface
    public void speakText(String text) {
        if (text == null || text.trim().isEmpty() || !isTtsReady) {
            return;
        }
        // nanoTime rather than currentTimeMillis so two taps in the same millisecond
        // still get distinct ids for the isCurrent() guard.
        final String utteranceId = "ymchat-tts-" + System.nanoTime();
        // Flip the state before leaving the calling thread: the widget polls isSpeaking()
        // immediately after this returns, and the engine takes a moment to report onStart.
        currentUtteranceId = utteranceId;
        isSpeaking = true;
        parentActivity.runOnUiThread(() -> {
            if (tts == null || !isTtsReady) {
                clearIfCurrent(utteranceId);
                return;
            }
            tts.stop();
            if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) != TextToSpeech.SUCCESS) {
                clearIfCurrent(utteranceId);
            }
        });
    }

    @JavascriptInterface
    public void stopSpeaking() {
        currentUtteranceId = null;
        isSpeaking = false;
        parentActivity.runOnUiThread(() -> {
            if (tts != null && isTtsReady) {
                tts.stop();
            }
        });
    }

    /**
     * Lets the widget keep its "Read aloud" button in sync with playback, the way
     * SpeechSynthesisUtterance's onstart/onend do on the web. The bridge cannot push
     * completion events instead: @JavascriptInterface objects are injected into every
     * frame, but WebView#evaluateJavascript only runs in the main frame, so native code
     * has no way to reach the widget's cross-origin iframe. The widget polls this.
     */
    @JavascriptInterface
    public boolean isSpeaking() {
        return isSpeaking;
    }

    public void destroy() {
        currentUtteranceId = null;
        isSpeaking = false;
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

}
