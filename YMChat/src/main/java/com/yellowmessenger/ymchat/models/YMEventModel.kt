package com.yellowmessenger.ymchat.models

public data class YMEventModel(
    val code:String,
    val data:Map<String,Any?> = mutableMapOf()
)
