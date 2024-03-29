package com.yellowmessenger.ymchat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.yellowmessenger.ymchat.models.YMBotEventResponse

class YellowBotWebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yellow_bot_web_view)
        loadFragment()
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