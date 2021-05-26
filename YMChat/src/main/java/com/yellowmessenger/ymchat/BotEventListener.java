package com.yellowmessenger.ymchat;

import com.yellowmessenger.ymchat.models.YMBotEventResponse;

public interface BotEventListener {
    void onSuccess(YMBotEventResponse botEvent);
}