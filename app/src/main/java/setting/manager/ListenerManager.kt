package com.chat.safeplay.setting.manager

import android.util.Log
import com.google.firebase.firestore.ListenerRegistration

object ListenerManager {

    private val listeners = mutableListOf<ListenerRegistration>()

    fun add(reg: ListenerRegistration?) {
        reg ?: return
        synchronized(listeners) {
            listeners.add(reg)

            // 🔍 DEBUG LOG: see how many listeners are active
            Log.d("ListenerManager", "Added listener, total = ${listeners.size}")
        }
    }

    fun remove(reg: ListenerRegistration?) {
        reg ?: return
        synchronized(listeners) {
            listeners.remove(reg)
            try { reg.remove() } catch (_: Exception) {}
            Log.d("ListenerManager", "Removed listener, total = ${listeners.size}")
        }
    }

    fun removeAll() {
        synchronized(listeners) {
            for (r in listeners) {
                try { r.remove() } catch (_: Exception) {}
            }
            listeners.clear()

            // 🔍 DEBUG LOG: verify everything is cleared
            Log.d("ListenerManager", "All listeners removed, total = 0")
        }
    }
}
