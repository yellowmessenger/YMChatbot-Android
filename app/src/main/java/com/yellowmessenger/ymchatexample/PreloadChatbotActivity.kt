package com.yellowmessenger.ymchatexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yellowmessenger.ymchat.YMChat
import com.yellowmessenger.ymchat.YMConfig
import com.yellowmessenger.ymchat.models.YMBotEventResponse

/*
* This activity load the fragments as soon as onCreate method is called but visibility of fragment
* is kept GONE.
* Visibility of fragment can be controlled from FAB.
* */

class PreloadChatbotActivity : AppCompatActivity() {
    private val botId = "x1645602443989"
    private val chatBotViewTag = "yellowChatBot"
    private lateinit var frame:FrameLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preload_chatbot)
        frame = findViewById(R.id.frag_preload)
        val fab:FloatingActionButton = findViewById(R.id.fab_action)

        fab.setOnClickListener {
            val visibility = frame.visibility
            if(visibility == View.VISIBLE){
                frame.visibility = View.GONE
            } else {
                frame.visibility = View.VISIBLE
            }
        }
        // Initialise the Chatbot
        val chat = initialiseBot()

        // Preload the fragment in but keep it hidden
        loadFragment(chat.getChatBotView(this),chatBotViewTag)

    }

    private fun initialiseBot(): YMChat {
        //Get YMChat instance
        val ymChat = YMChat.getInstance()
        ymChat.config = YMConfig(botId)

        //To enable speach to text
        ymChat.config.enableSpeech = true

        //Payload attributes
        val payloadData = HashMap<String, Any>()

        //Setting Payload Data
        payloadData["some-key"] = "some-value"
        ymChat.config.payload = payloadData

        // Choose version(1 or 2), default is 1
        ymChat.config.version = 2


        // To Change the color of status bar, by default it will pick app theme
        ymChat.config.statusBarColor = R.color.colorPrimaryDark

        // To Change the color of close button, default color is white
        ymChat.config.closeButtonColor = R.color.white

        /* Note: if color is set from both setStatusBarColor and statusBarColorFromHex,
         * statusBarColorFromHex will take priority
         * */
        // To set statusBarColor from hexadecimal color code
        ymChat.config.statusBarColorFromHex = "#49c656"

        /* Note: if color is set from both closeButtonColor and closeButtonColorHex,
         * closeButtonColorHex will take priority
         * */
        // To set closeButtonColor from hexadecimal color code
        ymChat.config.closeButtonColorFromHex = "#b72a2a"

        // Set custom loader url , it should be a valid, light weight and public image url
        // This is an optional parameter
        // ymChat.config.customLoaderUrl = "https://yellow.ai/images/Logo.svg";

        // Hide input bar and disallow action while bot is loading
        ymChat.config.disableActionsOnLoad = true

        //Set custom base url like follows in case of On-Prem environment and multi-region
        // ymChat.config.customBaseUrl = "https:/rx.cloud.yellow.ai";

        //setting event listener
        ymChat.onEventFromBot { botEvent: YMBotEventResponse ->
            when (botEvent.code) {
                "event-name" -> {}
            }
        }
        ymChat.onBotClose {
            Log.d("Example App", "Bot Was closed")
            //removeFragment()
            onBackPressed()
        }

        return ymChat
    }

    private fun removeFragment(tag: String) {
        val frag = supportFragmentManager.findFragmentByTag(tag)
        if (frag != null) {
            supportFragmentManager.beginTransaction().remove(frag)
        }
    }

    @Throws(Exception::class)
    private fun loadFragment(frag: Fragment, tag: String) {
        val transaction = supportFragmentManager.beginTransaction()
        //frag_container is your layout name in xml file
        transaction.replace(R.id.frag_preload, frag, tag)
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