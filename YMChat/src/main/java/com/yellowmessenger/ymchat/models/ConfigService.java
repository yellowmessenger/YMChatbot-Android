package com.yellowmessenger.ymchat.models;


import android.webkit.WebView;

import com.google.gson.Gson;
import com.yellowmessenger.ymchat.R;
import com.yellowmessenger.ymchat.YMConfig;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class ConfigService {
    private static ConfigService configInstance;
    private YMConfig config; // For configurations
    private HashMap<String, Object> payload; // For payload key-values
    private Map<String, String> customData; // other data key-values

    private ConfigService(){
        config = new YMConfig("");
        payload = new HashMap<>();
        customData = new HashMap<>();
    }

    public static ConfigService getInstance(){
        if (configInstance == null) {
            synchronized (ConfigService.class) {
                if (configInstance == null) {
                    configInstance = new ConfigService();
                }
            }
        }
        return  configInstance;
    }

    public boolean setConfigData(YMConfig config) {
        if (config !=null) {
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

    public String getBotURLParams(){
        String botId = config.botId;

        Map payload =  config.payload;
        payload.put("platform","Android-App");
        String payloadJSON = URLEncoder.encode(new Gson().toJson(payload));
        boolean enableHistory = config.enableHistory;
        String ymAuthenticationToken = config.ymAuthenticationToken;

        final String botURLParams = "?botId=" + botId + "&enableHistory=" + enableHistory +"&ymAuthenticationToken="+ymAuthenticationToken + "&ym.payload=" + payloadJSON;
        return botURLParams;

    }

//    public boolean setPayload(Map botPayload) {
//        if (botPayload !=null) {
//            payload.putAll(botPayload);
//            return true;
//        }
//        return false;
//    }
//
//    public boolean emptyPayload() {
//        if (payload !=null) {
//            payload.clear();
//            return true;
//        }
//        return false;
//    }

//    public boolean setCustomData(Map customDataPayload) {
//        if (customDataPayload !=null) {
//            customData.putAll(customDataPayload);
//            return true;
//        }
//        return false;
//    }


    public String getCustomDataByKey(String key) {
        return customData.get(key);
    }


}
