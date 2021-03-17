package com.yellowmessenger.ymchat.models;


import android.util.Log;

import com.google.gson.Gson;
import com.yellowmessenger.ymchat.YMConfig;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class ConfigService {
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
            this.customData = config.customData;
            return true;
        }
        return false;
    }

    public YMConfig getConfig() {
        return config;
    }

    public String getBotURLParams() {
        String botId = config.botId;
        payload = config.payload;
        payload.put("platform", "Android-App");
        String payloadJSON = null;
        try {
            payloadJSON = URLEncoder.encode(new Gson().toJson(payload),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e("ConfigService-",e.getMessage());
        }
        String ymAuthenticationToken = "";
        if (config.ymAuthenticationToken != null)
            ymAuthenticationToken = config.ymAuthenticationToken;

        return "?botId=" + botId + "&enableHistory=" + config.enableHistory + "&ymAuthenticationToken=" + ymAuthenticationToken + "&deviceToken=" + config.deviceToken + "&ym.payload=" + payloadJSON;

    }

    public String getCustomDataByKey(String key) {
        return customData.get(key);
    }


}
