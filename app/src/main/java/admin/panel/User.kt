package com.chat.safeplay.model

data class User(
    val uid: String = "",
    val email: String = "",
    val phone: String = "",
    val name: String = "",
    val publicId: String = "",
    val showDisplayName: Boolean = false,
    val photoUrl: String = "",
    val role: String = "user"
)
