package com.chat.safeplay.chat.handler


object ChatRoutes {
    // base route used in NavHost
    const val CHAT_WITH = "chat/{publicId}"

    // function to build a route string, optional focusId as query param
    fun chatWith(publicId: String, focusId: String? = null): String {
        return if (focusId.isNullOrBlank()) {
            "chat/$publicId"
        } else {
            "chat/$publicId?focusId=$focusId"
        }
    }
}
