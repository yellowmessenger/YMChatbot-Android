package com.yellowmessenger.ymchat;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.yellowmessenger.ymchat.models.ConfigService;
import com.yellowmessenger.ymchat.models.YMBotEventResponse;

import java.net.URLEncoder;


public class YMChat {
    private final String TAG = "YMChat";
    private BotEventListener listener, localListener;
    private static YMChat botPluginInstance;
    public YMConfig config;

    private YMChat() {
        this.listener = botEvent -> {
        };
    }

    public static YMChat getInstance() {
        if (botPluginInstance == null) {
            synchronized (YMChat.class) {
                if (botPluginInstance == null) {
                    botPluginInstance = new YMChat();
                }
            }
        }
        return botPluginInstance;
    }

    public void setLocalListener(BotEventListener localListener) {
        this.localListener = localListener;
    }

    public void onEventFromBot(BotEventListener listener) {
        this.listener = listener;
    }


    public void startChatbot(@NonNull Context context) throws Exception {
        try {
            if (context == null) {
                throw new Exception("Context passed is null. Please pass valid context");
            }

            if (config != null) {
                if(config.botId == null || config.botId.isEmpty()){
                    throw new Exception("botId is not configured. Please set botId before calling startChatbot()");
                }
                if(config.customBaseUrl == null || config.customBaseUrl.isEmpty()){
                    throw new Exception("customBaseUrl cannot be null or empty.");
                }
                if(config.payload != null){
                    try {
                        String payloadJSON = URLEncoder.encode(new Gson().toJson(config.payload), "UTF-8");
                    } catch (Exception e) {
                        throw new Exception("In payload map, value can be of primitive type or Map<String,String> ::\nException message :: " + e.getMessage());
                    }
                }
                ConfigService.getInstance().setConfigData(config);
                Intent _intent = new Intent(context, BotWebView.class);
                _intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(_intent);
            } else {
                throw new Exception("Please initialise config, it cannot be null.");
            }
        } catch (Exception e) {
            throw new Exception(("Exception in staring chat bot ::\nException message :: " + e.getMessage()));
        }
    }

    public void closeBot() {
        if (localListener != null)
            localListener.onSuccess(new YMBotEventResponse("close-bot", ""));
    }

    public void emitEvent(YMBotEventResponse event) {
        if (event != null) {
            if (listener != null)
                listener.onSuccess(event);

            if (localListener != null)
                localListener.onSuccess(event);
        }
    }


}


