package com.chat.safeplay.setting.manager

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chat.safeplay.setting.manager.ListenerManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class AccountDeletionState(
    val isDeleteRequested: Boolean = false,
    val remainingTime: String = "",
    val scheduledDeletionTime: String = "",
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

class AccountDeletionViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _state = MutableStateFlow(AccountDeletionState())
    val state = _state.asStateFlow()

    private val _deletionCompleted = MutableStateFlow(false)
    val deletionCompleted = _deletionCompleted.asStateFlow()

    private var countdownJob: Job? = null

    // keep a reference so we can remove this particular listener if needed
    private var userSnapshotRegistration: ListenerRegistration? = null

    init {
        checkDeletionStatus()
    }

    // -------------------------
    private fun checkDeletionStatus() {
        val uid = auth.currentUser?.uid ?: return

        // remove prior registration if any
        try {
            userSnapshotRegistration?.remove()
            userSnapshotRegistration = null
        } catch (_: Exception) { /* ignore */ }

        userSnapshotRegistration = firestore.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val deleteRequested = snapshot.getBoolean("deleteRequested") ?: false
                    val scheduledDeletion = snapshot.getLong("scheduledDeletion")

                    if (deleteRequested && scheduledDeletion != null) {
                        startCountdown(scheduledDeletion)
                    } else {
                        stopCountdown()
                        _state.value = AccountDeletionState(isDeleteRequested = false)
                    }
                }
            }.also { ListenerManager.add(it) }
    }

    // -------------------------
    // Start deletion process (small test timer here; set to 24h in production)
    fun startDeletionProcess() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val now = System.currentTimeMillis()
            // test short delay: 5 seconds. Change to now + 24*60*60*1000 for production.
            val scheduled = now + (5 * 1000)

            val data = mapOf<String, Any?>(
                "deleteRequested" to true,
                "deleteTimestamp" to now,
                "scheduledDeletion" to scheduled
            )

            try {
                firestore.collection("users").document(uid)
                    .update(data)
                    .await()

                startCountdown(scheduled)
                Log.d("AccountDeletion", "Scheduled deletion for $uid at $scheduled")
            } catch (e: Exception) {
                Log.e("AccountDeletion", "Failed to schedule deletion: ${e.message}", e)
                _state.value = state.value.copy(errorMessage = "Failed to schedule deletion.")
            }
        }
    }

    fun cancelDeletionProcess() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val updates = mapOf<String, Any?>(
                "deleteRequested" to false,
                "deleteTimestamp" to null,
                "scheduledDeletion" to null
            )
            try {
                firestore.collection("users").document(uid).update(updates).await()
                stopCountdown()
                _state.value = AccountDeletionState(isDeleteRequested = false)
                Log.d("AccountDeletion", "Deletion cancelled for $uid")
            } catch (e: Exception) {
                Log.e("AccountDeletion", "Error cancelling deletion: ${e.message}", e)
                _state.value = state.value.copy(errorMessage = "Failed to cancel deletion.")
            }
        }
    }

    // -------------------------
    private fun startCountdown(scheduledDeletion: Long) {
        stopCountdown()
        Log.d("AccountDeletion", "🟡 Countdown started for $scheduledDeletion")
        countdownJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val remainingMillis = scheduledDeletion - now

                if (remainingMillis <= 0) {
                    Log.d("AccountDeletion", "🟥 Countdown finished — starting deletion")
                    performFinalDeletion()
                    break
                }

                val hours = (remainingMillis / (1000 * 60 * 60))
                val minutes = (remainingMillis / (1000 * 60)) % 60
                val seconds = (remainingMillis / 1000) % 60

                val timeString = String.format("%02dh %02dm %02ds", hours, minutes, seconds)
                val formattedTime = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                    .format(Date(scheduledDeletion))

                _state.value = AccountDeletionState(
                    isDeleteRequested = true,
                    remainingTime = timeString,
                    scheduledDeletionTime = formattedTime
                )

                Log.d("AccountDeletion", "⏱ Remaining: $timeString until deletion")

                delay(1000)
            }
        }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    // paste this function into AccountDeletionViewModel (replace the existing performFinalDeletion)
    private fun performFinalDeletion() {
        viewModelScope.launch {
            // remove all listeners app-wide to avoid rejected-listen errors
            try {
                ListenerManager.removeAll()
            } catch (_: Exception) { /* ignore */ }

            // small pause so SDK detaches listeners
            delay(150)

            val uid = auth.currentUser?.uid ?: run {
                Log.e("AccountDeletion", "No authenticated user available for deletion")
                _state.value = state.value.copy(errorMessage = "No authenticated user")
                return@launch
            }
            Log.d("AccountDeletion", "🔥 performFinalDeletion() triggered for uid=$uid")

            val userRef = firestore.collection("users").document(uid)
            val backupRef = firestore.collection("admin_backups")
                .document("deleted_users")
                .collection("users")
                .document(uid)

            try {
                // 1) Backup user doc (best-effort)
                try {
                    val userData = userRef.get().await()
                    if (userData.exists()) {
                        val userDataMap = userData.data as? Map<String, Any> ?: emptyMap<String, Any>()
                        backupRef.set(userDataMap).await()
                        Log.d("AccountDeletion", "📦 User data backed up for $uid")
                    } else {
                        Log.d("AccountDeletion", "📦 No user doc to backup for $uid")
                    }
                } catch (e: Exception) {
                    Log.w("AccountDeletion", "⚠️ Backup user doc failed: ${e.message}", e)
                }

                // 2) Delete messages authored by the user — safer route: iterate recentChats
                try {
                    val recentRef = firestore.collection("users").document(uid).collection("recentChats")
                    val recentSnap = recentRef.get().await()
                    Log.d("AccountDeletion", "🔎 Found ${recentSnap.documents.size} recentChats to inspect for user messages")

                    for (rc in recentSnap.documents) {
                        val convoId = rc.getString("chatId") ?: rc.id
                        Log.d("AccountDeletion", "Inspecting convoId = $convoId (from recentChats doc ${rc.id})")

                        // Attempt to read messages for this convo — catch permission/read errors per-convo
                        val messagesSnap = try {
                            firestore.collection("chats").document(convoId).collection("messages").get().await()
                        } catch (e: Exception) {
                            Log.e("AccountDeletion", "❌ Permission or read error when fetching messages for convo $convoId: ${e.message}", e)
                            if (e is com.google.firebase.firestore.FirebaseFirestoreException &&
                                e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                _state.value = state.value.copy(errorMessage = "Permission denied reading messages for $convoId")
                                return@launch
                            } else {
                                // other read error — continue to next convo
                                continue
                            }
                        }

                        for (msg in messagesSnap.documents) {
                            val senderId = msg.getString("senderId")
                            if (senderId == uid) {
                                try {
                                    backupRef.collection("chats").add(msg.data ?: emptyMap<String, Any>()).await()
                                    msg.reference.delete().await()
                                    Log.d("AccountDeletion", "🗑️ Deleted message ${msg.id} in chat $convoId")
                                } catch (innerEx: Exception) {
                                    Log.e("AccountDeletion", "❌ Could not delete message ${msg.id} in $convoId: ${innerEx.message}", innerEx)
                                    if (innerEx is com.google.firebase.firestore.FirebaseFirestoreException &&
                                        innerEx.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                        _state.value = state.value.copy(errorMessage = "Permission denied deleting message ${msg.id} in $convoId")
                                        return@launch
                                    }
                                    // otherwise continue with next message
                                }
                            }
                        }
                    }

                    Log.d("AccountDeletion", "💬 Client-side: messages deletion (via recentChats) attempts finished")
                } catch (e: Exception) {
                    Log.e("AccountDeletion", "❌ Error reading recentChats for user $uid: ${e.message}", e)
                    _state.value = state.value.copy(errorMessage = "Error reading recent chats.")
                    return@launch
                }

                // 3) Delete recentChats subcollection
                try {
                    val recentChatsRef = firestore.collection("users").document(uid).collection("recentChats")
                    val recentChats = recentChatsRef.get().await()
                    for (chatDoc in recentChats.documents) {
                        try {
                            chatDoc.reference.delete().await()
                            Log.d("AccountDeletion", "🗑️ Deleted recent chat: ${chatDoc.id}")
                        } catch (innerEx: Exception) {
                            Log.w("AccountDeletion", "⚠️ Could not delete recent chat ${chatDoc.id}: ${innerEx.message}", innerEx)
                            if (innerEx is com.google.firebase.firestore.FirebaseFirestoreException &&
                                innerEx.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                _state.value = state.value.copy(errorMessage = "Permission denied deleting recentChats.")
                                return@launch
                            }
                        }
                    }
                    Log.d("AccountDeletion", "✅ recentChats deletion attempts finished for $uid")
                } catch (e: Exception) {
                    Log.w("AccountDeletion", "⚠️ Error fetching recentChats: ${e.message}", e)
                }

                // 4) Delete storage files (profile + backgrounds) - best-effort client-side
                try {
                    val profileRef = storage.reference.child("profile_photos/$uid")
                    val backgroundRef = storage.reference.child("chatBackgrounds/$uid")

                    suspend fun deleteFolderRecursively(folderRef: com.google.firebase.storage.StorageReference) {
                        val listResult = folderRef.listAll().await()
                        for (item in listResult.items) {
                            try {
                                item.delete().await()
                                Log.d("AccountDeletion", "🧹 Deleted file: ${item.path}")
                            } catch (innerEx: Exception) {
                                Log.w("AccountDeletion", "⚠️ Could not delete file ${item.path}: ${innerEx.message}")
                            }
                        }
                        for (sub in listResult.prefixes) {
                            deleteFolderRecursively(sub)
                        }
                    }

                    try {
                        deleteFolderRecursively(profileRef)
                        deleteFolderRecursively(backgroundRef)
                        Log.d("AccountDeletion", "🧹 Storage deletion attempts finished")
                    } catch (sEx: Exception) {
                        Log.w("AccountDeletion", "⚠️ Storage deletion issue: ${sEx.message}", sEx)
                        _state.value = state.value.copy(errorMessage = "Storage deletion failed.")
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.w("AccountDeletion", "⚠️ Error initializing storage deletion: ${e.message}", e)
                }

                // 5) Delete user doc
                try {
                    userRef.delete().await()
                    Log.d("AccountDeletion", "🗑️ User document deleted from Firestore: $uid")
                } catch (e: Exception) {
                    Log.e("AccountDeletion", "❌ Failed deleting user document: ${e.message}", e)
                    if (e is com.google.firebase.firestore.FirebaseFirestoreException &&
                        e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        _state.value = state.value.copy(errorMessage = "Permission denied deleting user doc.")
                        return@launch
                    }
                }

                // 6) Try client-side Auth deletion; if fails with recent-login, surface message
                try {
                    auth.currentUser?.delete()?.await()
                    Log.d("AccountDeletion", "✅ Auth user deleted client-side for $uid")
                    _deletionCompleted.value = true
                } catch (e: Exception) {
                    Log.w("AccountDeletion", "⚠️ Auth deletion failed client-side: ${e.message}", e)
                    _state.value = state.value.copy(errorMessage = "Auth deletion failed: login required to delete account.")
                    // do not attempt cloud fallback in simple mode
                    return@launch
                }

            } catch (outer: Exception) {
                Log.e("AccountDeletion", "❌ Unexpected error in deletion flow: ${outer.message}", outer)
                _state.value = state.value.copy(errorMessage = "Unexpected deletion error.")
            }
        }
    }

}








