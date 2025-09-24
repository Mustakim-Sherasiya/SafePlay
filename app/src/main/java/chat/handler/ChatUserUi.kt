package com.chat.safeplay.chat.handler

data class ChatUserUi(
    val publicId: String,
    val name: String? = null,
    val showDisplayName: Boolean = false,
    val photoUrl: String? = null
)
