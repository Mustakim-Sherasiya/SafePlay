
package com.chat.safeplay.setting.manager

import android.content.Context
import android.net.Uri

private const val PREFS_NAME = "chat_background"
private const val KEY_URI = "uri"

fun saveBackgroundUri(context: Context, uri: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_URI, uri)
        .apply()
}

fun getSavedBackgroundUri(context: Context): Uri? {
    val uri = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_URI, null)
    return uri?.let { Uri.parse(it) }
}

fun clearSavedBackground(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(KEY_URI)
        .apply()
}
