package com.yellowmessenger.ymchat;

import com.yellowmessenger.ymchat.models.BotEventsModel;

public interface BotEventListener {
    void onSuccess(BotEventsModel botEvent);
    void onFailure(String error);
}