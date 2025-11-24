package com.chat.safeplay

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Log

object LocalStorage {

    /**
     * ✅ Create secure, stable preferences instance.
     * Always reuses the same MasterKey to prevent "decryption failed" errors.
     */
    private fun prefs(context: Context): SharedPreferences {
        return try {
            val masterKeyAlias = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "safeplay_prefs", // encrypted file name
                masterKeyAlias,   // reuse existing key
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // 🚨 Catch and reset corrupted prefs if key decryption fails
            Log.e("LocalStorage", "Error initializing prefs: ${e.message}")
            context.deleteSharedPreferences("safeplay_prefs")
            val masterKeyAlias = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "safeplay_prefs",
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    /**
     * Save login info (email + password) securely
     */
    fun saveLogin(context: Context, email: String, password: String) {
        prefs(context).edit().apply {
            putString("email", email)
            putString("password", password)
            putBoolean("isLoggedIn", true)
            apply()
        }
        Log.d("LocalStorage", "Login data saved for $email")
    }

    /**
     * Retrieve saved email and password for silent re-login
     */
    fun getEmail(context: Context): String? =
        prefs(context).getString("email", null)

    fun getPassword(context: Context): String? =
        prefs(context).getString("password", null)

    /**
     * Check login state
     */
    fun isLoggedIn(context: Context): Boolean =
        prefs(context).getBoolean("isLoggedIn", false)

    /**
     * Clear everything on logout safely
     */
    fun clear(context: Context) {
        try {
            prefs(context).edit().clear().apply()
            Log.d("LocalStorage", "Cleared all saved data")
        } catch (e: Exception) {
            Log.e("LocalStorage", "Error clearing prefs: ${e.message}")
            context.deleteSharedPreferences("safeplay_prefs") // fallback
        }
    }
}
