package com.yellowmessenger.ymchatexample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yellowmessenger.ymchat.YMChat;
import com.yellowmessenger.ymchat.YMConfig;
import com.yellowmessenger.ymchat.models.YMBotEventResponse;
import com.yellowmessenger.ymchat.models.YellowCallback;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    // Dummy bot id. (Purrs a lot)
    String botId = "x1608615889375";
    String deviceToken = "11231232132132dadsasd231assd23124234ac1";
    String apiKey = "6ecc7380e0d6d058565f447a1150a6dc230dd2ecb4d8ed2f6e1bcc15eec27bb8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get YMChat instance
        YMChat ymChat = YMChat.getInstance();
        ymChat.config = new YMConfig(botId);
        ymChat.config.enableSpeech = true;
        //Payload attributes
        HashMap<String, Object> payloadData = new HashMap<>();
        //Setting Payload Data
        payloadData.put("some-key", "some-value");
        ymChat.config.payload = payloadData;
        ymChat.config.enableHistory = true;
        ymChat.config.deviceToken = deviceToken;

        // To Change the color of status bar, by default it will pick app theme
        ymChat.config.statusBarColor = R.color.colorPrimaryDark;
        // To Change the color of close button, default color is white
        ymChat.config.closeButtonColor = R.color.white;

        //setting event listener
        ymChat.onEventFromBot((YMBotEventResponse botEvent) -> {
            switch (botEvent.getCode()) {
                case "event-name":
                    break;
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

        Button button = findViewById(R.id.button);
        button.setOnClickListener(v -> {
            try{
                ymChat.unlinkNotificationToken(botId, apiKey, deviceToken, new YellowCallback() {
                    @Override
                    public void success() {
                        Toast.makeText(MainActivity.this,"Token unlinked",Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void failure(String message) {
                        Toast.makeText(MainActivity.this,message,Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e){
                //Catch and handle the exception
                e.printStackTrace();
            }

        }
        );



    }
}