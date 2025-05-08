package com.yellowmessenger.ymchat

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentTransaction
import com.yellowmessenger.ymchat.models.ConfigService
import com.yellowmessenger.ymchat.models.YMBotEventResponse

class YellowBotWebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yellow_bot_web_view)
        loadFragment()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val rootView = findViewById<View>(R.id.container)
            val customStatusBar = findViewById<View>(R.id.customStatusBar)
            customStatusBar.visibility = View.VISIBLE
            val statusBarColor = ConfigService.getInstance().config.statusBarColor
            if (statusBarColor != -1) {
                customStatusBar.setBackgroundColor(statusBarColor)
            }
            try {
                val statusBarColorFromHex = ConfigService.getInstance().config.statusBarColorFromHex
                if (statusBarColorFromHex != null && statusBarColorFromHex.isNotEmpty()) {
                    customStatusBar.setBackgroundColor(Color.parseColor(statusBarColorFromHex))
                }
            } catch (e: java.lang.Exception) {
                //Exception occurred
            }
            rootView?.setOnApplyWindowInsetsListener { _, insets ->
                val statusBarInsets = insets.getInsets(WindowInsets.Type.statusBars())
                val displayCutoutInsets = insets.getInsets(WindowInsets.Type.displayCutout())

                val layoutParams = customStatusBar.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.height = statusBarInsets.top
                layoutParams.setMargins(displayCutoutInsets.left, 0, displayCutoutInsets.right, 0)
                customStatusBar.layoutParams = layoutParams
                customStatusBar.requestLayout()

                insets
            }
        }
    }

    private fun loadFragment() {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction();
        transaction.replace(R.id.container, YellowBotWebviewFragment.newInstance())
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onBackPressed() {
        try {
            if (supportFragmentManager.backStackEntryCount == 1) {
                YMChat.getInstance().emitEvent(YMBotEventResponse("bot-closed", "", false))
                finish()
            } else {
                super.onBackPressed()
            }
        } catch (e: Exception) {
            //Some problem occurred please relaunch the bot
            finish()

        }
    }
}