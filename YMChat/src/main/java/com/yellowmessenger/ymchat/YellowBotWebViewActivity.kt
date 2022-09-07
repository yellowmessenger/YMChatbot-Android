package com.yellowmessenger.ymchat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction

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
        if (supportFragmentManager.backStackEntryCount == 1) {
            finish()
        } else {
            super.onBackPressed()
        }
    }
}