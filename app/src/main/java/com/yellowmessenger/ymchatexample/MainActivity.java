package com.yellowmessenger.ymchatexample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.yellowmessenger.ymchat.YMChat;
import com.yellowmessenger.ymchat.YMConfig;
import com.yellowmessenger.ymchat.models.YMBotEventResponse;
import com.yellowmessenger.ymchat.models.YellowCallback;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    // Dummy bot id. (Purrs a lot)J
    String botId = "x1652333933102";
    String deviceToken = "your device token";
    String apiKey = "your api key";
    FrameLayout frame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Button startBtn = findViewById(R.id.startbtn);
        Button button = findViewById(R.id.button);
        Button button2 = findViewById(R.id.showFragment);
        Button button3 = findViewById(R.id.preloadFragment);
        //Get YMChat instance
        YMChat ymChat = YMChat.getInstance();
        ymChat.config = new YMConfig(botId);

        ymChat.config.disableActionsOnLoad = true;

        //To enable speach to text
       // ymChat.config.enableSpeech = true;

        //Payload attributes
        HashMap<String, Object> payloadData = new HashMap<>();
        //Setting Payload Data
       // payloadData.put("mobile", "919588863784");
        ymChat.config.payload = payloadData;

        ymChat.config.useLiteVersion = true;

        // Choose version(1 or 2), default is 1
        ymChat.config.version = 2;

        //To enable notifications
        ymChat.config.deviceToken = deviceToken;

        // To Change the color of status bar, by default it will pick app theme
        ymChat.config.statusBarColor = R.color.teal_200;
        // To Change the color of close button, default color is white
        ymChat.config.closeButtonColor = R.color.white;

        /* Note: if color is set from both setStatusBarColor and statusBarColorFromHex,
         * statusBarColorFromHex will take priority
         * */
        // To set statusBarColor from hexadecimal color code
        ymChat.config.statusBarColorFromHex = "#FF03DAC5";

        /* Note: if color is set from both closeButtonColor and closeButtonColorHex,
         * closeButtonColorHex will take priority
         * */
        // To set closeButtonColor from hexadecimal color code
        ymChat.config.closeButtonColorFromHex = "#b72a2a";

        // Set custom loader url , it should be a valid, light weight and public image url
        // This is an optional parameter
         ymChat.config.customLoaderUrl = "https://yellow.ai/images/Logo.svg";

        // Hide input bar and disallow action while bot is loading
        ymChat.config.disableActionsOnLoad = true;


        //Set custom base url like follows in case of On-Prem environment and multi-region
         ymChat.config.customBaseUrl = "https://staging.yellowmessenger.com";

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

        startBtn.setOnClickListener(view -> {
            //Starting the bot activity
            try {
                ymChat.startChatbot(this);
            } catch (Exception e) {
                //Catch and handle the exception
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });

        button.setOnClickListener(v -> unlinkDevice());

        button2.setOnClickListener(v -> startActivity(new Intent(this, BotViewActivity.class)));
        button3.setOnClickListener(v -> startActivity(new Intent(this, PreloadChatbotActivity.class)));
    }

    private void unlinkDevice() {
        try {
            YMChat ymChat = YMChat.getInstance();
            ymChat.unlinkDeviceToken(botId, apiKey, deviceToken, new YellowCallback() {
                @Override
                public void success() {
                    Toast.makeText(MainActivity.this, "Token unlinked", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void failure(String message) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            //Catch and handle the exception
            e.printStackTrace();
        }
    }

}