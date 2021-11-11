package com.yellowmessenger.ymchat;

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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.yellowmessenger.ymchat.models.ConfigService;
import com.yellowmessenger.ymchat.models.JavaScriptInterface;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WebviewOverlay extends Fragment {
    private final String TAG = "YMChat";
    private WebView myWebView;
    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI = null;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;
    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private boolean isCameraPermissionDenied = false;
    private String requestedPermission = null;
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!TextUtils.isEmpty(requestedPermission)) {
                    if (requestedPermission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        if (isGranted) {
                            showFileChooser();
                        } else {
                            if (mFilePathCallback != null) {
                                mFilePathCallback.onReceiveValue(null);
                                mFilePathCallback = null;
                            }
                            Toast.makeText(getContext(), "Read storage permission required to complete this operation.", Toast.LENGTH_LONG).show();
                        }
                    } else if (requestedPermission.equals(Manifest.permission.CAMERA)) {
                        if (!isGranted) {
                            isCameraPermissionDenied = true;
                        }
                        showFileChooser();
                    } else {
                        if (mFilePathCallback != null) {
                            mFilePathCallback.onReceiveValue(null);
                            mFilePathCallback = null;
                        }
                    }
                }
            });


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myWebView = (WebView) preLoadWebView();
        return myWebView;
    }

    //File picker activity result
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        Uri[] results = null;

        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
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

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
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
        myWebView.addJavascriptInterface(new JavaScriptInterface((BotWebView) getActivity(), myWebView), "YMHandler");

        myWebView.setWebViewClient(new WebViewClient());

        myWebView.setWebChromeClient(new WebChromeClient() {

            private View mCustomView;
            private CustomViewCallback mCustomViewCallback;
            private int mOriginalOrientation;
            private int mOriginalSystemUiVisibility;

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                if (ConfigService.getInstance().getConfig().showConsoleLogs)
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

        StringBuilder botUrlBuilder = new StringBuilder();
        botUrlBuilder.append(getString(R.string.ym_chatbot_base_url));
        botUrlBuilder.append(ConfigService.getInstance().getBotURLParams());
        String botUrl = botUrlBuilder.toString();
        myWebView.loadUrl(botUrl);
        return myWebView;
    }

    private void showFileChooser() {
        boolean hideCameraForUpload = ConfigService.getInstance().getConfig().hideCameraForUpload;
        if (checkForStoragePermission(getContext())) {
            Intent takePictureIntent = null;
            if (!hideCameraForUpload) {
                //Check if Camera Permission is used in Parent App
                //if true ,
                if (hasCameraPermissionInManifest(getContext())) {
                    // check if app is granted permission for Camera
                    // If true show Camera option else ask for permission
                    if (checkForCameraPermission(getContext())) {
                        takePictureIntent = getPictureIntent();
                        showContentPicker(takePictureIntent);
                    } else if (isCameraPermissionDenied) {
                        showContentPicker(takePictureIntent);
                    }
                } else {
                    takePictureIntent = getPictureIntent();
                    showContentPicker(takePictureIntent);
                }
            } else {
                showContentPicker(takePictureIntent);
            }
        }

    }

    private Intent getPictureIntent() {
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

            } else {
                takePictureIntent = null;
            }
        }
        return takePictureIntent;
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
            if (!isCameraPermissionDenied) {
                requestedPermission = Manifest.permission.CAMERA;
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
            return false;
        }
    }

    private void showContentPicker(Intent takePictureIntent) {
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("*/*");

        Intent[] intentArray;
        if (takePictureIntent != null) {
            intentArray = new Intent[]{takePictureIntent};
        } else {
            intentArray = new Intent[0];
        }
        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
        getActivity().startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
    }

    // Sending messages to bot
    public void sendEvent(String s) {
        myWebView.loadUrl("javascript:sendEvent(\"" + s + "\");");
    }

    //Empty url string on bot-close
    public void closeBot() {
        myWebView.loadUrl("");
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
}
