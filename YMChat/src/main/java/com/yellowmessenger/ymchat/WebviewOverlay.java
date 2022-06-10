package com.yellowmessenger.ymchat;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yellowmessenger.ymchat.models.ConfigService;
import com.yellowmessenger.ymchat.models.JavaScriptInterface;
import com.yellowmessenger.ymchat.models.YMBotEventResponse;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
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

public class WebviewOverlay extends Fragment {
    private final String TAG = "YMChat";
    private WebView myWebView;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;
    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private String requestedPermission = null;
    private View parentLayout = null;
    private String uid;
    public String postUrl = "https://app.yellowmessenger.com/api/chat/upload?bot=";
    private String updateUserStatusUrlEndPoint = "/api/presence/usersPresence/log_user_profile";
    private boolean isAgentConnected = false;
    private boolean shouldKeepApplicationInBackground = true;
    private String ymAuthKey;

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!TextUtils.isEmpty(requestedPermission)) {
                    if (requestedPermission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        if (isGranted) {
                            launchFileIntent();
                        } else {
                            resetFilePathCallback();
                            if (getContext() != null) {
                                YmHelper.showSnackBarWithSettingAction(getContext(), parentLayout, getString(R.string.ym_message_storgae_permission));
                            }
                        }
                    } else if (requestedPermission.equals(Manifest.permission.CAMERA)) {
                        if (isGranted) {
                            launchCameraIntent();
                        } else {
                            resetFilePathCallback();
                            if (getContext() != null) {
                                YmHelper.showSnackBarWithSettingAction(getContext(), parentLayout, getString(R.string.ym_message_camera_permission));
                            }
                        }
                    } else {
                        resetFilePathCallback();
                    }
                }
            });
    private boolean isMediaUploadOptionSelected = false;

    private void resetFilePathCallback() {
        if (mFilePathCallback != null) {
            mFilePathCallback.onReceiveValue(null);
            mFilePathCallback = null;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ymAuthKey = getArguments().getString("KEY");
        // setting up local listener
        YMChat.getInstance(ymAuthKey).setLocalListener(botEvent -> {
            switch (botEvent.getCode()) {
                case "close-bot":
                    closeBot();
                    YMChat.getInstance(ymAuthKey).emitEvent(new YMBotEventResponse("bot-closed", "", false));
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
                    getActivity().runOnUiThread(() -> {
                        // hideMic();
                        // hideCloseButton();
                    });
                    break;
                case "image-closed":
                    getActivity().runOnUiThread(() -> {
                        // showCloseButton();
                        // showMic();
                    });
                case "yellowai-uid":
                    getActivity().runOnUiThread(() -> {
                        this.uid = botEvent.getData();
                    });
                    break;
                case "agent-ticket-connected":
                    isAgentConnected = true;
                    break;
                case "agent-ticket-closed":
                    isAgentConnected = false;
                    break;

            }
        });
    }

    private void updateAgentStatus(String status) {
        OkHttpClient client = new OkHttpClient();
        String url = ConfigService.getInstance(ymAuthKey).getConfig().customBaseUrl + updateUserStatusUrlEndPoint;
        if (uid != null) {
            RequestBody formBody = new FormBody.Builder()
                    .add("user", this.uid)
                    .add("resource", "bot_" + ConfigService.getInstance(ymAuthKey).getConfig().botId)
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
            String botId = ConfigService.getInstance(ymAuthKey).getConfig().botId;
            postUrl = postUrl + botId + "&uid=" + uid + "&secure=false";
            run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void run() throws IOException {

        OkHttpClient client = new OkHttpClient();

        String imagePath = ConfigService.getInstance(ymAuthKey).getCustomDataByKey("imagePath");
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myWebView = (WebView) preLoadWebView();
        return myWebView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        parentLayout = view;

        if (ConfigService.getInstance(ymAuthKey).getConfig().botId == null || ConfigService.getInstance(ymAuthKey).getConfig().botId.trim().isEmpty()) {
            //finish();
        }

        if (shouldKeepApplicationInBackground && isAgentConnected) {
            reload();
        } else {
            enableShouldKeepApplicationInBackground();
        }

    }

    //File picker activity result
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == 100) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                sendEvent(result.get(0));
            }
        }

        Uri[] results = null;

        // Check that the response is a good one
        if (resultCode == RESULT_OK) {
            if (data != null && data.getDataString() != null) {
                String dataString = data.getDataString();
                results = new Uri[]{Uri.parse(dataString)};
            } else {
                // If there is no data, then we may have taken a photo
                if (mCameraPhotoPath != null) {
                    results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                }
            }

        }
        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;

    }

    public View preLoadWebView() {
        // Preload start
        final Context context = getActivity();

        myWebView = new WebView(context);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setDomStorageEnabled(true);
        myWebView.getSettings().setSupportMultipleWindows(true);
        myWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        myWebView.getSettings().setAllowFileAccess(true);
        myWebView.getSettings().setGeolocationDatabasePath(context.getFilesDir().getPath());
        myWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        myWebView.addJavascriptInterface(new JavaScriptInterface((Activity) getActivity(), myWebView, ymAuthKey), "YMHandler");

        myWebView.setWebViewClient(new WebViewClient());

        myWebView.setWebChromeClient(new WebChromeClient() {

            private View mCustomView;
            private CustomViewCallback mCustomViewCallback;
            private int mOriginalOrientation;
            private int mOriginalSystemUiVisibility;

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                if (ConfigService.getInstance(ymAuthKey).getConfig().showConsoleLogs)
                    Log.d("WebView", consoleMessage.message());
                return true;
            }

            // For Android 5.0
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, FileChooserParams fileChooserParams) {
                // Double check that we don't have any existing callbacks
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePath;
                isMediaUploadOptionSelected = false;
                showFileChooser();
                return true;
            }

            public Bitmap getDefaultVideoPoster() {
                if (mCustomView == null) {
                    return null;
                }
                return BitmapFactory.decodeResource(context.getResources(), 2130837573);
            }

            public void onHideCustomView() {
                if (getActivity() != null) {
                    ((FrameLayout) getActivity().getWindow().getDecorView()).removeView(this.mCustomView);
                    getActivity().getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
                    getActivity().setRequestedOrientation(this.mOriginalOrientation);
                }
                this.mCustomView = null;
                this.mCustomViewCallback.onCustomViewHidden();
                this.mCustomViewCallback = null;
            }

            public void onShowCustomView(View paramView, CustomViewCallback paramCustomViewCallback) {
                if (this.mCustomView != null) {
                    onHideCustomView();
                    return;
                }
                this.mCustomView = paramView;
                if (getActivity() != null) {
                    this.mOriginalSystemUiVisibility = getActivity().getWindow().getDecorView().getSystemUiVisibility();
                    this.mOriginalOrientation = getActivity().getRequestedOrientation();
                }
                this.mCustomViewCallback = paramCustomViewCallback;
                if (getActivity() != null) {
                    ((FrameLayout) getActivity().getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
                    getActivity().getWindow().getDecorView().setSystemUiVisibility(3846 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                }
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView newWebView = new WebView(context);
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                        browserIntent.setData(Uri.parse(url));
                        startActivity(browserIntent);
                        return true;
                    }
                });
                return true;

            }
        });

        String newUrl = ConfigService.getInstance(ymAuthKey).getUrl(getString(R.string.ym_chatbot_base_url));
        myWebView.loadUrl(newUrl);
        return myWebView;
    }

    private void showFileChooser() {
        boolean hideCameraForUpload = ConfigService.getInstance(ymAuthKey).getConfig().hideCameraForUpload;
        if (hideCameraForUpload) {
            if (checkForStoragePermission(getContext())) {
                launchFileIntent();
            }
        } else {
            showBottomSheet();
        }
    }

    private void showBottomSheet() {
        if (getContext() != null) {
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_attachment);
            LinearLayout cameraLayout = bottomSheetDialog.findViewById(R.id.camera_layout);
            LinearLayout fileLayout = bottomSheetDialog.findViewById(R.id.file_layout);

            if (cameraLayout != null) {
                cameraLayout.setOnClickListener(v -> {
                    isMediaUploadOptionSelected = true;
                    checkAndLaunchCamera();
                    bottomSheetDialog.dismiss();
                });
            }

            if (fileLayout != null) {
                fileLayout.setOnClickListener(v -> {
                    isMediaUploadOptionSelected = true;
                    checkAndLaunchFilePicker();
                    bottomSheetDialog.dismiss();
                });

            }
            bottomSheetDialog.setOnDismissListener(dialogInterface -> {
                if (!isMediaUploadOptionSelected) {
                    resetFilePathCallback();
                }
            });
            bottomSheetDialog.show();
        }
    }

    private void checkAndLaunchFilePicker() {
        if (getContext() != null) {
            if (checkForStoragePermission(getContext())) {
                launchFileIntent();
            }
        }
    }

    private void checkAndLaunchCamera() {
        if (getContext() != null) {
            if (hasCameraPermissionInManifest(getContext())) {
                if (checkForCameraPermission(getContext())) {
                    launchCameraIntent();
                }
            } else {
                launchCameraIntent();
            }
        }
    }

    private void launchCameraIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (getActivity() != null && takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
            } catch (IOException ex) {
                //IO exception occcurred
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                Uri photoURI;
                if (Build.VERSION.SDK_INT >= 24 && getContext() != null) {
                    photoURI = FileProvider.getUriForFile(getContext(),
                            getString(R.string.ym_file_provider),
                            photoFile);
                } else {
                    photoURI = Uri.fromFile(photoFile);

                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                disableShouldKeepApplicationInBackground();
                startActivityForResult(takePictureIntent, INPUT_FILE_REQUEST_CODE);

            } else {
                YmHelper.showMessageInSnackBar(parentLayout, getActivity().getApplicationContext().getString(R.string.ym_message_camera_error));
            }
        }
    }


    private boolean hasCameraPermissionInManifest(Context context) {

        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);

            String[] permissions = packageInfo.requestedPermissions;

            if (permissions == null || permissions.length == 0) {
                return false;
            }

            for (String perm : permissions) {
                if (perm.equals(Manifest.permission.CAMERA))
                    return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            //Exception occurred
            return false;
        }
        return false;
    }

    private boolean checkForCameraPermission(Context context) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
        )
                == PackageManager.PERMISSION_GRANTED
        ) {
            return true;
        } else {
            requestedPermission = Manifest.permission.CAMERA;
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            return false;
        }
    }

    private void launchFileIntent() {
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("*/*");
        if (getActivity() != null) {
            disableShouldKeepApplicationInBackground();
            startActivityForResult(contentSelectionIntent, INPUT_FILE_REQUEST_CODE);
        }
    }

    // Sending messages to bot
    public void sendEvent(String s) {
        myWebView.loadUrl("javascript:sendEvent(\"" + s + "\");");
    }

    public void closeBot() {
        try {
            requireActivity().runOnUiThread(new Runnable() {
                public void run() {
                    myWebView.loadUrl("");
                }
            });
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    // creating image filename
    private File createImageFile() throws IOException {

        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getContext().getExternalCacheDir();
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",  /* suffix */
                storageDir      /* directory */
        );
        return image;
    }


    private boolean checkForStoragePermission(Context context) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
        )
                == PackageManager.PERMISSION_GRANTED
        ) {
            return true;
        } else {
            requestedPermission = Manifest.permission.READ_EXTERNAL_STORAGE;
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            return false;
        }
    }

    public void enableShouldKeepApplicationInBackground() {
        shouldKeepApplicationInBackground = true;
    }

    public void disableShouldKeepApplicationInBackground() {
        shouldKeepApplicationInBackground = false;
    }

    @Override
    public void onPause() {
        if (shouldKeepApplicationInBackground && isAgentConnected) {
            updateAgentStatus("offline");
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if (shouldKeepApplicationInBackground && isAgentConnected) {
            updateAgentStatus("offline");
        }
        super.onStop();
    }

    void reload() {
        if (myWebView != null) {
            myWebView.reload();
        }
    }

    public static WebviewOverlay newInstance(String ymAuthenticationToken) {
        Bundle args = new Bundle();
        args.putString("KEY", ymAuthenticationToken);
        WebviewOverlay fragment = new WebviewOverlay();
        fragment.setArguments(args);
        return fragment;
    }
}
