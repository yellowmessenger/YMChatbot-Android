package com.yellowmessenger.ymchat;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.yellowmessenger.ymchat.models.ConfigService;
import com.yellowmessenger.ymchat.models.YMBotEventResponse;


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

    public void startChatbot(Context context) {
        try {
            if (context == null) {
                throw new RuntimeException("Context passed is null. Please pass valid context");
            }

            if (config != null && config.botId != null && !config.botId.isEmpty() ) {
                ConfigService.getInstance().setConfigData(config);
                Intent _intent = new Intent(context, BotWebView.class);
                _intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(_intent);
            } else {
                throw new RuntimeException("botId is not configured. Please set botId before calling startChatbot()");
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "startChatbot: ", e);
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


