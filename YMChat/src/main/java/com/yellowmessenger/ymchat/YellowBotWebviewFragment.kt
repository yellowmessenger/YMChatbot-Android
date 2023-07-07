package com.yellowmessenger.ymchat

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.webkit.*
import android.webkit.WebView.WebViewTransport
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yellowmessenger.ymchat.models.ConfigService
import com.yellowmessenger.ymchat.models.JavaScriptInterface
import com.yellowmessenger.ymchat.models.YMBotEventResponse
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


/**
 * A simple [Fragment] subclass.
 * Use the [YellowBotWebviewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class YellowBotWebviewFragment : Fragment() {
    private var willStartMic = false
    var postUrl = "https://app.yellowmessenger.com/api/chat/upload?bot="
    private var uid: String? = null
    private lateinit var closeButton: ImageView
    private lateinit var micButton: YmMovableFloatingActionButton
    private lateinit var parentLayout: View
    private var shouldKeepApplicationInBackground = true
    private var isAgentConnected = false
    private var hasAudioPermissionInManifest = false
    private val TAG = "YMChat"
    private lateinit var myWebView: WebView
    private var mFilePathCallback: ValueCallback<Array<Uri?>>? = null
    private var mCameraPhotoPath: String? = null
    private val INPUT_FILE_REQUEST_CODE = 1
    private var requestedPermission: String? = null
    private var geoCallback: GeolocationPermissions.Callback? = null
    private var geoOrigin: String? = null
    private var isMultiFileUpload = false
    private var storgePermissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    var storgePermission33 = arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_VIDEO
    )

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if ((permissions.containsKey(Manifest.permission.READ_MEDIA_IMAGES) && permissions[Manifest.permission.READ_MEDIA_IMAGES] == true)
            || (permissions.containsKey(Manifest.permission.READ_MEDIA_VIDEO) && permissions[Manifest.permission.READ_MEDIA_VIDEO] == true)
            || (permissions.containsKey(Manifest.permission.READ_MEDIA_AUDIO) && permissions[Manifest.permission.READ_MEDIA_AUDIO] == true)
            || (permissions.containsKey(Manifest.permission.READ_EXTERNAL_STORAGE) && permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true)
        ) {
            launchFileIntent()
        } else {
            resetFilePathCallback()
            if (context != null) {
                YmHelper.showSnackBarWithSettingAction(
                    requireContext(),
                    parentLayout,
                    getString(R.string.ym_message_storgae_permission)
                )
            }
        }

    }

    private val requestPermissionLauncher = registerForActivityResult(
        RequestPermission()
    ) { isGranted: Boolean ->
        if (!TextUtils.isEmpty(requestedPermission)) {
            if (requestedPermission == Manifest.permission.CAMERA) {
                if (isGranted) {
                    launchCameraIntent()
                } else {
                    resetFilePathCallback()
                    if (context != null) {
                        YmHelper.showSnackBarWithSettingAction(
                            requireContext(),
                            parentLayout,
                            getString(R.string.ym_message_camera_permission)
                        )
                    }
                }
            } else if (requestedPermission == Manifest.permission.ACCESS_FINE_LOCATION) {
                if (isGranted && geoCallback != null && geoOrigin != null) {
                    geoCallback!!.invoke(geoOrigin, true, false)
                    geoCallback = null
                    geoOrigin = null
                } else {
                    if (geoCallback != null && geoOrigin != null) {
                        geoCallback!!.invoke(geoOrigin, false, false)
                    }
                    geoCallback = null
                    geoOrigin = null
                    if (context != null) {
                        YmHelper.showSnackBarWithSettingAction(
                            requireContext(),
                            parentLayout,
                            getString(R.string.ym_message_location_permission)
                        )
                    }
                }
            } else if (requestedPermission == Manifest.permission.RECORD_AUDIO) {
                if (isGranted) {
                    toggleBottomSheet()
                } else {
                    YmHelper.showSnackBarWithSettingAction(
                        requireContext(),
                        parentLayout,
                        getString(R.string.ym_message_mic_permission)
                    )
                }
            } else {
                resetFilePathCallback()
            }
        }
    }
    private var isMediaUploadOptionSelected = false

    private fun resetFilePathCallback() {
        if (mFilePathCallback != null) {
            mFilePathCallback!!.onReceiveValue(null)
            mFilePathCallback = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatusBarColor()

        hasAudioPermissionInManifest = hasAudioPermissionInManifest(requireContext())
        // setting up local listener
        YMChat.getInstance().setLocalListener { botEvent: YMBotEventResponse ->
            if (botEvent.code == null) return@setLocalListener
            when (botEvent.code) {
                "close-bot" -> try {
                    activity?.runOnUiThread {
                        YMChat.getInstance().emitEvent(YMBotEventResponse("bot-closed", "", false))
                        if (activity is YellowBotWebViewActivity) {
                            closeBot()
                            activity?.onBackPressed()
                        }
                    }
                } catch (e: java.lang.Exception) {
                    //Exception Occurred
                }
                "upload-image" -> {
                    val retMap =
                        Gson().fromJson<Map<String, Any>>(
                            botEvent.data,
                            object :
                                TypeToken<HashMap<String?, Any?>?>() {}.type
                        )
                    if (retMap != null && retMap.containsKey("uid")) {
                        val uid = retMap["uid"]
                        if (uid is String) {
                            val uId = retMap["uid"] as String?
                            runUpload(uId)
                        }
                    }
                }
                "image-opened" -> try {
                    activity?.runOnUiThread {
                        hideMic()
                        hideCloseButton()
                    }
                } catch (e: java.lang.Exception) {
                    //Exception Occurred
                }
                "image-closed" -> try {
                    activity?.runOnUiThread {
                        showCloseButton()
                        showMic()
                    }
                } catch (e: java.lang.Exception) {
                    //Exception Occurred
                }
                "yellowai-uid" -> try {
                    activity?.runOnUiThread { uid = botEvent.data }
                } catch (e: java.lang.Exception) {
                    //Exception Occurred
                }
                "agent-ticket-connected" -> try {
                    activity?.runOnUiThread { isAgentConnected = true }
                } catch (e: java.lang.Exception) {
                    //Exception Occurred
                }
                "agent-ticket-closed" -> try {
                    activity?.runOnUiThread { isAgentConnected = false }
                } catch (e: java.lang.Exception) {
                    //Exception Occurred
                }
                "multi-upload" -> try {
                    activity?.runOnUiThread {
                        setMultiFileUpload(true)
                    }
                } catch (e: java.lang.Exception) {
                    //Exception Occurred
                }
                "disable-multi-upload" -> try {
                    activity?.runOnUiThread {
                        setMultiFileUpload(false)
                    }
                } catch (e: java.lang.Exception) {
                    //Exception Occurred
                }
                "reload-bot" -> try {
                    activity?.runOnUiThread {
                        reload()
                    }
                } catch (e: java.lang.Exception) {
                    //Exception Occurred
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_yellow_bot_webview, container, false)
        myWebView = v.findViewById(R.id.yellowWebView)

        preLoadWebView()
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentLayout = view
        val enableSpeech = ConfigService.getInstance().config.enableSpeech
        micButton = view.findViewById(R.id.floatingActionButton)
        if (enableSpeech) {
            if (hasAudioPermissionInManifest) {
                micButton.visibility = View.VISIBLE
                micButton.setOnClickListener { showVoiceOption() }
                alignMicButton()
                val speechValue = ConfigService.getInstance().config.enableSpeechConfig
                try {
                    if (speechValue?.fabBackgroundColor?.isNotEmpty() == true) {
                        micButton.backgroundTintList =
                            ColorStateList.valueOf(Color.parseColor(speechValue.fabBackgroundColor))
                    }
                    if (speechValue?.fabIconColor?.isNotEmpty() == true) {
                        micButton.imageTintList =
                            ColorStateList.valueOf(Color.parseColor(speechValue.fabIconColor))

                    }
                } catch (e: Exception) {
                    //
                }
            } else {
                YmHelper.showMessageInSnackBar(
                    parentLayout,
                    getString(R.string.ym_declare_audio_permission)
                )
            }
        }

        closeButton = view.findViewById(R.id.backButton)
        closeButton.setOnClickListener {
            YMChat.getInstance().emitEvent(YMBotEventResponse("bot-closed", "", false))
            if (activity is YellowBotWebViewActivity) {
                closeBot()
                activity?.onBackPressed()
            }
        }
        showCloseButton()
        setStatusBarColorFromHex()
        setCloseButtonColorFromHex()
        setKeyboardListener()
    }

    //File picker activity result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        var results: Array<Uri?>? = null

        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (data != null && data.dataString != null) {
                val dataString = data.dataString
                results = arrayOf(Uri.parse(dataString))
            } else if (data != null && data.clipData != null) {
                val count = data.clipData!!.itemCount
                if (count > 0) {
                    results = arrayOfNulls(count)
                    for (i in 0 until count) {
                        results[i] = data.clipData!!.getItemAt(i).uri
                    }
                }
            } else {
                // If there is no data, then we may have taken a photo
                if (mCameraPhotoPath != null) {
                    results = arrayOf(Uri.parse(mCameraPhotoPath))
                }
            }
        }
        mFilePathCallback!!.onReceiveValue(results)
        mFilePathCallback = null
    }

    private fun preLoadWebView(): View {
        // Preload start
        val context: Context? = activity
        myWebView.settings.javaScriptEnabled = true
        myWebView.settings.domStorageEnabled = true
        myWebView.settings.setSupportMultipleWindows(true)
        myWebView.settings.javaScriptCanOpenWindowsAutomatically = true
        myWebView.settings.allowFileAccess = true
        myWebView.settings.setGeolocationDatabasePath(context?.filesDir?.path)
        myWebView.settings.mediaPlaybackRequiresUserGesture = false
        myWebView.addJavascriptInterface(
            JavaScriptInterface(requireActivity(), myWebView),
            "YMHandler"
        )
        myWebView.webViewClient = WebViewClient()
        myWebView.webChromeClient = object : WebChromeClient() {
            private var mCustomView: View? = null
            private var mCustomViewCallback: CustomViewCallback? = null
            private var mOriginalOrientation = 0
            private var mOriginalSystemUiVisibility = 0
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                if (ConfigService.getInstance().config.showConsoleLogs) Log.d(
                    "WebView",
                    consoleMessage.message()
                )
                return true
            }

            // For Android 5.0
            override fun onShowFileChooser(
                view: WebView,
                filePath: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                // Double check that we don't have any existing callbacks
                if (mFilePathCallback != null) {
                    mFilePathCallback!!.onReceiveValue(null)
                }
                mFilePathCallback = filePath as ValueCallback<Array<Uri?>>?
                isMediaUploadOptionSelected = false
                showFileChooser()
                return true
            }

            override fun getDefaultVideoPoster(): Bitmap? {
                return if (mCustomView == null) {
                    null
                } else BitmapFactory.decodeResource(context?.resources, 2130837573)
            }

            override fun onHideCustomView() {
                if (activity != null) {
                    (activity!!.window.decorView as FrameLayout).removeView(mCustomView)
                    activity!!.window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
                    activity!!.requestedOrientation = mOriginalOrientation
                }
                mCustomView = null
                mCustomViewCallback!!.onCustomViewHidden()
                mCustomViewCallback = null
            }

            override fun onShowCustomView(
                paramView: View,
                paramCustomViewCallback: CustomViewCallback
            ) {
                if (mCustomView != null) {
                    onHideCustomView()
                    return
                }
                mCustomView = paramView
                if (activity != null) {
                    mOriginalSystemUiVisibility = activity!!.window.decorView.systemUiVisibility
                    mOriginalOrientation = activity!!.requestedOrientation
                }
                mCustomViewCallback = paramCustomViewCallback
                if (activity != null) {
                    (activity!!.window.decorView as FrameLayout).addView(
                        mCustomView,
                        FrameLayout.LayoutParams(-1, -1)
                    )
                    activity!!.window.decorView.systemUiVisibility =
                        3846 or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                }
            }

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                val newWebView = context?.let { WebView(it) }
                val transport = resultMsg.obj as WebViewTransport
                transport.webView = newWebView
                resultMsg.sendToTarget()
                if (newWebView != null) {
                    newWebView.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW)
                                browserIntent.data = Uri.parse(url)
                                startActivity(browserIntent)
                            } catch (e: Exception) {
                                //Some error occurred
                            }
                            return true
                        }
                    }
                }
                return true
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                if (getContext() == null) return
                if (!hasLocationPermissionInManifest(requireContext())) {
                    YmHelper.showMessageInSnackBar(
                        parentLayout,
                        getString(R.string.ym_no_location_permission_declared)
                    )
                    return
                }
                if (checkForLocationPermission(requireContext())) {
                    callback.invoke(origin, true, false)
                } else {
                    geoOrigin = origin
                    geoCallback = callback
                }
            }
        }
        val htmlurl = if (ConfigService.getInstance().config.useLiteVersion) {
            getString(R.string.ym_lite_chatbot_base_url)

        } else {
            getString(R.string.ym_chatbot_base_url)
        }
        val newUrl = ConfigService.getInstance().getUrl(htmlurl)
        myWebView.loadUrl(newUrl)
        return myWebView
    }

    private fun showFileChooser() {
        val hideCameraForUpload = ConfigService.getInstance().config.hideCameraForUpload
        if (hideCameraForUpload || isMultiFileUpload()) {
            if (context?.let { checkForStoragePermission(it) } == true) {
                launchFileIntent()
            }
        } else {
            showBottomSheet()
        }
    }

    private fun showBottomSheet() {
        if (context != null) {
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_attachment)
            val cameraLayout = bottomSheetDialog.findViewById<LinearLayout>(R.id.camera_layout)
            val fileLayout = bottomSheetDialog.findViewById<LinearLayout>(R.id.file_layout)
            cameraLayout?.setOnClickListener { v: View? ->
                isMediaUploadOptionSelected = true
                checkAndLaunchCamera()
                bottomSheetDialog.dismiss()
            }
            fileLayout?.setOnClickListener { v: View? ->
                isMediaUploadOptionSelected = true
                checkAndLaunchFilePicker()
                bottomSheetDialog.dismiss()
            }
            bottomSheetDialog.setOnDismissListener {
                if (!isMediaUploadOptionSelected) {
                    resetFilePathCallback()
                }
            }
            bottomSheetDialog.show()
        }
    }

    private fun checkAndLaunchFilePicker() {
        if (context != null) {
            if (checkForStoragePermission(requireContext())) {
                launchFileIntent()
            }
        }
    }

    private fun checkAndLaunchCamera() {
        if (context != null) {
            if (hasCameraPermissionInManifest(requireContext())) {
                if (checkForCameraPermission(requireContext())) {
                    launchCameraIntent()
                }
            } else {
                launchCameraIntent()
            }
        }
    }

    private fun launchCameraIntent() {
        try {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (activity != null && takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
                // Create the File where the photo should go
                var photoFile: File? = null
                try {
                    photoFile = createImageFile()
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
                } catch (ex: IOException) {
                    //IO exception occurred
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.absolutePath

                    if (context == null) {
                        return
                    }

                    val appId = requireContext().packageName + ".yellow.chatbot.provider"
                    val photoURI: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        appId,
                        photoFile
                    )

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    disableShouldKeepApplicationInBackground()
                    startActivityForResult(
                        takePictureIntent,
                        INPUT_FILE_REQUEST_CODE
                    )
                } else {
                    YmHelper.showMessageInSnackBar(
                        parentLayout,
                        requireContext().getString(R.string.ym_message_camera_error)
                    )
                }
            }
        } catch (e: Exception) {
            YmHelper.showMessageInSnackBar(
                parentLayout,
                requireContext().getString(R.string.ym_message_camera_error)
            )

        }
    }


    private fun hasCameraPermissionInManifest(context: Context): Boolean {
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            val permissions = packageInfo.requestedPermissions
            if (permissions == null || permissions.isEmpty()) {
                return false
            }
            for (perm in permissions) {
                if (perm == Manifest.permission.CAMERA) return true
            }
        } catch (e: PackageManager.NameNotFoundException) {
            //Exception occurred
            return false
        }
        return false
    }

    private fun hasLocationPermissionInManifest(context: Context): Boolean {
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            val permissions = packageInfo.requestedPermissions
            if (permissions == null || permissions.isEmpty()) {
                return false
            }
            for (perm in permissions) {
                if (perm == Manifest.permission.ACCESS_FINE_LOCATION) return true
            }
        } catch (e: PackageManager.NameNotFoundException) {
            //Exception occurred
            return false
        }
        return false
    }

    private fun checkForLocationPermission(context: Context): Boolean {
        return if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            requestedPermission = Manifest.permission.ACCESS_FINE_LOCATION
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            false
        }
    }

    private fun checkForCameraPermission(context: Context): Boolean {
        return if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            requestedPermission = Manifest.permission.CAMERA
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            false
        }
    }

    private fun launchFileIntent() {
        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
        contentSelectionIntent.type = "*/*"
        if (isMultiFileUpload()) {
            contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        if (activity != null) {
            disableShouldKeepApplicationInBackground()
            startActivityForResult(
                contentSelectionIntent,
                INPUT_FILE_REQUEST_CODE
            )
        }
    }

    // Sending messages to bot
    fun sendEvent(s: String) {
        myWebView.loadUrl("javascript:sendEvent(\"$s\");")
    }

    private fun closeBot() {
        try {
            requireActivity().runOnUiThread { myWebView.loadUrl("") }
        } catch (e: Exception) {
//            e.printStackTrace();
        }
    }

    // creating image filename
    @Throws(IOException::class)
    private fun createImageFile(): File? {

        // Create an image file name
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = requireContext().externalCacheDir
        return File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
    }


    private fun checkForStoragePermission(context: Context): Boolean {
        val p: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            storgePermission33
        } else {
            storgePermissions
        }
        return if (hasStoragePermissions(context, p)) {
            true
        } else {
            requestMultiplePermissions.launch(p)
            false
        }
    }

    private fun hasStoragePermissions(context: Context, p: Array<String>): Boolean {
        p.forEach {
            if (ContextCompat.checkSelfPermission(
                    context,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                return true
            }
        }
        return false
    }


    fun reload() {
        myWebView.reload()
    }

    private fun isMultiFileUpload(): Boolean {
        return isMultiFileUpload
    }

    private fun setMultiFileUpload(multiFileUpload: Boolean) {
        isMultiFileUpload = multiFileUpload
    }


    private fun setStatusBarColor() {
        try {
            val color = ConfigService.getInstance().config.statusBarColor
            if (color != -1) {
                val window: Window = requireActivity().window
                // clear FLAG_TRANSLUCENT_STATUS flag:
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                // finally change the color
                context?.let {
                    window.statusBarColor = ContextCompat.getColor(it, color)
                }
            }
        } catch (e: java.lang.Exception) {
            //Exception occurred
        }
    }

    private fun setStatusBarColorFromHex() {
        try {
            val color = ConfigService.getInstance().config.statusBarColorFromHex
            if (color != null && color.isNotEmpty() && activity != null) {
                val window: Window = requireActivity().window
                // clear FLAG_TRANSLUCENT_STATUS flag:
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                // finally change the color
                window.statusBarColor = Color.parseColor(color)
            }
        } catch (e: java.lang.Exception) {
            //Exception occurred
        }
    }

    private fun setCloseButtonColor() {
        try {
            val color = ConfigService.getInstance().config.closeButtonColor
            if (color != -1 && context != null) {
                DrawableCompat.setTint(
                    DrawableCompat.wrap(closeButton.drawable),
                    ContextCompat.getColor(requireContext(), color)
                )
            }
        } catch (e: java.lang.Exception) {
            //Exception occurred
        }
    }

    private fun setCloseButtonColorFromHex() {
        try {
            val color = ConfigService.getInstance().config.closeButtonColorFromHex
            if (color != null && color.isNotEmpty()) {
                DrawableCompat.setTint(
                    DrawableCompat.wrap(closeButton.drawable),
                    Color.parseColor(color)
                )
            }
        } catch (e: java.lang.Exception) {
            //Exception occurred
        }
    }

    private fun setKeyboardListener() {
        parentLayout.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            parentLayout.getWindowVisibleDisplayFrame(r)
            val screenHeight = parentLayout.rootView.height
            val keypadHeight = screenHeight - r.bottom
            if (keypadHeight > screenHeight * 0.15) {
                hideMic()
            } else {
                showMic()
            }
        }
    }

    // Adjust view of FAB based on version
    private fun alignMicButton() {
        val version = ConfigService.getInstance().config.version
        val params = micButton.layoutParams as RelativeLayout.LayoutParams
        if (version == 1) {
            params.setMargins(0, 0, 4, 200)
        } else {
            params.setMargins(0, 0, 0, 186)
        }
        micButton.layoutParams = params
    }

    private fun hideCloseButton() {
        closeButton.visibility = View.GONE
    }

    private fun hideMic() {
        micButton.hide()
    }


    private fun showCloseButton() {
        val showCloseButton = ConfigService.getInstance().config.showCloseButton
        if (showCloseButton) {
            closeButton.visibility = View.VISIBLE
            setCloseButtonColor()
        } else {
            closeButton.visibility = View.GONE
        }
    }

    private fun showMic() {
        val enableSpeech = ConfigService.getInstance().config.enableSpeech
        if (enableSpeech && hasAudioPermissionInManifest) {
            micButton.show()
        } else {
            micButton.hide()
        }
    }

    private fun hasAudioPermissionInManifest(context: Context): Boolean {
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            val permissions = packageInfo.requestedPermissions
            if (permissions == null || permissions.isEmpty()) {
                return false
            }
            for (perm in permissions) {
                if (perm == Manifest.permission.RECORD_AUDIO) return true
            }
        } catch (e: PackageManager.NameNotFoundException) {
            //Exception occurred
            return false
        }
        return false
    }

    private fun showVoiceOption() {
        if (!hasAudioPermissionInManifest) {
            YmHelper.showMessageInSnackBar(
                parentLayout,
                getString(R.string.ym_declare_audio_permission)
            )
            return
        }
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.RECORD_AUDIO
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            toggleBottomSheet()
        } else {
            requestedPermission = Manifest.permission.RECORD_AUDIO
            requestPermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    override fun onStart() {
        if (ConfigService.getInstance().config.botId == null || ConfigService.getInstance().config.botId.trim()
                .isEmpty()
        ) {
            activity?.onBackPressed()
        }
        if (shouldKeepApplicationInBackground && (isAgentConnected || ConfigService.getInstance().config.alwaysReload)) {
            reload()
        } else {
            enableShouldKeepApplicationInBackground()
        }
        super.onStart()
    }

    private fun enableShouldKeepApplicationInBackground() {
        shouldKeepApplicationInBackground = true
    }

    private fun disableShouldKeepApplicationInBackground() {
        shouldKeepApplicationInBackground = false
    }

    private fun updateAgentStatus(status: String) {
        val client = OkHttpClient()
        val updateUserStatusUrlEndPoint = "/api/presence/usersPresence/log_user_profile"
        val url = ConfigService.getInstance().config.customBaseUrl + updateUserStatusUrlEndPoint
        if (uid != null) {
            val formBody: RequestBody = FormBody.Builder()
                .add("user", uid!!)
                .add("resource", "bot_" + ConfigService.getInstance().config.botId)
                .add("status", status)
                .build()
            val request: Request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    call.cancel()
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                }
            })
        }
    }

    private fun runUpload(uid: String?) {
        try {
            if (uid == null) {
                return
            }
            val botId = ConfigService.getInstance().config.botId
            postUrl = "$postUrl$botId&uid=$uid&secure=false"
            run()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun run() {
        val client = OkHttpClient()
        val imagePath = ConfigService.getInstance().getCustomDataByKey("imagePath")
        if (imagePath != null && !imagePath.isEmpty()) {
            val sourceFile = File(imagePath)
            val MEDIA_TYPE: MediaType? = if (imagePath.endsWith("png")) {
                "image/png".toMediaTypeOrNull()
            } else {
                "image/jpeg".toMediaTypeOrNull()
            }
            val requestBody: RequestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "images",
                    sourceFile.name,
                    RequestBody.create(MEDIA_TYPE, sourceFile)
                )
                .build()
            val request: Request = Request.Builder()
                .url(postUrl)
                .post(requestBody)
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    call.cancel()
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                }
            })
        }
    }

    var sr: SpeechRecognizer? = null

    private fun startListeningWithoutDialog() {
        // Intent to listen to user vocal input and return the result to the same activity.
        if (context != null) {
            val appContext: Context = requireContext()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

            // Use a language model based on free-form speech recognition.
            val payload: Map<String, Any>? = ConfigService.getInstance().config.payload
            var defaultLanguage =
                if (payload != null) payload["defaultLanguage"] as String? else null
            if (defaultLanguage == null) {
                defaultLanguage = Locale.getDefault().toString()
            }
            val languagePref: String = defaultLanguage
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languagePref)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languagePref)
            intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languagePref)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            intent.putExtra(
                RecognizerIntent.EXTRA_CALLING_PACKAGE,
                appContext.packageName
            )

            // Add custom listeners.
            sr = SpeechRecognizer.createSpeechRecognizer(appContext)
            val listener = CustomRecognitionListener()
            sr!!.setRecognitionListener(listener)
            sr!!.startListening(intent)
        }
    }


    private fun toggleBottomSheet() {
        if (context == null)
            return
        val voiceArea: RelativeLayout = parentLayout.findViewById<RelativeLayout>(R.id.voiceArea)
        val micButton: YmMovableFloatingActionButton =
            parentLayout.findViewById<YmMovableFloatingActionButton>(R.id.floatingActionButton)
        val textView: TextView = parentLayout.findViewById<TextView>(R.id.speechTranscription)
        if (voiceArea.visibility == View.INVISIBLE) {
            textView.setText(R.string.ym_msg_listening)
            willStartMic = false
            voiceArea.visibility = View.VISIBLE
            startListeningWithoutDialog()
            micButton.setImageDrawable(
                AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.ic_back_button_ym
                )
            )
        } else {
            voiceArea.visibility = View.INVISIBLE
            micButton.setImageDrawable(
                AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.ic_mic_ym_small
                )
            )
            if (sr != null) {
                sr!!.stopListening()
            }
        }
    }

    fun closeVoiceArea() {

        if (context == null)
            return

        val voiceArea: RelativeLayout = parentLayout.findViewById<RelativeLayout>(R.id.voiceArea)
        val micButton: YmMovableFloatingActionButton =
            parentLayout.findViewById<YmMovableFloatingActionButton>(R.id.floatingActionButton)
        val textView: TextView = parentLayout.findViewById<TextView>(R.id.speechTranscription)
        voiceArea.visibility = View.INVISIBLE
        micButton.setImageDrawable(
            AppCompatResources.getDrawable(
                requireContext(),
                R.drawable.ic_mic_ym_small
            )
        )
        if (sr != null) {
            sr!!.stopListening()
            sr!!.destroy()
        }
    }

    inner class CustomRecognitionListener : RecognitionListener {
        var singleResult = true
        override fun onReadyForSpeech(params: Bundle) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            closeVoiceArea()
            val snackbar = Snackbar
                .make(
                    parentLayout,
                    "We've encountered an error. Please press Mic to continue with voice input.",
                    Snackbar.LENGTH_LONG
                )
            snackbar.show()
        }

        override fun onResults(results: Bundle) {
            val result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val textView: TextView = parentLayout.findViewById<TextView>(R.id.speechTranscription)
            textView.text = if (result != null && result.size > 0) result[0] else ""
            if (singleResult) {
                if (result != null && result.size > 0) {
                    if (sr != null) sr!!.cancel()
                    sendEvent(result[0])
                }
                closeVoiceArea()
                singleResult = false
            }
        }

        override fun onPartialResults(partialResults: Bundle) {
            val value =
                if (partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) != null
                    && partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!!.size > 0
                ) partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!![0] else ""
            val textView: TextView = parentLayout.findViewById<TextView>(R.id.speechTranscription)
            textView.text = value
        }

        override fun onEvent(eventType: Int, params: Bundle) {}
    }

    override fun onStop() {
        if (shouldKeepApplicationInBackground && (isAgentConnected || ConfigService.getInstance().config.alwaysReload)) {
            updateAgentStatus("offline")
        }
        super.onStop()
    }

    companion object {
        fun newInstance(): YellowBotWebviewFragment {
            return YellowBotWebviewFragment()
        }
    }
}