package com.yellowmessenger.ymchat;


import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skyfishjy.library.RippleBackground;
import com.yellowmessenger.ymchat.models.ConfigService;
import com.yellowmessenger.ymchat.models.YMBotEventResponse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
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
    private boolean willStartMic = false;
    public String postUrl = "https://app.yellowmessenger.com/api/chat/upload?bot=";


    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    public void startMic(long countdown_time) {
        RelativeLayout voiceArea = findViewById(R.id.voiceArea);
        if (!willStartMic) {
            willStartMic = true;
            new CountDownTimer(countdown_time, 1000) {
                public void onTick(long millisUntilFinished) {
                }

                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                public void onFinish() {
                    if (voiceArea.getVisibility() == View.INVISIBLE && willStartMic) {
                        toggleBottomSheet();
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
            String color = ConfigService.getInstance().getConfig().statusBarColor;
            int customColor = -1;
            try {

                customColor = Integer.parseInt(color);
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }

            if (customColor != -1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Window window = BotWebView.this.getWindow();
                    // clear FLAG_TRANSLUCENT_STATUS flag:
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                    // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    // finally change the color
                    window.setStatusBarColor(customColor);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Incorrect color code for status bar.");
        }
    }

    public void setActionBarColor() {
        try {
            String color = ConfigService.getInstance().getConfig().actionBarColor;
            int customColor = -1;
            try {
                customColor = Integer.parseInt(color);
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }

            if (customColor != -1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ActionBar actionBar = BotWebView.this.getSupportActionBar();
                    if (actionBar != null) {
                        actionBar.setBackgroundDrawable(new ColorDrawable(customColor));
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Incorrect color code for App bar.");
        }
    }

    public void setOverviewColor() {
        try {
            String color = ConfigService.getInstance().getConfig().actionBarColor;
            int customColor = -1;
            try {
                customColor = Integer.parseInt(color);
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }

            if (customColor != -1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ActivityManager.TaskDescription td = new ActivityManager.TaskDescription(null, null, customColor);
                    this.setTaskDescription(td);
                }
            }
        } catch (Exception e) {

            Log.d(TAG, "Incorrect color code for overview title bar.");
        }


    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor();
        setActionBarColor();
        setOverviewColor();

        // setting up local listener
        Log.d(TAG, "onCreate: setting up local listener");
        YMChat.getInstance().setLocalListener(botEvent -> {
            Log.d(TAG, "onSuccess: " + botEvent.getCode());

            switch (botEvent.getCode()) {
                case "close-bot":
                    closeBot();
                    this.finish();
                    YMChat.getInstance().emitEvent(new YMBotEventResponse("bot-closed", ""));
                    break;
                case "upload-image":
                    Log.d(TAG, "onSuccess: got event");
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
            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content), (v, insets) -> {
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    params.bottomMargin = insets.getSystemWindowInsetBottom();
                    return insets.consumeSystemWindowInsets();
                });
        setContentView(R.layout.activity_bot_web_view);

        fh = new WebviewOverlay();
        FragmentManager fragManager = getSupportFragmentManager();
        fragManager.beginTransaction()
                .add(R.id.container, fh)
                .commit();
        boolean enableSpeech = ConfigService.getInstance().getConfig().enableSpeech;
        if (enableSpeech) {
            FloatingActionButton micButton = findViewById(R.id.floatingActionButton);
            micButton.setVisibility(View.VISIBLE);
            micButton.setOnClickListener(view -> toggleBottomSheet());
        }


        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> {
            YMChat.getInstance().emitEvent(new YMBotEventResponse("bot-closed", ""));
            fh.closeBot();
            this.finish();
        });
        boolean showCloseButton = ConfigService.getInstance().getConfig().showCloseButton;
        if (!showCloseButton) {
            backButton.setVisibility(View.INVISIBLE);
        }


    }

    @Override
    public void onResume() {
        super.onResume();
        setOverviewColor();
    }


    public void runUpload(String uid) {
        try {
            if (uid == null) {
                Log.e(TAG, "uid is null from bot.");
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
            Log.d(TAG, "run: " + imagePath);

            File sourceFile = new File(imagePath);

            Log.d(TAG, "File...::::" + sourceFile + " : " + sourceFile.exists());

            final MediaType MEDIA_TYPE = imagePath.endsWith("png") ?
                    MediaType.parse("image/png") : MediaType.parse("image/jpeg");
            Log.d(TAG, "run: " + postUrl);
            Log.d(TAG, sourceFile.getName());


            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("images", sourceFile.getName(), RequestBody.create(MEDIA_TYPE, sourceFile))
//                .addFormDataPart("images", sourceFile.getName()+"."+MEDIA_TYPE.subtype(), RequestBody.create(MEDIA_TYPE, sourceFile))
                    .build();

            Request request = new Request.Builder()
                    .url(postUrl)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    call.cancel();
                    Log.d("Upload", "Can't upload");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    final String myResponse = response.body() != null ? response.body().string() : "";

                    BotWebView.this.runOnUiThread(() -> Log.d("Upload", myResponse));

                }
            });
        } else {
            Log.e(TAG, "imagePath is either null or empty");
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        YMChat.getInstance().emitEvent(new YMBotEventResponse("bot-closed", ""));
        if (fh != null) {
            fh.closeBot();
        }
        this.finish();
    }

    private void speechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
        startActivityForResult(intent, 100);

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
        Log.d(TAG, "startListeningWithoutDialog: " + defaultLanguage);
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void toggleBottomSheet() {

        final RippleBackground rippleBackground = (RippleBackground) findViewById(R.id.animated_btn);
        RelativeLayout voiceArea = findViewById(R.id.voiceArea);
        FloatingActionButton micButton = findViewById(R.id.floatingActionButton);
        TextView textView = findViewById(R.id.speechTranscription);

        if (voiceArea.getVisibility() == View.INVISIBLE) {
            textView.setText("I'm listening...");
            willStartMic = false;
            voiceArea.setVisibility(View.VISIBLE);
            rippleBackground.startRippleAnimation();
            startListeningWithoutDialog();

            micButton.setImageDrawable(getDrawable(R.drawable.ic_back_button));
        } else {
            voiceArea.setVisibility(View.INVISIBLE);
            rippleBackground.stopRippleAnimation();
            micButton.setImageDrawable(getDrawable(R.drawable.ic_mic_button));
            if (sr != null) {
                sr.stopListening();
            }
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void closeVoiceArea() {
        final RippleBackground rippleBackground = (RippleBackground) findViewById(R.id.animated_btn);
        RelativeLayout voiceArea = findViewById(R.id.voiceArea);
        FloatingActionButton micButton = findViewById(R.id.floatingActionButton);
        TextView textView = findViewById(R.id.speechTranscription);

        voiceArea.setVisibility(View.INVISIBLE);
        rippleBackground.stopRippleAnimation();
        micButton.setImageDrawable(getDrawable(R.drawable.ic_mic_button));
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
            Log.d("BotWebView", "onActivityResult is being called");
            fh.onActivityResult(requestCode, resultCode, data);
        }


        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == 100) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                fh.sendEvent(result.get(0));
            }
        } else {
            Toast.makeText(getApplicationContext(), "Failed to recognize speech!", Toast.LENGTH_LONG).show();
        }
    }

    private String speech_result = "";


    class CustomRecognitionListener implements RecognitionListener {
        boolean singleResult = true;

        private static final String TAG = "RecognitionListener";


        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "onReadyForSpeech");
        }

        public void onBeginningOfSpeech() {

        }

        public void onRmsChanged(float rmsdB) {
            Log.d(TAG, "onRmsChanged");

        }

        public void onBufferReceived(byte[] buffer) {
            Log.d(TAG, "onBufferReceived");
        }

        public void onEndOfSpeech() {
            Log.d(TAG, "onEndofSpeech");
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void onError(int error) {
            closeVoiceArea();
            View parentLayout = findViewById(android.R.id.content);
            Snackbar snackbar = Snackbar
                    .make(parentLayout, "We've encountered an error. Please press Mic to continue with voice input.", Snackbar.LENGTH_LONG);
            snackbar.show();

        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
            Log.d(TAG, "onPartialResults " + value);
            TextView textView = findViewById(R.id.speechTranscription);
            textView.setText(value);
        }

        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, "onEvent " + eventType);
        }
    }


}

