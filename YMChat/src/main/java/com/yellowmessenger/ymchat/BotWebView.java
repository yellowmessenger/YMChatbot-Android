package com.yellowmessenger.ymchat;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yellowmessenger.ymchat.models.ConfigService;
import com.yellowmessenger.ymchat.models.YMBotEventResponse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class BotWebView extends AppCompatActivity {
    private final String TAG = "YMChat";
    WebviewOverlay fh;
    public String postUrl = "https://app.yellowmessenger.com/api/chat/upload?bot=";

    private ImageView closeButton;
    private FloatingActionButton micButton;
    private RelativeLayout parentLayout;


    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    public void closeBot() {
        fh.closeBot();
    }

    public void setStatusBarColor() {
        try {
            int color = ConfigService.getInstance().getConfig().statusBarColor;
            if (color != -1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Window window = BotWebView.this.getWindow();
                    // clear FLAG_TRANSLUCENT_STATUS flag:
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                    // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    // finally change the color
                    window.setStatusBarColor(ContextCompat.getColor(this, color));
                }
            }
        } catch (Exception e) {
            //Exception occurred
        }
    }

    public void setCloseButtonColor() {
        try {
            int color = ConfigService.getInstance().getConfig().closeButtonColor;
            if (color != -1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    DrawableCompat.setTint(
                            DrawableCompat.wrap(closeButton.getDrawable()),
                            ContextCompat.getColor(this, color)
                    );
                }
            }
        } catch (Exception e) {
            //Exception occurred
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor();

        // setting up local listener
        YMChat.getInstance().setLocalListener(botEvent -> {
            switch (botEvent.getCode()) {
                case "close-bot":
                    closeBot();
                    YMChat.getInstance().emitEvent(new YMBotEventResponse("bot-closed", "", false));
                    this.finish();
                    break;
                case "upload-image":
                    Map<String, Object> retMap = new Gson().fromJson(
                            botEvent.getData(), new TypeToken<HashMap<String, Object>>() {
                            }.getType());
                    if (retMap != null && retMap.containsKey("uid")) {
                        Object uid = retMap.get("uid");
                        if (uid instanceof String) {
                            String uId = (String) retMap.get("uid");
                            runUpload(uId);
                        }
                    }
                    break;
                case "image-opened":
                    runOnUiThread(this::hideCloseButton);
                    break;
                case "image-closed":
                    runOnUiThread(this::showCloseButton);
                    break;

            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content), (v, insets) -> {
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    params.bottomMargin = insets.getSystemWindowInsetBottom();
                    return insets.consumeSystemWindowInsets();
                });
        setContentView(R.layout.activity_bot_web_view);

        parentLayout = findViewById(R.id.parentView);

        fh = new WebviewOverlay();
        FragmentManager fragManager = getSupportFragmentManager();
        fragManager.beginTransaction()
                .add(R.id.container, fh)
                .commit();

        closeButton = findViewById(R.id.backButton);
        closeButton.setOnClickListener(view -> {
            YMChat.getInstance().emitEvent(new YMBotEventResponse("bot-closed", "", false));
            fh.closeBot();
            this.finish();
        });
        showCloseButton();
    }

    private void hideCloseButton() {
        closeButton.setVisibility(View.GONE);
    }

    private void showCloseButton() {
        boolean showCloseButton = ConfigService.getInstance().getConfig().showCloseButton;
        if (showCloseButton) {
            closeButton.setVisibility(View.VISIBLE);
            setCloseButtonColor();
        } else {
            closeButton.setVisibility(View.GONE);
        }
    }


    public void runUpload(String uid) {
        try {
            if (uid == null) {
                return;
            }
            String botId = ConfigService.getInstance().getConfig().botId;
            postUrl = postUrl + botId + "&uid=" + uid + "&secure=false";
            run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void run() throws IOException {

        OkHttpClient client = new OkHttpClient();

        String imagePath = ConfigService.getInstance().getCustomDataByKey("imagePath");
        if (imagePath != null && !imagePath.isEmpty()) {

            File sourceFile = new File(imagePath);
            final MediaType MEDIA_TYPE = imagePath.endsWith("png") ?
                    MediaType.parse("image/png") : MediaType.parse("image/jpeg");
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("images", sourceFile.getName(), RequestBody.create(MEDIA_TYPE, sourceFile))
                    .build();

            Request request = new Request.Builder()
                    .url(postUrl)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    call.cancel();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                }
            });
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        YMChat.getInstance().emitEvent(new YMBotEventResponse("bot-closed", "", false));
        if (fh != null) {
            fh.closeBot();
        }
        this.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        super.onActivityResult(requestCode, resultCode, data);
        if (fh != null) {
            fh.onActivityResult(requestCode, resultCode, data);
        }


        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == 100) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                fh.sendEvent(result.get(0));
            }
        }
    }

}

