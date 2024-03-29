package com.yellowmessenger.ymchat;

import androidx.annotation.NonNull;

import com.yellowmessenger.ymchat.models.YMEnableSpeechConfig;
import com.yellowmessenger.ymchat.models.YMTheme;

import java.util.HashMap;

public class YMConfig {
    public String botId;
    public boolean enableSpeech = false;
    public boolean showConsoleLogs = false;
    public String ymAuthenticationToken = "";
    public String deviceToken = "";
    public String customBaseUrl = "https://app.yellowmessenger.com";
    public int statusBarColor = -1;
    public String statusBarColorFromHex = "";
    public boolean hideCameraForUpload = false;
    public boolean showCloseButton = true;
    public int closeButtonColor = -1;
    public String closeButtonColorFromHex = "";
    public int version = 1;
    public String customLoaderUrl = "file:///android_asset/yellowLoader.gif";
    public HashMap<String, Object> payload = new HashMap<>(); // For payload key-values
    public HashMap<String, String> customData = new HashMap<>(); // other data key-values
    public boolean disableActionsOnLoad = false;
    public boolean useLiteVersion = false;
    public boolean alwaysReload = false;
    public boolean useSecureYmAuth = false;
    public YMEnableSpeechConfig enableSpeechConfig = new YMEnableSpeechConfig();
    public YMTheme theme = new YMTheme();

    public YMConfig(@NonNull String botId) {
        this.botId = botId;
    }
}
