package com.chat.safeplay.chat.handler

object ChatRoutes {
    const val CHAT_WITH = "chat/{publicId}"
    fun chatWith(publicId: String) = "chat/$publicId"
}
