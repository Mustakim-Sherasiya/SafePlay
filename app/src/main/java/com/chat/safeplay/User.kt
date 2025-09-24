
package com.chat.safeplay.model
import com.chat.safeplay.model.User

data class User(
    val uid: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "user"
)
