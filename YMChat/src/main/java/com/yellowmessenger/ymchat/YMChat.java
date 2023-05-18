package com.yellowmessenger.ymchat;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yellowmessenger.ymchat.models.ConfigService;
import com.yellowmessenger.ymchat.models.YMBotEventResponse;
import com.yellowmessenger.ymchat.models.YellowCallback;
import com.yellowmessenger.ymchat.models.YellowDataCallback;
import com.yellowmessenger.ymchat.models.YellowGenericResponseModel;
import com.yellowmessenger.ymchat.models.YellowUnreadMessageResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
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
    private final String unlinkNotificationUrl = "/api/plugin/removeDeviceToken?bot=";
    private final String registerDeviceUrl = "/api/mobile-backend/device-token?bot=";
    private final String unreadMessagesUrl = "/api/mobile-backend/message/unreadMsgs?bot=";

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

    public void reloadBot() {
        emitLocalEvent(new YMBotEventResponse("reload-bot", "", true));
    }


    public void startChatbot(@NonNull Context context) throws Exception {
        try {
            if (validate(context)) {
                ConfigService.getInstance().setConfigData(config);
                Intent _intent = new Intent(context, YellowBotWebViewActivity.class);
                _intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(_intent);
            }
        } catch (Exception e) {
            throw new Exception(("Exception in staring chat bot ::\nException message :: " + e.getMessage()));
        }
    }

    public Fragment getChatBotView(@NonNull Context context) throws Exception {
        try {
            if (validate(context)) {
                ConfigService.getInstance().setConfigData(config);
                return YellowBotWebviewFragment.Companion.newInstance();
            }
        } catch (Exception e) {
            throw new Exception(("Exception in staring chat bot ::\nException message :: " + e.getMessage()));
        }
        return null;
    }

    private boolean validate(Context context) throws Exception {
        if (context == null) {
            throw new Exception("Context passed is null. Please pass valid context");
        }

        if (config == null) {
            throw new Exception("Please initialise config, it cannot be null.");
        }

        if (config.botId == null || config.botId.trim().isEmpty()) {
            throw new Exception("botId is not configured. Please set botId before calling startChatbot()");
        }
        if (config.customBaseUrl == null || config.customBaseUrl.isEmpty()) {
            throw new Exception("customBaseUrl cannot be null or empty.");
        }

        if (config.customLoaderUrl == null || config.customLoaderUrl.isEmpty() || !isValidUrl(config.customLoaderUrl)) {
            throw new Exception("Please provide valid customLoaderUrl");
        }

        if (config.payload != null) {
            try {
                URLEncoder.encode(new Gson().toJson(config.payload), "UTF-8");
            } catch (Exception e) {
                throw new Exception("In payload map, value can be of primitive type or json convertible value ::\nException message :: " + e.getMessage());
            }
        }

        if (!(config.version == 1 || config.version == 2)) {
            throw new Exception("version can be either 1 or 2");
        }
        return true;
    }

    private boolean isValidUrl(String url) {
        /* Try creating a valid URL */
        try {
            new URL(url).toURI();
            return true;
        }

        // If there was an Exception
        // while creating URL object
        catch (Exception e) {
            return false;
        }
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

    /**
     * @deprecated use
     * {@link #unlinkDeviceToken(String, YMConfig, YellowCallback)}
     * You can set `botId`, `deviceToken` in YMConfig object.
     * If your bot is deployed in different region (apart from India) or it is a on-prem url please pass `customBaseUrl` as well
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    public void unlinkDeviceToken(String botId, String apiKey, String deviceToken, YellowCallback callback) throws Exception {
        YMConfig ymConfig = new YMConfig(botId);
        ymConfig.deviceToken = deviceToken;
        ymConfig.customBaseUrl = "https://cloud.yellow.ai";
        unlinkDeviceToken(apiKey, ymConfig, callback);
    }


    public void unlinkDeviceToken(String apiKey, YMConfig ymConfig, YellowCallback callback) throws Exception {
        try {
            if (isValidate(ymConfig.botId, apiKey, ymConfig.deviceToken, ymConfig.customBaseUrl, callback)) {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        // create your json here
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("deviceToken", ymConfig.deviceToken);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        String postUrl = ymConfig.customBaseUrl + unlinkNotificationUrl + ymConfig.botId;
                        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                        // put your json here
                        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());

                        OkHttpClient client = new OkHttpClient();

                        Request request = new Request.Builder()
                                .url(postUrl)
                                .addHeader("x-api-key", apiKey)
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

    public void registerDevice(String apiKey, YMConfig ymConfig, YellowCallback callback) throws Exception {
        try {
            if (isRegisterDeviceParamsValidated(ymConfig.botId, apiKey, ymConfig.deviceToken, ymConfig.ymAuthenticationToken, ymConfig.customBaseUrl, callback)) {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        // create your json here
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("deviceToken", ymConfig.deviceToken);
                            jsonObject.put("ymAuthenticationToken", ymConfig.ymAuthenticationToken);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        String postUrl = ymConfig.customBaseUrl + registerDeviceUrl + ymConfig.botId;
                        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                        // put your json here
                        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());

                        OkHttpClient client = new OkHttpClient();

                        Request request = new Request.Builder()
                                .url(postUrl)
                                .addHeader("x-api-key", apiKey)
                                .addHeader("Content-Type", "application/json")
                                .put(requestBody)
                                .build();

                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                call.cancel();
                                sendFailureCallback(callback, "Failed to register device :: Error message :: " + e.getMessage());
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
                                                sendFailureCallback(callback, "Failed to register device :: Error message :: " + message);
                                            }
                                            // Do something here
                                        } catch (JSONException e) {
                                            sendFailureCallback(callback, "Failed to register device :: Error message :: " + e.getMessage());
                                        }
                                    }
                                } else if (response.code() >= 400 && response.code() <= 499) {
                                    sendFailureCallback(callback, "Failed to register device. Please make sure you are passing correct `apiKey`");
                                } else {
                                    sendFailureCallback(callback, "Failed to register device. Please try after sometime.");
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

    public void getUnreadMessagesCount(YMConfig ymConfig, YellowDataCallback callback) throws Exception {
        try {
            if (isUnreadParamsValidated(ymConfig.botId, ymConfig.ymAuthenticationToken, ymConfig.customBaseUrl, callback)) {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        // create your json here
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("ymAuthenticationToken", ymConfig.ymAuthenticationToken);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        String postUrl = ymConfig.customBaseUrl + unreadMessagesUrl + ymConfig.botId;
                        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                        // put your json here
                        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());

                        OkHttpClient client = new OkHttpClient();

                        Request request = new Request.Builder()
                                .url(postUrl)
                                .addHeader("Content-Type", "application/json")
                                .post(requestBody)
                                .build();

                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                call.cancel();
                                sendFailureDataCallback(callback, "Failed to get unread messages :: Error message :: " + e.getMessage());
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                Log.d(TAG, response.body().toString());
                                if (response.isSuccessful()) {
                                    ResponseBody body = response.body();
                                    if (body != null) {
                                        try {
                                            Type collectionType = new TypeToken<YellowGenericResponseModel<YellowUnreadMessageResponse>>() {
                                            }.getType();
                                            YellowGenericResponseModel<YellowUnreadMessageResponse> resp = new Gson().fromJson(body.string(), collectionType);

                                            boolean isSuccess = resp.getSuccess();
                                            String message = resp.getMessage();
                                            YellowUnreadMessageResponse unreadMessages = resp.getData();

                                            if (isSuccess) {
                                                sendSuccessDataCallback(callback, unreadMessages);
                                            } else {
                                                sendFailureDataCallback(callback, "Failed to get unread messages :: Error message :: " + message);
                                            }
                                            // Do something here
                                        } catch (Exception e) {
                                            sendFailureDataCallback(callback, "Failed to get unread messages :: Error message :: " + e.getMessage());
                                        }
                                    }
                                } else if (response.code() >= 400 && response.code() <= 499) {
                                    sendFailureDataCallback(callback, "Failed to get unread messages . Please make sure you are passing correct `apiKey`");
                                } else {
                                    sendFailureDataCallback(callback, "Failed to get unread messages . Please try after sometime.");
                                }

                            }
                        });
                    }
                };
                thread.start();
            }
        } catch (Exception e) {
            throw new Exception("Exception in getting unread messages  ::\nException message :: " + e.getMessage());
        }
    }


    private void sendFailureCallback(YellowCallback callback, String message) {
        new Handler(Looper.getMainLooper()).post(() -> callback.failure(message));
    }

    private void sendSuccessCallback(YellowCallback callback) {
        new Handler(Looper.getMainLooper()).post(callback::success);
    }

    private void sendFailureDataCallback(YellowDataCallback callback, String message) {
        new Handler(Looper.getMainLooper()).post(() -> callback.failure(message));
    }

    private void sendSuccessDataCallback(YellowDataCallback callback, YellowUnreadMessageResponse unreadMessageResponse) {
        new Handler(Looper.getMainLooper()).post(() -> callback.success(unreadMessageResponse));
    }

    private boolean isRegisterDeviceParamsValidated(String botId, String apiKey, String deviceToken, String userId, String customBaseUrl, YellowCallback callback) throws Exception {
        isValidParam(botId, "Bot Id");
        isValidParam(apiKey, "Api Key");
        isValidParam(deviceToken, "Device Token");
        isValidParam(userId, "User Id");
        isValidParam(customBaseUrl, "Custom base url");

        if (callback == null)
            throw new Exception("callback cannot be null");

        return true;
    }

    private boolean isUnreadParamsValidated(String botId, String userId, String customBaseUrl, YellowDataCallback callback) throws Exception {
        isValidParam(botId, "Bot Id");
        isValidParam(userId, "User Id");
        isValidParam(customBaseUrl, "Custom base url");

        if (callback == null)
            throw new Exception("callback cannot be null");

        return true;
    }

    private boolean isValidParam(String param, String key) throws Exception {
        if (param == null || param.isEmpty()) {
            String msg = key + "cannot be null or empty";
            throw new Exception(msg);
        }
        return true;
    }

    private boolean isValidate(String botId, String apiKey, String deviceToken, String customBaseUrl, YellowCallback callback) throws Exception {
        isValidParam(botId, "Bot Id");
        isValidParam(apiKey, "Api Key");
        isValidParam(deviceToken, "Device Token");
        isValidParam(customBaseUrl, "Custom base url");

        if (callback == null)
            throw new Exception("callback cannot be null");

        return true;
    }
}


