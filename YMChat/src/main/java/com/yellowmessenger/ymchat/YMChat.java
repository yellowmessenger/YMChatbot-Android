package com.yellowmessenger.ymchat;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.yellowmessenger.ymchat.models.ConfigService;
import com.yellowmessenger.ymchat.models.YMBotEventResponse;
import com.yellowmessenger.ymchat.models.YellowCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class YMChat {
    private final String TAG = "YMChat";
    private BotEventListener listener, localListener;
    private BotCloseEventListener botCloseEventListener;
    private static YMChat botPluginInstance;
    public YMConfig config;
    private String unlinkNotificationUrl = "https://app.yellow.ai/api/plugin/removeDeviceToken?bot=";

    private YMChat() {
        this.listener = botEvent -> {
        };
    }

    public static YMChat getInstance() {
        if (botPluginInstance == null) {
            synchronized (YMChat.class) {
                if (botPluginInstance == null) {
                    botPluginInstance = new YMChat();
                }
            }
        }
        return botPluginInstance;
    }

    public void setLocalListener(BotEventListener localListener) {
        this.localListener = localListener;
    }

    public void onEventFromBot(BotEventListener listener) {
        this.listener = listener;
    }

    public void onBotClose(BotCloseEventListener listener) {
        this.botCloseEventListener = listener;
    }


    public void startChatbot(@NonNull Context context) throws Exception {
        try {
            if (validate(context)) {
                ConfigService.getInstance().setConfigData(config);
                Intent _intent = new Intent(context, BotWebView.class);
                _intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(_intent);
            }
        } catch (Exception e) {
            throw new Exception(("Exception in staring chat bot ::\nException message :: " + e.getMessage()));
        }
    }

    private boolean validate(Context context) throws Exception {
        if (context == null) {
            throw new Exception("Context passed is null. Please pass valid context");
        }

        if (config == null) {
            throw new Exception("Please initialise config, it cannot be null.");
        }

        if (config.botId == null || config.botId.isEmpty()) {
            throw new Exception("botId is not configured. Please set botId before calling startChatbot()");
        }
        if (config.customBaseUrl == null || config.customBaseUrl.isEmpty()) {
            throw new Exception("customBaseUrl cannot be null or empty.");
        }
        if (config.payload != null) {
            try {
                URLEncoder.encode(new Gson().toJson(config.payload), "UTF-8");
            } catch (Exception e) {
                throw new Exception("In payload map, value can be of primitive type or json convertible value ::\nException message :: " + e.getMessage());
            }
        }
        return true;
    }

    public void closeBot() {
        if (localListener != null)
            localListener.onSuccess(new YMBotEventResponse("close-bot", "", false));
    }

    public void emitEvent(YMBotEventResponse event) {
        if (event != null) {
            if (botCloseEventListener != null && event.getCode() != null && isCloseBotEvent(event)) {
                botCloseEventListener.onClosed();
            } else {
                if (listener != null)
                    listener.onSuccess(event);
            }

        }
    }


    public void emitLocalEvent(YMBotEventResponse event) {
        if (event != null) {
            if (localListener != null)
                localListener.onSuccess(event);
        }
    }

    private boolean isCloseBotEvent(YMBotEventResponse event) {
        return (event.getCode() != null && event.getCode().equals("bot-closed"));
    }

    public void unlinkDeviceToken(String botId, String apiKey, String deviceToken, YellowCallback callback) throws Exception {
        try {
            if (isValidate(botId, apiKey, deviceToken,callback)) {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        // create your json here
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("deviceToken", deviceToken);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        String postUrl = unlinkNotificationUrl + botId;
                        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                        // put your json here
                        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());

                        OkHttpClient client = new OkHttpClient();

                        Request request = new Request.Builder()
                                .url(postUrl)
                                .addHeader("x-auth-token", apiKey)
                                .addHeader("Content-Type", "application/json")
                                .post(requestBody)
                                .build();

                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                call.cancel();
                                sendFailureCallback(callback, "Failed to unlink the device :: Error message :: " + e.getMessage());
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                Log.d(TAG, response.body().toString());
                                if (response.isSuccessful()) {
                                    ResponseBody body = response.body();
                                    if (body != null) {
                                        try {
                                            JSONObject jsonObject = new JSONObject(body.string());
                                            boolean isSuccess = jsonObject.getBoolean("success");
                                            String message = jsonObject.getString("message");
                                            if (isSuccess) {
                                                sendSuccessCallback(callback);
                                            } else {
                                                sendFailureCallback(callback, "Failed to unlink the device :: Error message :: " + message);
                                            }
                                            // Do something here
                                        } catch (JSONException e) {
                                            sendFailureCallback(callback, "Failed to unlink the device :: Error message :: " + e.getMessage());
                                        }
                                    }
                                } else if (response.code() >= 400 && response.code() <= 499) {
                                    sendFailureCallback(callback, "Failed to unlink the device. Please make sure you are passing correct `apiKey`");
                                } else {
                                    sendFailureCallback(callback, "Failed to unlink the device. Please try after sometime.");
                                }

                            }
                        });
                    }
                };
                thread.start();
            }
        } catch (Exception e) {
            throw new Exception("Exception in unlink notification ::\nException message :: " + e.getMessage());
        }
    }

    private void sendFailureCallback(YellowCallback callback, String message) {
        new Handler(Looper.getMainLooper()).post(() -> callback.failure(message));
    }

    private void sendSuccessCallback(YellowCallback callback) {
        new Handler(Looper.getMainLooper()).post(callback::success);
    }


    private boolean isValidate(String botId, String apiKey, String deviceToken, YellowCallback callback) throws Exception {
        if (botId == null || botId.isEmpty()) {
            throw new Exception("botId is cannot be null or empty");
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("apiKey is cannot be null or empty");
        }

        if (deviceToken == null || deviceToken.isEmpty()) {
            throw new Exception("deviceToken is cannot be null or empty");
        }

        if(callback == null)
            throw new Exception("callback cannot be null");

        return true;
    }
}


