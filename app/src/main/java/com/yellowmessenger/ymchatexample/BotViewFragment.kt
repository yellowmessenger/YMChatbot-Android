package com.yellowmessenger.ymchatexample

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button


class BotViewFragment : Fragment() {

    private lateinit var button:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_preload_view, container, false)
        button = v.findViewById(R.id.showViewButton)
        button.setOnClickListener {
            (activity as BotViewActivity).showBotView()
        }
        return v
    }

    companion object {
        @JvmStatic
        fun newInstance() = BotViewFragment()

    }
}