//package com.chat.safeplay.setting.manager
//
//import android.content.Context
//import android.util.Log
//import android.widget.Toast
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.chat.safeplay.setting.manager.ListenerManager.ListenerManager
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//import java.text.SimpleDateFormat
//import java.util.*
//import com.google.firebase.storage.FirebaseStorage
//import com.google.firebase.firestore.ListenerRegistration
//
//
//
//data class AccountDeletionState(
//    val isDeleteRequested: Boolean = false,
//    val remainingTime: String = "",
//    val scheduledDeletionTime: String = "",
//    val isProcessing: Boolean = false
//)
//
//class AccountDeletionViewModel : ViewModel() {
//
//    private val firestore = FirebaseFirestore.getInstance()
//    private val auth = FirebaseAuth.getInstance()
//
//    private val _state = MutableStateFlow(AccountDeletionState())
//    val state = _state.asStateFlow()
//
//    private val _deletionCompleted = MutableStateFlow(false)
//    val deletionCompleted = _deletionCompleted.asStateFlow()
//
//    private var countdownJob: Job? = null
//    // keep a reference so we can remove this particular listener if needed
//    private var userSnapshotRegistration: ListenerRegistration? = null
//
//
//    init {
//        checkDeletionStatus()
//    }
//
//
//    // 🔹 Check Firestore for deletion status
//
//
//    private fun checkDeletionStatus() {
//        val uid = auth.currentUser?.uid ?: return
//
//        // If there was a previous registration, remove it first
//        try {
//            userSnapshotRegistration?.remove()
//            userSnapshotRegistration = null
//        } catch (_: Exception) { /* ignore */ }
//
//        // attach and store the registration, and add it to the global manager
//        userSnapshotRegistration = firestore.collection("users")
//            .document(uid)
//            .addSnapshotListener { snapshot, e ->
//                if (e != null) return@addSnapshotListener
//                if (snapshot != null && snapshot.exists()) {
//                    val deleteRequested = snapshot.getBoolean("deleteRequested") ?: false
//                    val scheduledDeletion = snapshot.getLong("scheduledDeletion")
//
//                    if (deleteRequested && scheduledDeletion != null) {
//                        // ✅ start countdown automatically when Firestore shows deletion scheduled
//                        startCountdown(scheduledDeletion)
//                    } else {
//                        stopCountdown()
//                        _state.value = AccountDeletionState(isDeleteRequested = false)
//                    }
//                }
//            }
//            .also { ListenerManager.add(it) }
//    }
//
//
//
////    private fun checkDeletionStatus() {
////        val uid = auth.currentUser?.uid ?: return
////        firestore.collection("users").document(uid)
////            .addSnapshotListener { snapshot, e ->
////                if (e != null) return@addSnapshotListener
////                if (snapshot != null && snapshot.exists()) {
////                    val deleteRequested = snapshot.getBoolean("deleteRequested") ?: false
////                    val scheduledDeletion = snapshot.getLong("scheduledDeletion")
////
////                    if (deleteRequested && scheduledDeletion != null) {
////                        // ✅ start countdown automatically when Firestore shows deletion scheduled
////                        startCountdown(scheduledDeletion)
////                    } else {
////                        stopCountdown()
////                        _state.value = AccountDeletionState(isDeleteRequested = false)
////                    }
////                }
////            }
////    }
//
//
//    // 🔹 Start deletion process (1 minute test timer)
//    fun startDeletionProcess(context: Context) {
//        viewModelScope.launch {
//            val uid = auth.currentUser?.uid ?: return@launch
//            val now = System.currentTimeMillis()
//
//            //   val scheduled = now + (24 * 60 * 60 * 1000) // 24 hours later
//
//            val scheduled = now + (1 * 5 * 1000) // 🧪 1 minute for testing
//
//            val data = mapOf(
//                "deleteRequested" to true,
//                "deleteTimestamp" to now,
//                "scheduledDeletion" to scheduled
//            )
//
//            firestore.collection("users").document(uid)
//                .update(data)
//                .addOnSuccessListener {
//                    Toast.makeText(
//                        context,
//                        "Account deletion scheduled (1 min).",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    startCountdown(scheduled)
//
//                }
//                .addOnFailureListener {
//                    Toast.makeText(context, "Error scheduling deletion.", Toast.LENGTH_SHORT).show()
//                }
//        }
//    }
//
//    // 🔹 Cancel deletion process
//    fun cancelDeletionProcess(context: Context) {
//        viewModelScope.launch {
//            val uid = auth.currentUser?.uid ?: return@launch
//            val updates = mapOf(
//                "deleteRequested" to false,
//                "deleteTimestamp" to null,
//                "scheduledDeletion" to null
//            )
//            firestore.collection("users").document(uid).update(updates)
//                .addOnSuccessListener {
//                    stopCountdown()
//                    Toast.makeText(context, "Account deletion cancelled.", Toast.LENGTH_SHORT)
//                        .show()
//                }
//                .addOnFailureListener {
//                    Toast.makeText(context, "Error cancelling deletion.", Toast.LENGTH_SHORT).show()
//                }
//        }
//    }
//
//    // 🔹 Countdown timer
//    private fun startCountdown(scheduledDeletion: Long) {
//        stopCountdown()
//        Log.d("AccountDeletion", "🟡 Countdown started for $scheduledDeletion")
//        countdownJob = viewModelScope.launch {
//            while (true) {
//                val now = System.currentTimeMillis()
//                val remainingMillis = scheduledDeletion - now
//
//                if (remainingMillis <= 0) {
//                    Log.d("AccountDeletion", "🟥 Countdown finished — starting deletion")
//                    performFinalDeletion()
//                    break
//                }
//
//                val hours = (remainingMillis / (1000 * 60 * 60))
//                val minutes = (remainingMillis / (1000 * 60)) % 60
//                val seconds = (remainingMillis / 1000) % 60
//
//                val timeString = String.format("%02dh %02dm %02ds", hours, minutes, seconds)
//                val formattedTime = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
//                    .format(Date(scheduledDeletion))
//
//                _state.value = AccountDeletionState(
//                    isDeleteRequested = true,
//                    remainingTime = timeString,
//                    scheduledDeletionTime = formattedTime
//                )
//
//                // 🧠 Log each tick (once per second)
//                Log.d("AccountDeletion", "⏱ Remaining: $timeString until deletion")
//
//                delay(1000)
//            }
//        }
//    }
//
//
//
//    private fun stopCountdown() {
//        countdownJob?.cancel()
//        countdownJob = null
//    }
//
//    /**
//     * 🔥 Performs final deletion:
//     * 1. Backup user data and chats.
//     * 2. Delete chats and user document.
//     * 3. Delete Firebase Auth account.
//     * 4. Clear local session and notify UI.
//     */
//
//
//    
//
////    private fun performFinalDeletion() {
////        viewModelScope.launch {
////            val uid = auth.currentUser?.uid ?: return@launch
////            Log.d("AccountDeletion", "🔥 performFinalDeletion() triggered for uid=$uid")
////
////            val userRef = firestore.collection("users").document(uid)
////            val backupRef = firestore.collection("admin_backups")
////                .document("deleted_users")
////                .collection("users")
////                .document(uid)
////
////            try {
////                val userData = userRef.get().await()
////                if (userData.exists()) {
////                    val userDataMap = userData.data as? Map<String, Any> ?: emptyMap()
////                    backupRef.set(userDataMap).await()
////                    Log.d("AccountDeletion", "📦 User data backed up for $uid")
////                }
////
////                // ✅ Backup + delete chats
////                val chatQuery = firestore.collection("chats")
////                    .whereArrayContains("participants", uid)
////                    .get()
////                    .await()
////
////                for (chatDoc in chatQuery.documents) {
////                    val messages = chatDoc.reference.collection("messages").get().await()
////                    for (msg in messages.documents) {
////                        val data = msg.data ?: continue
////                        if ((data["senderId"] == uid) || (data["receiverId"] == uid)) {
////                            backupRef.collection("chats").add(data).await()
////                            msg.reference.delete().await()
////                        }
////                    }
////                }
////                Log.d("AccountDeletion", "💬 Chats deleted and User backed up for $uid")
////
////// ✅ Delete user's recentChats subcollection
////                try {
////                    val recentChatsRef = firestore.collection("users").document(uid).collection("recentChats")
////                    val recentChats = recentChatsRef.get().await()
////                    for (chatDoc in recentChats.documents) {
////                        chatDoc.reference.delete().await()
////                        Log.d("AccountDeletion", "🗑️ Deleted recent chat: ${chatDoc.id}")
////                    }
////                    Log.d("AccountDeletion", "✅ All recentChats deleted for $uid")
////                } catch (e: Exception) {
////                    Log.w("AccountDeletion", "⚠️ Error deleting recentChats: ${e.message}")
////                }
////
////// ✅ Continue to delete Storage files next...
////
////                // ✅ Delete user’s Storage files (profile photos + chat backgrounds)
////                try {
////                    val storage = FirebaseStorage.getInstance()
////                    val uid = auth.currentUser?.uid ?: return@launch
////
////                    val profileRef = storage.reference.child("profile_photos/$uid")
////                    val backgroundRef = storage.reference.child("chatBackgrounds/$uid")
////
////                    // Helper function to recursively delete all files in a folder
////                    suspend fun deleteFolderRecursively(folderRef: com.google.firebase.storage.StorageReference) {
////                        try {
////                            val listResult = folderRef.listAll().await()
////                            for (item in listResult.items) {
////                                item.delete().await()
////                                Log.d("AccountDeletion", "🧹 Deleted file: ${item.path}")
////                            }
////                            for (subfolder in listResult.prefixes) {
////                                deleteFolderRecursively(subfolder)
////                            }
////                            Log.d("AccountDeletion", "✅ Folder deleted: ${folderRef.path}")
////                        } catch (e: Exception) {
////                            Log.w("AccountDeletion", "⚠️ Could not delete folder ${folderRef.path}: ${e.message}")
////                        }
////                    }
////
////                    deleteFolderRecursively(profileRef)
////                    deleteFolderRecursively(backgroundRef)
////
////                } catch (e: Exception) {
////                    Log.w("AccountDeletion", "⚠️ Error deleting Storage files: ${e.message}")
////                }
////
////
////
////                userRef.delete().await()
////                auth.currentUser?.delete()?.await()
////
////                Log.d("AccountDeletion", "✅ User $uid deleted from Firestore & Auth")
////
////                _deletionCompleted.value = true
////
////            } catch (e: Exception) {
////                Log.e("AccountDeletion", "❌ Error deleting user: ${e.message}", e)
////            }
////        }
////    }
//}
