package com.yellowmessenger.ymchat.models;


import java.util.HashMap;
import java.util.Map;

public class ConfigDataModel{
    private static ConfigDataModel configInstance;
    private final Map<String, String> config; // For configurations
    private final Map<String, String> payload; // For payload key-values
    private final Map<String, String> customData; // other data key-values

    private ConfigDataModel(){
        config = new HashMap<>();
        payload = new HashMap<>();
        customData = new HashMap<>();
    }

    public static  ConfigDataModel getInstance(){
        if (configInstance == null) {
            synchronized (ConfigDataModel.class) {
                if (configInstance == null) {
                    configInstance = new ConfigDataModel();
                }
            }
        }
        return  configInstance;
    }

    public boolean setConfig(Map configMap) {


        if (!configMap.isEmpty()) {
            config.putAll(configMap);
            return true;
        }
        return false;
    }
    public boolean setConfigByKey(String key, String value) {
        if (!key.isEmpty() && !value.isEmpty()) {
            config.put(key,value);
            return true;
        }
        return false;
    }

    public String getConfig(String key) {
        if(config.get(key) != null){
            return  config.get(key);
        }
        else return "";
    }

    public boolean setPayload(Map botPayload) {
        if (botPayload !=null) {
            payload.putAll(botPayload);
            return true;
        }
        return false;
    }

    public boolean emptyPayload() {
        if (payload !=null) {
            payload.clear();
            return true;
        }
        return false;
    }

    public boolean setCustomData(Map customDataPayload) {
        if (customDataPayload !=null) {
            customData.putAll(customDataPayload);
            return true;
        }
        return false;
    }

    public boolean emptyCustomdata() {
        if (customData !=null) {
            customData.clear();
            return true;
        }
        return false;
    }

    public String getPayloadByKey(String key) {
        return payload.get(key);
    }

    public String getCustomDataByKey(String key) {
        return customData.get(key);
    }

    public Map<String, String> getPayload() {
        return payload;
    }


}
