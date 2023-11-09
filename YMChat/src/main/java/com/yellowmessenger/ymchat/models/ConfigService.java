package com.yellowmessenger.ymchat.models;


import android.net.Uri;

import com.google.gson.Gson;
import com.yellowmessenger.ymchat.YMConfig;

import java.util.HashMap;
import java.util.Map;

public class ConfigService {
    private static final String TAG = ConfigService.class.getSimpleName();
    private static ConfigService configInstance;
    private YMConfig config; // For configurations
    private Map<String, Object> payload; // For payload key-values
    private Map<String, String> customData; // other data key-values

    private ConfigService() {
        config = new YMConfig("");
        payload = new HashMap<>();
        customData = new HashMap<>();
    }

    public static ConfigService getInstance() {
        if (configInstance == null) {
            synchronized (ConfigService.class) {
                if (configInstance == null) {
                    configInstance = new ConfigService();
                }
            }
        }
        return configInstance;
    }

    public boolean setConfigData(YMConfig config) {
        if (config != null) {
            this.config = config;
            this.payload = config.payload;
            this.customData = config.customData != null ? config.customData : new HashMap<>();
            return true;
        }
        return false;
    }

    public YMConfig getConfig() {
        return config;
    }

    public String getUrl(String baseUrl) {
        Uri builtUri = Uri.parse(baseUrl)
                .buildUpon()
                .appendQueryParameter("botId", config.botId)
                .appendQueryParameter("ym.payload", getPayload())
                .appendQueryParameter("ymAuthenticationToken", config.ymAuthenticationToken == null ? "" : config.ymAuthenticationToken)
                .appendQueryParameter("useSecureYmAuth", String.valueOf(config.useSecureYmAuth))
                .appendQueryParameter("deviceToken", config.deviceToken == null ? "" : config.deviceToken)
                .appendQueryParameter("customBaseUrl", config.customBaseUrl)
                .appendQueryParameter("version", Integer.toString(config.version))
                .appendQueryParameter("customLoaderUrl", config.customLoaderUrl)
                .appendQueryParameter("disableActionsOnLoad", String.valueOf(config.disableActionsOnLoad))
                .appendQueryParameter("ym.theme",config.theme == null ? "" : getTheme())
                .build();

        return builtUri.toString();
    }

    private String getPayload() {
        payload = config.payload != null ? config.payload : new HashMap<>();
        payload.put("Platform", "Android-App");
        return new Gson().toJson(payload);
    }

    private String getTheme() {
        return new Gson().toJson(config.theme);
    }

    public String getCustomDataByKey(String key) {
        return customData.get(key);
    }


}
