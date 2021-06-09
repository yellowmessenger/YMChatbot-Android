package com.yellowmessenger.ymchatexample;

import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yellowmessenger.ymchat.YMChat;
import com.yellowmessenger.ymchat.YMConfig;
import com.yellowmessenger.ymchat.models.YMBotEventResponse;

import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    // Dummy bot id. (Purrs a lot)
    String botId = "x1608615889375";//"x1587041004122"; //x1612170282642

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get YMChat instance
        YMChat ymChat =  YMChat.getInstance();
        ymChat.config = new YMConfig(botId);
        ymChat.config.enableSpeech = true;
        //Payload attributes
        HashMap<String, Object> payloadData = new HashMap<>();
        //Setting Payload Data
        payloadData.put("some-key","some-value");
        ymChat.config.payload = payloadData;
        ymChat.config.enableHistory = true;

        //setting event listener
        ymChat.onEventFromBot((YMBotEventResponse botEvent) -> {
            switch (botEvent.getCode()){
                case "event-name": break;
            }
        });
        ymChat.onBotClose(() -> {
            Log.d("Example App", "Bot Was closed");
        });

        setContentView(R.layout.activity_main);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            //Starting the bot activity
            try {
                ymChat.startChatbot(this);
            } catch (Exception e) {
                //Catch and handle the exception
                e.printStackTrace();
            }
        });
    }
}