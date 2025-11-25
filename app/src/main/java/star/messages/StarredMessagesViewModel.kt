package com.chat.safeplay.star.messages

import android.util.Log
import androidx.lifecycle.ViewModel
import com.chat.safeplay.chat.handler.ChatUiMessage
import com.chat.safeplay.chat.handler.toChatUiMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StarredMessagesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _messages = MutableStateFlow<List<ChatUiMessage>>(emptyList())
    val messages: StateFlow<List<ChatUiMessage>> = _messages.asStateFlow()

    private var listener: ListenerRegistration? = null

    // myKey = publicId or uid (same as star key used in starredBy.{myKey})
    private var myKey: String? = null
    val starKey: String?
        get() = myKey

    // Keep track of the exact Firestore document for each starred message
    // Key = messageId (doc.id), Value = DocumentReference: chats/{convoId}/messages/{messageId}
    private val messageDocRefs = mutableMapOf<String, DocumentReference>()

    init {
        loadMyKeyAndStart()
    }

    private fun loadMyKeyAndStart() {
        val uid = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(uid)

        userRef.get()
            .addOnSuccessListener { doc ->
                val publicId = doc.getString("publicId")
                val key = if (!publicId.isNullOrBlank()) publicId else uid
                myKey = key
                startStarsListener(key)
            }
            .addOnFailureListener { e ->
                Log.w("StarredMessagesVM", "Failed to load my profile", e)
                // fallback: use uid only
                myKey = uid
                startStarsListener(uid)
            }
    }

    private fun startStarsListener(myKey: String) {
        listener?.remove()

        listener = db.collectionGroup("messages")
            .whereEqualTo("starredBy.$myKey", true)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snaps, err ->
                if (err != null) {
                    Log.w("StarredMessagesVM", "stars listener error", err)
                    return@addSnapshotListener
                }

                if (snaps != null) {
                    val refsMap = mutableMapOf<String, DocumentReference>()

                    val list = snaps.documents.mapNotNull { snap ->
                        val msg = snap.toChatUiMessage(myKey)
                        if (msg != null) {
                            // Store reference so we can unstar later
                            refsMap[msg.id] = snap.reference
                        }
                        msg
                    }

                    // Replace local maps atomically
                    messageDocRefs.clear()
                    messageDocRefs.putAll(refsMap)

                    _messages.value = list
                }
            }
    }

    /**
     * Unstar a batch of messages for the current user.
     * This removes the myKey entry from `starredBy.{myKey}` in Firestore.
     */
    fun unstarMessages(messageIds: List<String>) {
        val key = myKey ?: return
        if (messageIds.isEmpty()) return

        // 🔹 First, capture the refs BEFORE touching the map
        val refs: List<Pair<String, DocumentReference>> = messageIds.mapNotNull { id ->
            val ref = messageDocRefs[id]
            if (ref != null) id to ref else null
        }

        if (refs.isEmpty()) {
            Log.w("StarredMessagesVM", "No docRefs found for messageIds=$messageIds")
            return
        }

        // ✅ Optimistic UI: remove from local list immediately
        _messages.value = _messages.value.filterNot { it.id in messageIds }

        // Also clean local map (optional, keeps it in sync)
        refs.forEach { (id, _) ->
            messageDocRefs.remove(id)
        }

        // ✅ Firestore: actually remove `starredBy.{myKey}` on each message
        refs.forEach { (id, ref) ->
            ref.update("starredBy.$key", FieldValue.delete())
                .addOnSuccessListener {
                    Log.d("StarredMessagesVM", "Unstarred message $id")
                }
                .addOnFailureListener { e ->
                    Log.w("StarredMessagesVM", "Failed to unstar message $id", e)
                }
        }
    }



    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}



















//package com.chat.safeplay.star.messages
//
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import com.chat.safeplay.chat.handler.ChatUiMessage
//import com.chat.safeplay.chat.handler.toChatUiMessage
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.ListenerRegistration
//import com.google.firebase.firestore.Query
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//
//class StarredMessagesViewModel : ViewModel() {
//
//    private val db = FirebaseFirestore.getInstance()
//    private val auth = FirebaseAuth.getInstance()
//
//    private val _messages = MutableStateFlow<List<ChatUiMessage>>(emptyList())
//    val messages: StateFlow<List<ChatUiMessage>> = _messages.asStateFlow()
//
//    private var listener: ListenerRegistration? = null
//
//    private var myKey: String? = null // publicId or uid (same as star key)
//    val starKey: String?
//        get() = myKey
//
//
//    init {
//        loadMyKeyAndStart()
//    }
//
//    private fun loadMyKeyAndStart() {
//        val uid = auth.currentUser?.uid ?: return
//        val userRef = db.collection("users").document(uid)
//
//        userRef.get()
//            .addOnSuccessListener { doc ->
//                val publicId = doc.getString("publicId")
//                val key = if (!publicId.isNullOrBlank()) publicId else uid
//                myKey = key
//                startStarsListener(key)
//            }
//            .addOnFailureListener { e ->
//                Log.w("StarredMessagesVM", "Failed to load my profile", e)
//                // fallback: use uid only
//                myKey = uid
//                startStarsListener(uid)
//            }
//    }
//
//    private fun startStarsListener(myKey: String) {
//        listener?.remove()
//
//        listener = db.collectionGroup("messages")
//            .whereEqualTo("starredBy.$myKey", true)
//            .orderBy("timestamp", Query.Direction.DESCENDING)
//            .addSnapshotListener { snaps, err ->
//                if (err != null) {
//                    Log.w("StarredMessagesVM", "stars listener error", err)
//                    return@addSnapshotListener
//                }
//
//                if (snaps != null) {
//                    val list = snaps.documents.mapNotNull { snap ->
//                        snap.toChatUiMessage(myKey)
//                    }
//                    _messages.value = list
//                }
//            }
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        listener?.remove()
//    }
//}
