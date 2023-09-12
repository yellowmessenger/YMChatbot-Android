package com.yellowmessenger.ymchatexample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.yellowmessenger.ymchat.YMChat;
import com.yellowmessenger.ymchat.YMConfig;
import com.yellowmessenger.ymchat.models.YMBotEventResponse;
import com.yellowmessenger.ymchat.models.YellowCallback;
import com.yellowmessenger.ymchat.models.YellowDataCallback;
import com.yellowmessenger.ymchat.models.YellowUnreadMessageResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    // Dummy bot id. (Purrs a lot)
    String botId = "x1657623696077";
    String deviceToken = "yourdevicetoken";
    String apiKey = "your-api-key";
    String ymAuthenticationToken = "secure and unique auth token";
    FrameLayout frame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        WebView.setWebContentsDebuggingEnabled(true);

        Button startBtn = findViewById(R.id.startbtn);
        Button button = findViewById(R.id.button);
        Button button2 = findViewById(R.id.showFragment);
        Button button3 = findViewById(R.id.preloadFragment);
        Button registerDevice = findViewById(R.id.registerDevice);
        Button unreadMessages = findViewById(R.id.unreadMessages);
        //Get YMChat instance
        YMChat ymChat = YMChat.getInstance();
        ymChat.config = new YMConfig(botId);

        // ymChat.config.ymAuthenticationToken = ymAuthenticationToken;

        // Set this flag to hide input bar while bot is loading the history
        //ymChat.config.disableActionsOnLoad = true;

        //To enable speach to text
        // ymChat.config.enableSpeech = true;

        //Payload attributes
        HashMap<String, Object> payloadData = new HashMap<>();
        //Setting Payload Data
        // payloadData.put("mobile", "919588863784");
        ymChat.config.payload = payloadData;

        //If you want to use lite version please add ymChat.config.useLiteVersion = true
        // In case of light version, custom loader url is not supported
        // ymChat.config.useLiteVersion = true;

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

        // Set custom loader url , it should be a valid, light weight and image url
        // This is an optional parameter
        ymChat.config.customLoaderUrl = "https://cdn.yellowmessenger.com/PQuTgjPG7NxY1611996274269.png"; //"file:///android_asset/yellowLogo.svg";

        // Hide input bar and disallow action while bot is loading
        ymChat.config.disableActionsOnLoad = true;


        //Set custom base url like follows in case of On-Prem environment and multi-region
        //ymChat.config.customBaseUrl = "https://rx.cloud.yellow.ai";

        //setting event listener
        ymChat.onEventFromBot((YMBotEventResponse botEvent) -> {
            Toast.makeText(this, botEvent.getCode(), Toast.LENGTH_SHORT).show();
            switch (botEvent.getCode()) {
                case "event-name":
                    break;
                case "ym-revalidate-token": {
                    /** This event will be received when client is using secure ymAuth approach
                     This feature is available for only cloud bots On-prem bot does not support this
                     Set ymChat.config.useSecureYmAuth = true in config
                     */

                    // You will receive 'refreshSession' boolean value in botEvent.getData()
                    boolean refreshSession = false;
                    try {
                        JSONObject object = new JSONObject(botEvent.getData());
                        refreshSession = object.getBoolean("refreshSession");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    /** make api call to fetch new encrypted token and then call
                     * YMChat.getInstance().revalidateToken("your-new-token", refreshSession);
                     * */
                    break;
                }
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


        registerDevice.setOnClickListener(view -> registerDevice());
        unreadMessages.setOnClickListener(view -> getUnreadMessagesCount());

        button.setOnClickListener(v -> unlinkDevice());

        button2.setOnClickListener(v -> startActivity(new Intent(this, BotViewActivity.class)));
        button3.setOnClickListener(v -> startActivity(new Intent(this, PreloadChatbotActivity.class)));
    }

    private void unlinkDevice() {
        try {
            YMChat ymChat = YMChat.getInstance();
            YMConfig ymConfig = new YMConfig(botId);
            // Set the FCM token as device token, this is required so that it can be removed and customer stop receiving the notification
            ymConfig.deviceToken = deviceToken;
            // Set custom base url in case your bot does not belong to india region and yellow cloud
            // Example- If your bot is in `r5` region custom base url would be `https://r5.cloud.yellow.ai
            // ymConfig.customBaseUrl = "https://r5.cloud.yellow.ai";
            ymChat.unlinkDeviceToken(apiKey, ymConfig, new YellowCallback() {
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


    private void registerDevice() {
        try {
            YMChat ymChat = YMChat.getInstance();
            YMConfig ymConfig = new YMConfig(botId);
            ymConfig.deviceToken = deviceToken;
            ymConfig.ymAuthenticationToken = ymAuthenticationToken;
            // Set custom base url in case your bot does not belong to india region and yellow cloud
            // Example- If your bot is in `r5` region custom base url would be `https://r5.cloud.yellow.ai
            // ymConfig.customBaseUrl = "https://r5.cloud.yellow.ai";
            ymChat.registerDevice(apiKey, ymConfig, new YellowCallback() {
                @Override
                public void success() {
                    Toast.makeText(MainActivity.this, "Device Registered", Toast.LENGTH_SHORT).show();
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

    private void getUnreadMessagesCount() {
        try {
            YMChat ymChat = YMChat.getInstance();
            YMConfig ymConfig = new YMConfig(botId);
            ymConfig.ymAuthenticationToken = ymAuthenticationToken;
            // Set custom base url in case your bot does not belong to india region and yellow cloud
            // Example- If your bot is in `r5` region custom base url would be `https://r5.cloud.yellow.ai
            // ymConfig.customBaseUrl = "https://r5.cloud.yellow.ai";
            ymChat.getUnreadMessagesCount(ymConfig, new YellowDataCallback() {
                @Override
                public <T> void success(T data) {
                    YellowUnreadMessageResponse response = (YellowUnreadMessageResponse) data;
                    Toast.makeText(MainActivity.this, "Unread messages - " + response.getUnreadCount(), Toast.LENGTH_SHORT).show();
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