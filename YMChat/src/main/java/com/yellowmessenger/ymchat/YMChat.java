package com.yellowmessenger.ymchat;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.yellowmessenger.ymchat.models.YMBotEventResponse;
import com.yellowmessenger.ymchat.models.ConfigService;

public class YMChat {
    private Context myContext;
    private Intent _intent;
    private BotEventListener listener, localListener;
    private static YMChat botPluginInstance;
    private boolean isInitialized;
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
        ConfigService.getInstance().setConfigData(config); // convert to map
        myContext = context;
        _intent = new Intent(myContext, BotWebView.class);
        myContext.startActivity(_intent);
    }

    public void emitEvent(YMBotEventResponse event){
        if(event != null){
            Log.v("WebView Event","From Bot: "+event.getCode());
            listener.onSuccess(event);
            localListener.onSuccess(event);
        }
//        else
//            listener.onFailure("An error occurred.");
    }


}


