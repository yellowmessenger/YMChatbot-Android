package com.yellowmessenger.ymchatexample;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.yellowmessenger.ymchat.YMChat;
import com.yellowmessenger.ymchat.YMConfig;
import com.yellowmessenger.ymchat.models.YMBotEventResponse;
import com.yellowmessenger.ymchat.models.YellowCallback;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    // Dummy bot id. (Purrs a lot)J
    String botId = "x1645602443989";
    String deviceToken = "your device token";
    String apiKey = "your api key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get YMChat instance
        YMChat ymChat = YMChat.getInstance();
        ymChat.config = new YMConfig(botId);

        //To enable speach to text
        // ymChat.config.enableSpeech = true;

        //Payload attributes
        HashMap<String, Object> payloadData = new HashMap<>();
        //Setting Payload Data
        payloadData.put("some-key", "some-value");
        ymChat.config.payload = payloadData;

        // Choose version(1 or 2), default is 1
        ymChat.config.version = 2;

        //To enable notifications
        ymChat.config.deviceToken = deviceToken;

        // To Change the color of status bar, by default it will pick app theme
        ymChat.config.statusBarColor = R.color.colorPrimaryDark;
        // To Change the color of close button, default color is white
        ymChat.config.closeButtonColor = R.color.white;

        /* Note: if color is set from both setStatusBarColor and statusBarColorFromHex,
         * statusBarColorFromHex will take priority
         * */
        // To set statusBarColor from hexadecimal color code
        ymChat.config.statusBarColorFromHex = "#49c656";

        /* Note: if color is set from both closeButtonColor and closeButtonColorHex,
         * closeButtonColorHex will take priority
         * */
        // To set closeButtonColor from hexadecimal color code
        ymChat.config.closeButtonColorFromHex = "#b72a2a";

        // Set custom loader url , it should be a valid, light weight and public image url
        // This is an optional parameter
        ymChat.config.customLoaderUrl = "https://yellow.ai/images/Logo.svg";

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

        Button startBtn = findViewById(R.id.startbtn);
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

        Button button = findViewById(R.id.button);
        button.setOnClickListener(v -> {
                    unlinkDevice();
                }
        );
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