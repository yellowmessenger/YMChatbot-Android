package com.yellowmessenger.ymchatexample;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yellowmessenger.ymchat.BotEventListener;
import com.yellowmessenger.ymchat.YMBotPlugin;
import com.yellowmessenger.ymchat.models.BotEventsModel;

import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    // Dummy bot id. (Purrs a lot)
    String botId = "x1587041004122";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Initialize the bot
        YMBotPlugin pluginYM =  YMBotPlugin.getInstance();
        //Configuration data
        HashMap<String, Object> configurations = new HashMap<>();
        //Payload attributes
        HashMap<String, Object> payloadData = new HashMap<>();

        //Setting Config data.
        configurations.put("botID", botId); // Required.
        String configData = YMBotPlugin.mapToString(configurations);

        //Setting Payload Data
        payloadData.put("platform","Android-App");
        pluginYM.setPayload(payloadData);

        //Initialising chatbot
        pluginYM.init(configData, new BotEventListener() {
            @Override
            public void onSuccess(BotEventsModel botEvent) {
            }
            @Override
            public void onFailure(String error) {
            }
        });

        setContentView(R.layout.activity_main);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            //Starting the bot activity
            pluginYM.startChatBot(this);
        });
    }
}