package com.yellowmessenger.ymchatexample;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yellowmessenger.ymchat.YMChat;
import com.yellowmessenger.ymchat.YMConfig;

import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    // Dummy bot id. (Purrs a lot)
    String botId = "x1587041004122";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get YMChat instance
        YMChat ymChat =  YMChat.getInstance();
        ymChat.config = new YMConfig(botId);
        //Payload attributes
        HashMap<String, Object> payloadData = new HashMap<>();
        //Setting Payload Data
        payloadData.put("some-key","some-value");
        ymChat.config.payload = payloadData;

        //setting event listener
        ymChat.onEventFromBot(botEvent -> {
            switch (botEvent.getCode()){
                case "event-name": break;
            }
        });

        setContentView(R.layout.activity_main);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            //Starting the bot activity
            ymChat.startChatbot(this);
        });
    }
}