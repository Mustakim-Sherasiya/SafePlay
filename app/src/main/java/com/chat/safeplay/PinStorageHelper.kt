

package com.chat.safeplay

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.nio.charset.Charset

object PinStorageHelper {
    private const val PREFS_NAME = "safeplay_prefs"
    private const val KEY_PIN = "user_pin"
    private const val KEY_PIN_LENGTH = "user_pin_length"
    private const val KEY_AUTO_SUBMIT = "user_pin_auto_submit"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Simple encode/decode (not secure, replace with real encryption)
    private fun encodePin(pin: String): String =
        Base64.encodeToString(pin.toByteArray(Charset.forName("UTF-8")), Base64.DEFAULT)

    private fun decodePin(encoded: String): String =
        String(Base64.decode(encoded, Base64.DEFAULT), Charset.forName("UTF-8"))

    fun savePin(context: Context, pin: String, pinLength: Int, autoSubmit: Boolean) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putString(KEY_PIN, encodePin(pin))
            .putInt(KEY_PIN_LENGTH, pinLength)
            .putBoolean(KEY_AUTO_SUBMIT, autoSubmit)
            .apply()
    }

    fun getPin(context: Context): String? {
        val prefs = getPrefs(context)
        val encoded = prefs.getString(KEY_PIN, null) ?: return null
        return decodePin(encoded).trim()
    }

    fun getPinLength(context: Context): Int {
        val prefs = getPrefs(context)
        return prefs.getInt(KEY_PIN_LENGTH, 4)  // default 4
    }

    fun getAutoSubmit(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_AUTO_SUBMIT, true)  // default true
    }

    fun clearPin(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().remove(KEY_PIN).remove(KEY_PIN_LENGTH).remove(KEY_AUTO_SUBMIT).apply()
    }
}
