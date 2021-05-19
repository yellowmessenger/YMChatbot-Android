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
            this.customData = config.customData != null ? config.customData : new HashMap<>();
            return true;
        }
        return false;
    }

    public YMConfig getConfig() {
        return config;
    }

    public String getBotURLParams() throws RuntimeException {
        if (config.customBaseUrl == null || config.customBaseUrl.isEmpty())
            throw new RuntimeException("customBaseUrl cannot be null or empty.");
        String botId = config.botId;
        payload = config.payload != null ? config.payload : new HashMap<>();
        payload.put("Platform", "Android-App");
        String payloadJSON = null;
        try {
            payloadJSON = URLEncoder.encode(new Gson().toJson(payload), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e("ConfigService-", e.getMessage());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("?botId=");
        sb.append(botId);
        sb.append("&enableHistory=");
        sb.append(config.enableHistory);
        sb.append("&ymAuthenticationToken=");
        sb.append(config.ymAuthenticationToken != null ? config.ymAuthenticationToken : "");
        sb.append("&deviceToken=");
        sb.append(config.deviceToken != null ? config.deviceToken : "");
        sb.append("&customBaseUrl=");
        sb.append(config.customBaseUrl);


        sb.append("&ym.payload=");
        sb.append(payloadJSON);


        return sb.toString();

    }

    public String getCustomDataByKey(String key) {
        return customData.get(key);
    }


}
