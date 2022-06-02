package com.yellowmessenger.ymchatexample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yellowmessenger.ymchat.YMChat;
import com.yellowmessenger.ymchat.YMConfig;
import com.yellowmessenger.ymchat.models.YMBotEventResponse;

import java.util.HashMap;

public class TestActivity extends AppCompatActivity {

    private FloatingActionButton micButton, micButton2;
    private boolean isVisible = false;
    private boolean isVisible2 = false;
    private FrameLayout container, container2;

    String botId = "x1645602443989";
    String deviceToken = "your device token";
    String apiKey = "your api key";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        container = findViewById(R.id.containertest);
        container2 = findViewById(R.id.container2);
        micButton = findViewById(R.id.floatButton);
        micButton2 = findViewById(R.id.floatButton2);

        loadChatBot1();
        loadChatBot2();
        micButton.setOnClickListener(v -> {
            if (isVisible) {
                isVisible = false;
                container.setVisibility(View.GONE);
            } else {
                container.setVisibility(View.VISIBLE);
                isVisible = true;

                container2.setVisibility(View.GONE);
                isVisible2 = false;
            }
        });

        micButton2.setOnClickListener(v -> {
            if (isVisible2) {
                isVisible2 = false;
                container2.setVisibility(View.GONE);
            } else {
                container2.setVisibility(View.VISIBLE);
                isVisible2 = true;

                container.setVisibility(View.GONE);
                isVisible = false;
            }
        });
    }

    private YMConfig getConfig1(String botId) {
        YMConfig config = new YMConfig(botId);
        //To enable speach to text
        //ymChat.config.enableSpeech = true;

        //Payload attributes
        HashMap<String, Object> payloadData = new HashMap<>();
        //Setting Payload Data
       /* payloadData.put("category", "4402713315609");
        payloadData.put("email", "ellowtest@g.com");
        payloadData.put("name", "Dhhd");
        payloadData.put("section", "4402713320217");
        payloadData.put("selectedArticle", "4402713344281");
        payloadData.put("ticketSubject", "rewards | 4402713344281");
        payloadData.put("userState", "application");*/

        config.payload = payloadData;

        // Choose version(1 or 2), default is 1
        config.version = 2;

        //To enable notifications
        config.deviceToken = deviceToken;

        // To Change the color of status bar, by default it will pick app theme
        config.statusBarColor = R.color.colorPrimaryDark;
        // To Change the color of close button, default color is white
        config.closeButtonColor = R.color.white;

        /* Note: if color is set from both setStatusBarColor and statusBarColorFromHex,
         * statusBarColorFromHex will take priority
         * */
        // To set statusBarColor from hexadecimal color code
        config.statusBarColorFromHex = "#49c656";

        /* Note: if color is set from both closeButtonColor and closeButtonColorHex,
         * closeButtonColorHex will take priority
         * */
        // To set closeButtonColor from hexadecimal color code
        config.closeButtonColorFromHex = "#b72a2a";

        // Set custom loader url , it should be a valid, light weight and public image url
        // This is an optional parameter
        config.customLoaderUrl = "https://yellow.ai/images/Logo.svg";

        config.ymAuthenticationToken = "1234";

        return config;

    }

    private void loadChatBot1() {
        YMChat ymChat = YMChat.getInstance("1234");
        ymChat.config = getConfig1(botId); // new YMConfig(botId);

        //setting event listener
        ymChat.onEventFromBot((YMBotEventResponse botEvent) -> {
            String ev =  "Event from bot 1"+ botEvent.getCode() + "Bot data" + botEvent.getData();
            Toast.makeText(TestActivity.this,    ev, Toast.LENGTH_SHORT).show();
            switch (botEvent.getCode()) {
                case "event-name":
                    break;
            }
        });

        ymChat.onBotClose(() -> {
            runOnUiThread(this::onBackPressed);
            Log.d("Example App", "Bot Was closed");
        });


        FragmentManager fragManager = getSupportFragmentManager();
        try {
            fragManager.beginTransaction()
                    .add(R.id.containertest, ymChat.getChatBotView(TestActivity.this))
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadChatBot2() {
        YMChat ymChat = YMChat.getInstance("2345");
        ymChat.config = getConfig2(botId); // new YMConfig(botId);

        //setting event listener
        ymChat.onEventFromBot((YMBotEventResponse botEvent) -> {
            String ev =  "Event from bot 2"+ botEvent.getCode() + "Bot data" + botEvent.getData();
            Toast.makeText(TestActivity.this,    ev, Toast.LENGTH_SHORT).show();
            switch (botEvent.getCode()) {
                case "event-name":
                    break;
            }
        });
        ymChat.onBotClose(() -> {
            runOnUiThread(this::onBackPressed);
            Log.d("Example App", "Bot Was closed");
        });
        FragmentManager fragManager = getSupportFragmentManager();
        try {
            fragManager.beginTransaction()
                    .add(R.id.container2, ymChat.getChatBotView(TestActivity.this))
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private YMConfig getConfig2(String botId) {
        YMConfig config = new YMConfig(botId);
        //To enable speach to text
        //ymChat.config.enableSpeech = true;

        //Payload attributes
        HashMap<String, Object> payloadData = new HashMap<>();
        //Setting Payload Data
       /* payloadData.put("category", "4402713315609");
        payloadData.put("email", "ellowtest@g.com");
        payloadData.put("name", "Dhhd");
        payloadData.put("section", "4402713320217");
        payloadData.put("selectedArticle", "4402713344281");
        payloadData.put("ticketSubject", "rewards | 4402713344281");
        payloadData.put("userState", "application");*/

        config.payload = payloadData;

        // Choose version(1 or 2), default is 1
        config.version = 2;

        //To enable notifications
        config.deviceToken = deviceToken;

        // To Change the color of status bar, by default it will pick app theme
        config.statusBarColor = R.color.colorPrimaryDark;
        // To Change the color of close button, default color is white
        config.closeButtonColor = R.color.white;

        /* Note: if color is set from both setStatusBarColor and statusBarColorFromHex,
         * statusBarColorFromHex will take priority
         * */
        // To set statusBarColor from hexadecimal color code
        config.statusBarColorFromHex = "#49c656";

        /* Note: if color is set from both closeButtonColor and closeButtonColorHex,
         * closeButtonColorHex will take priority
         * */
        // To set closeButtonColor from hexadecimal color code
        config.closeButtonColorFromHex = "#b72a2a";

        // Set custom loader url , it should be a valid, light weight and public image url
        // This is an optional parameter
        config.customLoaderUrl = "https://yellow.ai/images/Logo.svg";

        config.ymAuthenticationToken = "2345";

        return config;

    }
}