package com.yellowmessenger.ymchat;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.yellowmessenger.ymchat.models.ConfigService;
import com.yellowmessenger.ymchat.models.YMBotEventResponse;

import java.net.URLEncoder;


public class YMChat {
    private final String TAG = "YMChat";
    private BotEventListener listener, localListener;
    private BotCloseEventListener botCloseEventListener;
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

    public void onBotClose(BotCloseEventListener listener) {
        this.botCloseEventListener = listener;
    }


    public void startChatbot(@NonNull Context context) throws Exception {
        try {
            if (validate(context)) {
                ConfigService.getInstance().setConfigData(config);
                Intent _intent = new Intent(context, BotWebView.class);
                _intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(_intent);
            }
        } catch (Exception e) {
            throw new Exception(("Exception in staring chat bot ::\nException message :: " + e.getMessage()));
        }
    }

    private boolean validate(Context context) throws Exception {
        if (context == null) {
            throw new Exception("Context passed is null. Please pass valid context");
        }

        if (config == null) {
            throw new Exception("Please initialise config, it cannot be null.");
        }

        if (config.botId == null || config.botId.isEmpty()) {
            throw new Exception("botId is not configured. Please set botId before calling startChatbot()");
        }
        if (config.customBaseUrl == null || config.customBaseUrl.isEmpty()) {
            throw new Exception("customBaseUrl cannot be null or empty.");
        }
        if (config.payload != null) {
            try {
                URLEncoder.encode(new Gson().toJson(config.payload), "UTF-8");
            } catch (Exception e) {
                throw new Exception("In payload map, value can be of primitive type or json convertible value ::\nException message :: " + e.getMessage());
            }
        }
        return true;
    }

    public void closeBot() {
        if (localListener != null)
            localListener.onSuccess(new YMBotEventResponse("close-bot", ""));
    }

    public void emitEvent(YMBotEventResponse event) {
        if (event != null) {
            if (botCloseEventListener != null && event.getCode() != null && event.getCode().equals("bot-closed")) {
                botCloseEventListener.onClosed();
            } else {
                if (listener != null)
                    listener.onSuccess(event);

                if (localListener != null)
                    localListener.onSuccess(event);
            }

        }
    }


}


