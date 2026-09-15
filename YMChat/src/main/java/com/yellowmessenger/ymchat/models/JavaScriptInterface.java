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
     * Emoji code points, as inclusive ranges. java.util.regex has no
     * Extended_Pictographic property, so the ranges are spelled out instead.
     *
     * Deliberately conservative: (c) U+00A9, (R) U+00AE and (TM) U+2122 are emoji-capable
     * but read as ordinary text far more often, so they are left alone.
     */
    private static final int[][] EMOJI_RANGES = {
            {0x1F000, 0x1FAFF},  // mahjong, cards, enclosed alphanumerics, pictographs, emoticons,
                                 // transport, supplemental symbols, extended-A/B, skin-tone modifiers
            {0x2600, 0x27BF},    // miscellaneous symbols and dingbats
            {0x231A, 0x231B},    // watch, hourglass
            {0x23E9, 0x23F3},    // media controls, timers
            {0x23F8, 0x23FA},    // pause, stop, record
            {0x2B05, 0x2B07},    // heavy arrows
            {0x2B1B, 0x2B1C},    // large squares
            {0x2B50, 0x2B50},    // star
            {0x2B55, 0x2B55},    // heavy circle
    };

    /**
     * Invisible sequence glue — joiners, variation selectors and the tag characters used
     * by subdivision flags. These are dropped outright rather than replaced with a space:
     * they occupy no width, and turning one into a space would separate a keycap from the
     * digit it encloses.
     */
    private static final int[][] ZERO_WIDTH_RANGES = {
            {0x200D, 0x200D},    // zero-width joiner
            {0xFE00, 0xFE0F},    // variation selectors
            {0xE0020, 0xE007F},  // tag characters, used by subdivision flags
    };

    /** U+20E3 COMBINING ENCLOSING KEYCAP — turns a preceding digit, # or * into an emoji. */
    private static final int KEYCAP = 0x20E3;

    private static boolean inRanges(int codePoint, int[][] ranges) {
        for (int[] range : ranges) {
            if (codePoint >= range[0] && codePoint <= range[1]) {
                return true;
            }
        }
        return false;
    }

    private static boolean isKeycapBase(char c) {
        return (c >= '0' && c <= '9') || c == '#' || c == '*';
    }

    /**
     * Removes emoji so the engine doesn't pronounce them by name — otherwise a bot reply
     * is read as "your payment is confirmed smiling face with smiling eyes" (ISS-16408).
     *
     * The widget strips emoji too, and more precisely, using Unicode property escapes.
     * This is the backstop for callers that don't: older widget builds, and anything else
     * on the page that reaches the bridge. Stripping twice is harmless.
     *
     * Visible for testing.
     */
    static String stripEmoji(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);

            if (inRanges(codePoint, ZERO_WIDTH_RANGES)) {
                // Drop with no space, so a keycap still sees its base character.
                continue;
            }
            if (codePoint == KEYCAP) {
                int last = out.length() - 1;
                if (last >= 0 && isKeycapBase(out.charAt(last))) {
                    out.deleteCharAt(last);
                }
                out.append(' ');
            } else if (inRanges(codePoint, EMOJI_RANGES)) {
                // A space rather than nothing, so "hello<emoji>world" stays two words.
                out.append(' ');
            } else {
                out.appendCodePoint(codePoint);
            }
        }
        // Tidy the gaps the removals left. Newlines survive, since voices pause on them.
        return out.toString()
                .replaceAll("[^\\S\\n]{2,}", " ")
                .replaceAll("[^\\S\\n]+(?=[.,!?;:])", "")
                .replaceAll("(?m)[^\\S\\n]+$", "")
                .trim();
    }

    /**
     * Native text-to-speech fallback for widget JS running inside the WebView, since
     * android.webkit.WebView does not implement window.speechSynthesis.
     */
    @JavascriptInterface
    public void speakText(String text) {
        if (text == null || !isTtsReady) {
            return;
        }
        final String spoken = stripEmoji(text);
        if (spoken.isEmpty()) {
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
            if (tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, utteranceId) != TextToSpeech.SUCCESS) {
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
