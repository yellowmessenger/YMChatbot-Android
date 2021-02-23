package com.yellowmessenger.ymchat;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.yellowmessenger.ymchat.models.BotEventsModel;
import com.yellowmessenger.ymchat.models.ConfigDataModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

public class YMBotPlugin {
    private Context myContext;
    private Intent _intent;
    private BotEventListener listener, localListener;
    private static YMBotPlugin botPluginInstance;
    private boolean isInitialized;

    private YMBotPlugin(){}


    public static YMBotPlugin getInstance(){
        if (botPluginInstance == null) {
            synchronized (YMBotPlugin.class) {
                if (botPluginInstance == null) {
                    botPluginInstance = new YMBotPlugin();
                }
            }
        }
        return  botPluginInstance;
    }
    public void setLocalListener(BotEventListener localListener){
        this.localListener = localListener;
    }

    public void init(String configData, BotEventListener listener){
        if(!isInitialized){
            isInitialized = true;
            if (configData != null && listener != null) {
                ConfigDataModel.getInstance().setConfig(new Gson().fromJson(configData, Map.class));
                this.listener = listener;
            } else {
                throw new RuntimeException("Mandatory arguments not present");
            }
        } else {
            throw new RuntimeException("Cannot initialize " + this.getClass().getName() + " multiple times");
        }
    }
    public void startChatBot(Context context){
        myContext = context;
        _intent = new Intent(myContext, BotWebView.class);
        myContext.startActivity(_intent);
    }

    public boolean setBotId(String botId){
       return  ConfigDataModel.getInstance().setConfigByKey("botID", botId);
    }


    public void setPayload(Map botPayload){
        ConfigDataModel.getInstance().emptyPayload();
        ConfigDataModel.getInstance().setPayload(botPayload);
    }

    public void setCustomData(Map botCustomPayload){
        ConfigDataModel.getInstance().emptyCustomdata();
        ConfigDataModel.getInstance().setCustomData(botCustomPayload);
    }

    public void emitEvent(BotEventsModel event){
        if(event != null){
            Log.v("WebView Event","From Bot: "+event.getCode());
            listener.onSuccess(event);
            localListener.onSuccess(event);
        }
        else
            listener.onFailure("An error occurred.");
    }

   public static String mapToString(HashMap<String, Object> map){
        String jsonString;
        Gson gson = new GsonBuilder().create();
        jsonString = gson.toJson(map);
        return jsonString;
   }

}

