package com.yellowmessenger.ymchat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.yellowmessenger.ymchat.models.YMBotEventResponse;
import com.yellowmessenger.ymchat.models.ConfigService;


public class YMChat {
    private final String TAG = "YMChat";
    private Context myContext;
    private Intent _intent;
    private BotEventListener listener, localListener;
    private static YMChat botPluginInstance;
    public YMConfig config;
    private YMChat(){}
    public static YMChat getInstance(){
        if (botPluginInstance == null) {
            synchronized (YMChat.class) {
                if (botPluginInstance == null) {
                    botPluginInstance = new YMChat();
                }
            }
        }
        return  botPluginInstance;
    }
    public void setLocalListener(BotEventListener localListener){
        this.localListener = localListener;
    }
    public void onEventFromBot( BotEventListener listener){
       this.listener = listener;
    }
    public void startChatbot(Context context){
        try {
            if (!config.botId.isEmpty()) {
                ConfigService.getInstance().setConfigData(config);
                myContext = context;
                _intent = new Intent(myContext, BotWebView.class);
                _intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                myContext.startActivity(_intent);
            } else {
                throw new RuntimeException("botId is not configured. Please set botId before calling startChatbot()");
            }
        }catch (RuntimeException e){
            Log.e(TAG, "startChatbot: ", e );
        }
    }
    public void closeBot(){
        localListener.onSuccess(new YMBotEventResponse("close-bot", ""));
    }
    public void emitEvent(YMBotEventResponse event){
        if(event != null){
            listener.onSuccess(event);
            localListener.onSuccess(event);
        }
    }


}


