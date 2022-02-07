package com.yellowmessenger.ymchat;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
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
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class BotWebView extends AppCompatActivity {
    private final String TAG = "YMChat";
    WebviewOverlay fh;
    private boolean willStartMic = false;
    public String postUrl = "https://app.yellowmessenger.com/api/chat/upload?bot=";
    private String updateUserStatusUrlEndPoint = "/api/presence/usersPresence/log_user_profile";
    private String uid;
    private ImageView closeButton;
    private FloatingActionButton micButton;
    private RelativeLayout parentLayout;


    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    toggleBottomSheet();
                } else {
                    YmHelper.showSnackBarWithSettingAction(BotWebView.this, parentLayout, getString(R.string.ym_message_mic_permission));
                }
            });

    public void startMic(long countdown_time) {
        RelativeLayout voiceArea = findViewById(R.id.voiceArea);
        if (!willStartMic) {
            willStartMic = true;
            new CountDownTimer(countdown_time, 1000) {
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    if (voiceArea.getVisibility() == View.INVISIBLE && willStartMic) {
                        showVoiceOption();
                    }
                }
            }.start();
        }
    }

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

    public void setStatusBarColorFromHex() {
        try {
            String color = ConfigService.getInstance().getConfig().statusBarColorFromHex;
            if (color != null && !color.isEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Window window = BotWebView.this.getWindow();
                    // clear FLAG_TRANSLUCENT_STATUS flag:
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                    // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    // finally change the color
                    window.setStatusBarColor(Color.parseColor(color));
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

    public void setCloseButtonColorFromHex() {
        try {
            String color = ConfigService.getInstance().getConfig().closeButtonColorHex;
            if (color != null && !color.isEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    DrawableCompat.setTint(
                            DrawableCompat.wrap(closeButton.getDrawable()),
                            Color.parseColor(color)
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
                    runOnUiThread(() -> {
                        hideMic();
                        hideCloseButton();
                    });
                    break;
                case "image-closed":
                    runOnUiThread(() -> {
                        showCloseButton();
                        showMic();
                    });
                case "yellowai-uid":
                    runOnUiThread(() -> {
                        this.uid = botEvent.getData();
                    });
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

        boolean enableSpeech = ConfigService.getInstance().getConfig().enableSpeech;
        micButton = findViewById(R.id.floatingActionButton);
        if (enableSpeech) {
            micButton.setVisibility(View.VISIBLE);
            micButton.setOnClickListener(view -> showVoiceOption());
            alignMicButton();
        }

        closeButton = findViewById(R.id.backButton);
        closeButton.setOnClickListener(view -> {
            YMChat.getInstance().emitEvent(new YMBotEventResponse("bot-closed", "", false));
            fh.closeBot();
            this.finish();
        });
        showCloseButton();
        setStatusBarColorFromHex();
        setCloseButtonColorFromHex();
        setKeyboardListener();
    }

    private void setKeyboardListener() {
        parentLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            parentLayout.getWindowVisibleDisplayFrame(r);
            int screenHeight = parentLayout.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            if (keypadHeight > screenHeight * 0.15) {
                hideMic();
            } else {
                showMic();
            }
        });
    }

    // Adjust view of FAB based on version
    private void alignMicButton() {
        int version = ConfigService.getInstance().getConfig().version;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) micButton.getLayoutParams();
        if (version == 1) {
            params.setMargins(0, 0, 4, 200);
        } else {
            params.setMargins(0, 0, 0, 144);
        }
        micButton.setLayoutParams(params);
    }

    private void hideCloseButton() {
        closeButton.setVisibility(View.GONE);
    }

    private void hideMic() {
        micButton.hide();
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

    private void showMic() {
        boolean enableSpeech = ConfigService.getInstance().getConfig().enableSpeech;
        if (enableSpeech) {
            micButton.show();
        } else {
            micButton.hide();
        }
    }

    private void showVoiceOption() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED) {
            toggleBottomSheet();
        } else {
            requestPermissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        fh = new WebviewOverlay();
        FragmentManager fragManager = getSupportFragmentManager();
        fragManager.beginTransaction()
                .add(R.id.container, fh)
                .commit();
    }

    @Override
    protected void onStop() {
        updateAgentStatus("offline");
        try {
            // trying to remove fragment
            getSupportFragmentManager().beginTransaction().remove(fh).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    private void updateAgentStatus(String status) {
        OkHttpClient client = new OkHttpClient();
        String url = ConfigService.getInstance().getConfig().customBaseUrl + updateUserStatusUrlEndPoint;
        if (uid != null) {
            RequestBody formBody = new FormBody.Builder()
                    .add("user", this.uid)
                    .add("resource", "bot_" + ConfigService.getInstance().getConfig().botId)
                    .add("status", status)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
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

    SpeechRecognizer sr;

    public void startListeningWithoutDialog() {
        // Intent to listen to user vocal input and return the result to the same activity.
        Context appContext = getApplicationContext();
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        // Use a language model based on free-form speech recognition.
        Map<String, Object> payload = ConfigService.getInstance().getConfig().payload;
        String defaultLanguage = payload != null ? (String) payload.get("defaultLanguage") : null;
        if (defaultLanguage == null) {
            defaultLanguage = "en";
        }
        String languagePref = defaultLanguage;
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languagePref);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languagePref);
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languagePref);


        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                appContext.getPackageName());

        // Add custom listeners.

        sr = SpeechRecognizer.createSpeechRecognizer(appContext);
        CustomRecognitionListener listener = new CustomRecognitionListener();
        sr.setRecognitionListener(listener);
        sr.startListening(intent);
    }


    public void toggleBottomSheet() {
        RelativeLayout voiceArea = findViewById(R.id.voiceArea);
        FloatingActionButton micButton = findViewById(R.id.floatingActionButton);
        TextView textView = findViewById(R.id.speechTranscription);

        if (voiceArea.getVisibility() == View.INVISIBLE) {
            textView.setText(R.string.ym_msg_listening);
            willStartMic = false;
            voiceArea.setVisibility(View.VISIBLE);
            startListeningWithoutDialog();

            micButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_back_button_ym));
        } else {
            voiceArea.setVisibility(View.INVISIBLE);
            micButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_mic_ym_small));
            if (sr != null) {
                sr.stopListening();
            }
        }


    }

    public void closeVoiceArea() {
        RelativeLayout voiceArea = findViewById(R.id.voiceArea);
        FloatingActionButton micButton = findViewById(R.id.floatingActionButton);
        TextView textView = findViewById(R.id.speechTranscription);

        voiceArea.setVisibility(View.INVISIBLE);
        micButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_mic_ym_small));
        if (sr != null) {
            sr.stopListening();
            sr.destroy();
        }
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

    private String speech_result = "";


    class CustomRecognitionListener implements RecognitionListener {
        boolean singleResult = true;

        private static final String TAG = "RecognitionListener";


        public void onReadyForSpeech(Bundle params) {
        }

        public void onBeginningOfSpeech() {
        }

        public void onRmsChanged(float rmsdB) {
        }

        public void onBufferReceived(byte[] buffer) {
        }

        public void onEndOfSpeech() {
        }

        public void onError(int error) {
            closeVoiceArea();
            View parentLayout = findViewById(android.R.id.content);
            Snackbar snackbar = Snackbar
                    .make(parentLayout, "We've encountered an error. Please press Mic to continue with voice input.", Snackbar.LENGTH_LONG);
            snackbar.show();

        }

        public void onResults(Bundle results) {
            ArrayList<String> result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            TextView textView = findViewById(R.id.speechTranscription);
            textView.setText(result != null && result.size() > 0 ? result.get(0) : "");

            if (singleResult) {
                if (result != null && result.size() > 0) {
                    speech_result = result.get(0);
                    if (sr != null)
                        sr.cancel();

                    if (fh != null)
                        fh.sendEvent(result.get(0));
                }
                closeVoiceArea();
                singleResult = false;
            }


        }

        public void onPartialResults(Bundle partialResults) {
            String value = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) != null
                    && partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).size() > 0
                    ? partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0)
                    : "";
            TextView textView = findViewById(R.id.speechTranscription);
            textView.setText(value);
        }

        public void onEvent(int eventType, Bundle params) {
        }
    }


}

