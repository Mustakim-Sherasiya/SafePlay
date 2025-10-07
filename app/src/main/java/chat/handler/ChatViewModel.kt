package com.chat.safeplay.chat.handler

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.util.copy
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.math.pow
import java.util.Date
import kotlin.collections.toMutableMap


/**
 * Feature-rich ChatViewModel:
 * - sending (with profanity filter + optimistic UI + retries)
 * - edit/delete messages
 * - reactions (emoji)
 * - delivered/read marking
 * - typing presence (chats/{convoId}/presence)
 * - pagination (load earlier messages)
 * - robust convoId selection (uid-based preferred, publicId fallback)
 *
 * Note: UI should call markMessagesReadUpTo(...) when messages are visible to mark read.
 */
class ChatViewModel(private val otherPublicId: String) : ViewModel() {

    private val TAG = "ChatViewModel"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val mainHandler = Handler(Looper.getMainLooper())

    // state flows
    private val _messages = MutableStateFlow<List<ChatUiMessage>>(emptyList())
    val messages: StateFlow<List<ChatUiMessage>> = _messages.asStateFlow()

    private val _otherUser = MutableStateFlow(ChatUserUi(publicId = otherPublicId))
    val otherUser: StateFlow<ChatUserUi> = _otherUser.asStateFlow()

    // typing presence map (uid -> boolean)
    private val _typingState = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typingState: StateFlow<Map<String, Boolean>> = _typingState.asStateFlow()

    // ------------------ SELECTION (UI-only) ------------------
    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    fun toggleSelect(messageId: String) {
        val set = _selected.value.toMutableSet()
        if (set.contains(messageId)) set.remove(messageId) else set.add(messageId)
        _selected.value = set
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    /**
     * Delete all selected messages (hard delete). Clears selection afterwards.
     */
    fun deleteSelected() {
        val convoId = uidConvoId() ?: publicIdConvoId()
        if (convoId.isBlank()) {
            clearSelection()
            return
        }
        val sel = _selected.value.toList()
        sel.forEach { id ->
            db.collection("chats").document(convoId).collection("messages").document(id).delete()
        }
        clearSelection()
    }

    /**
     * Toggle star for all selected messages (per-user).
     */
    fun toggleStarSelected() {
        val convoId = uidConvoId() ?: publicIdConvoId()
        if (convoId.isBlank() || myUid.isBlank()) {
            clearSelection()
            return
        }
        val sel = _selected.value.toList()
        sel.forEach { messageId ->
            val docRef = db.collection("chats").document(convoId).collection("messages").document(messageId)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val currentMap = (snap.get("starredBy") as? Map<String, Boolean>)?.toMutableMap() ?: mutableMapOf()
                val currently = currentMap[myUid] == true
                if (currently) currentMap.remove(myUid) else currentMap[myUid] = true
                tx.update(docRef, "starredBy", currentMap)
            }
        }
        clearSelection()
    }











    // my identifiers
    var myUid: String = auth.currentUser?.uid ?: ""
        private set
    var myPublicId: String = ""
        private set
    var myPhotoUrl: String? = auth.currentUser?.photoUrl?.toString()
        private set

    // other user's uid (resolved from /users doc)
    private var otherUid: String? = null

    // paging state
    private var pageSize = 25
    private var lastLoadedSnapshot: DocumentSnapshot? = null
    private var isLoadingEarlier = false
    private var reachedStart = false

    // listeners
    private var messagesListener: ListenerRegistration? = null
    private var fallbackListener: ListenerRegistration? = null
    private var presenceListener: ListenerRegistration? = null
    private var otherUserListener: ListenerRegistration? = null
    private var myProfileListener: ListenerRegistration? = null


    // 🔹 Delay chat settings
    private var delayChatEnabled: Boolean = false
    private var delayChatSeconds: Int = 1
    private var delaySettingsListener: ListenerRegistration? = null

    private val _pendingMessages = MutableStateFlow<Map<String, PendingMessage>>(emptyMap())
    val pendingMessages = _pendingMessages.asStateFlow()

    // pending local send state used for retries (messageLocalId -> retryCount)
    private val pendingRetries = ConcurrentHashMap<String, Int>()

    // profanity blacklist (simple example; extend as needed)
    private val profanityList = listOf("badword1", "badword2") // replace with real words

    init {
        Log.d(TAG, "Init ChatViewModel: myUid=$myUid otherPublicId=$otherPublicId")
        observeOtherUser()
        observeMyProfile()
        observeDelaySettings()

    }

    // ------------------ PROFILE & OTHER USER ------------------

    private fun observeOtherUser() {
        val otherDocRef = db.collection("users").document(otherPublicId)
        otherUserListener = otherDocRef.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "otherUserListener error", err)
                return@addSnapshotListener
            }
            if (snap != null && snap.exists()) {
                val name = snap.getString("name")
                val show = snap.getBoolean("showDisplayName") ?: false
                val photo = snap.getString("photoUrl")
                _otherUser.value = ChatUserUi(publicId = otherPublicId, name = name, showDisplayName = show, photoUrl = photo)

                // resolve otherUid (doc id may be uid or publicId; try field "uid" too)
                otherUid = when {
                    snap.id.isNotBlank() && snap.id.length >= 6 && snap.id != otherPublicId -> snap.id
                    snap.getString("uid") != null -> snap.getString("uid")
                    else -> null
                } ?: snap.getString("uid")

                // start listeners or refresh
                startPresenceListener()
                startListeningMessagesIfReady()
            } else {
                // fallback query by publicId field
                db.collection("users").whereEqualTo("publicId", otherPublicId).limit(1).get()
                    .addOnSuccessListener { docs ->
                        if (!docs.isEmpty) {
                            val doc = docs.documents[0]
                            val name = doc.getString("name")
                            val show = doc.getBoolean("showDisplayName") ?: false
                            val photo = doc.getString("photoUrl")
                            _otherUser.value = ChatUserUi(publicId = otherPublicId, name = name, showDisplayName = show, photoUrl = photo)
                            otherUid = if (!doc.id.isNullOrBlank()) doc.id else doc.getString("uid")
                            startPresenceListener()
                            startListeningMessagesIfReady()
                        } else {
                            _otherUser.value = ChatUserUi(publicId = otherPublicId)
                            startListeningMessagesIfReady()
                        }
                    }
                    .addOnFailureListener { e -> Log.w(TAG, "otherUser fallback query failed", e) }
            }
        }
    }

    private fun observeMyProfile() {
        if (myUid.isBlank()) {
            Log.w(TAG, "myUid blank; profile observe skipped")
            // still attempt to start listeners (publicId fallback)
            startListeningMessagesIfReady()
            return
        }
        val myDocRef = db.collection("users").document(myUid)
        myProfileListener = myDocRef.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "myProfileListener error", err)
                return@addSnapshotListener
            }
            if (snap != null && snap.exists()) {
                val newPublicId = snap.getString("publicId") ?: ""
                val newPhoto = snap.getString("photoUrl") ?: auth.currentUser?.photoUrl?.toString()
                val needRestart = myPublicId != newPublicId
                myPublicId = newPublicId
                myPhotoUrl = newPhoto
                Log.d(TAG, "Loaded my profile publicId=$myPublicId")
                if (needRestart) startListeningMessagesIfReady() else startListeningMessagesIfReady()
            } else {
                // fallback queries
                db.collection("users").whereEqualTo("uid", myUid).limit(1).get()
                    .addOnSuccessListener { docs ->
                        if (!docs.isEmpty) {
                            val doc = docs.documents[0]
                            myPublicId = doc.getString("publicId") ?: ""
                            myPhotoUrl = doc.getString("photoUrl") ?: auth.currentUser?.photoUrl?.toString()
                            startListeningMessagesIfReady()
                        } else {
                            db.collection("users").whereEqualTo("publicId", myUid).limit(1).get()
                                .addOnSuccessListener { results ->
                                    if (!results.isEmpty) {
                                        val doc = results.documents[0]
                                        myPublicId = doc.getString("publicId") ?: ""
                                        myPhotoUrl = doc.getString("photoUrl") ?: auth.currentUser?.photoUrl?.toString()
                                    }
                                    startListeningMessagesIfReady()
                                }
                                .addOnFailureListener { e -> Log.w(TAG, "fallback query (publicId==uid) failed", e) }
                        }
                    }
                    .addOnFailureListener { e -> Log.w(TAG, "fallback query (uid) failed", e) }
            }
        }
    }

        //-----  DELAY CHAT SETTINGS -----//

    // 🔹 Live listener for delay chat settings
    private fun observeDelaySettings() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = db.collection("users").document(uid)

        delaySettingsListener = userRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "observeDelaySettings error", error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                delayChatEnabled = snapshot.getBoolean("delayChatEnabled") ?: false
                delayChatSeconds = (snapshot.getLong("delayChatSeconds") ?: 1L).toInt()
                Log.d(TAG, "Delay settings updated: enabled=$delayChatEnabled, seconds=$delayChatSeconds")
            }
        }
    }






    // ------------------ CONVO ID HELPERS ------------------

    private fun publicIdConvoId(): String {
        return listOf(myPublicId.ifBlank { myUid }, otherPublicId).sorted().joinToString("_")
    }

    private fun uidConvoId(): String? {
        val o = otherUid
        return if (myUid.isNotBlank() && !o.isNullOrBlank()) listOf(myUid, o).sorted().joinToString("_") else null
    }

    private fun getActiveConvoIds(): Pair<String?, String> {
        // returns Pair(uidConvoId (nullable), publicConvo)
        val uid = uidConvoId()
        val pub = publicIdConvoId()
        return Pair(uid, pub)
    }

    // ------------------ PRESENCE (TYPING) ------------------

    private fun presenceDocRefFor(convoId: String) = db.collection("chats").document(convoId).collection("meta").document("presence")
    // Note: storing presence under chats/{convoId}/meta/presence to avoid colliding with messages

    private fun startPresenceListener() {
        // attach to public or uid convo presence (prefer uid if available)
        val (uidConvo, pubConvo) = getActiveConvoIds()
        val convo = uidConvo ?: pubConvo
        if (convo.isBlank()) return

        presenceListener?.remove()
        presenceListener = presenceDocRefFor(convo).addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "presenceListener error", err)
                return@addSnapshotListener
            }
            if (snap != null && snap.exists()) {
                val typingMap = snap.get("typing") as? Map<*, *>
                val typed = typingMap?.mapNotNull { (k, v) ->
                    val key = k as? String ?: return@mapNotNull null
                    key to (v as? Boolean == true)
                }?.toMap() ?: emptyMap()
                _typingState.value = typed
            } else {
                _typingState.value = emptyMap()
            }
        }
    }

    fun setTyping(isTyping: Boolean) {
        val (uidConvo, pubConvo) = getActiveConvoIds()
        val convo = uidConvo ?: pubConvo
        if (convo.isBlank() || myUid.isBlank()) return
        val ref = presenceDocRefFor(convo)
        ref.set(mapOf("typing" to mapOf(myUid to isTyping)), SetOptions.merge())
    }

    // ------------------ LISTENERS: messages (primary + fallback) ------------------

    private fun clearListeners() {
        messagesListener?.remove(); messagesListener = null
        fallbackListener?.remove(); fallbackListener = null
    }

    private fun startListeningMessagesIfReady() {
        clearListeners()

        val uidConvo = uidConvoId()
        if (uidConvo.isNullOrBlank()) {
            Log.d(TAG, "No valid UID convoId available yet.")
            return
        }

        Log.d(TAG, "Starting primary (UID) messages listener for convoId=$uidConvo")
        messagesListener = db.collection("chats")
            .document(uidConvo)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snaps, err ->
                if (err != null) {
                    if (err is FirebaseFirestoreException) {
                        Log.e(TAG, "messagesListener failed for convoId=$uidConvo code=${err.code}, message=${err.message}")
                    } else {
                        Log.e(TAG, "messagesListener unexpected error for convoId=$uidConvo", err)
                    }
                    return@addSnapshotListener
                }

                if (snaps != null) {
                    val msgs = snaps.documents.mapNotNull { it.toChatUiMessage(myPublicId.ifBlank { myUid }) }
                    Log.d(TAG, "messagesListener($uidConvo) update: ${msgs.size} messages")
                    _messages.value = msgs
                    markDeliveredForSnapshots(snaps.documents)
                } else {
                    Log.d(TAG, "messagesListener($uidConvo) null snaps")
                }
            }

        // ensure presence listener is (re)started
        startPresenceListener()
    }









//----------------------------------------------------------------------
//    private fun startListeningMessagesIfReady() {
//        // clear previous
//        clearListeners()
//
//        val (uidConvo, pubConvo) = getActiveConvoIds()
//        val uidConvoId = uidConvo
//        val publicConvoId = pubConvo
//
//        fun attachListenerFor(convoId: String, onUpdate: (List<ChatUiMessage>) -> Unit): ListenerRegistration {
//            return db.collection("chats")
//                .document(convoId)
//                .collection("messages")
//                .orderBy("timestamp", Query.Direction.ASCENDING)
//                .addSnapshotListener { snaps, err ->
//                    if (err != null) {
//                        Log.w(TAG, "messagesListener($convoId) error", err)
//                        return@addSnapshotListener
//                    }
//                    if (snaps != null) {
//                        val msgs = snaps.documents.mapNotNull { it.toChatUiMessage(myPublicId.ifBlank { myUid }) }
//                        Log.d(TAG, "messagesListener($convoId) update: ${msgs.size} messages")
//                        onUpdate(msgs)
//                        // mark delivered for any messages not delivered for this uid
//                        markDeliveredForSnapshots(snaps.documents)
//                    } else {
//                        Log.d(TAG, "messagesListener($convoId) null snaps")
//                    }
//                }
//        }
//
//        if (!uidConvoId.isNullOrBlank()) {
//            Log.d(TAG, "Starting primary (UID) messages listener for convoId=$uidConvoId")
//            messagesListener = attachListenerFor(uidConvoId) { msgs ->
//                _messages.value = msgs
//                // if uid listener empty, attach fallback to publicId path to try legacy messages
//                if (msgs.isEmpty() && publicConvoId.isNotBlank() && publicConvoId != uidConvoId) {
//                    if (fallbackListener == null) {
//                        Log.d(TAG, "UID listener empty — attaching fallback listener for publicId-based convoId=$publicConvoId")
//                        fallbackListener = attachListenerFor(publicConvoId) { fallbackMsgs ->
//                            if (fallbackMsgs.isNotEmpty()) _messages.value = fallbackMsgs
//                        }
//                    }
//                } else {
//                    fallbackListener?.remove(); fallbackListener = null
//                }
//            }
//        } else {
//            // fall back to publicId-based convo
//            if (publicConvoId.isBlank()) {
//                Log.d(TAG, "No valid convoId available for listening yet.")
//                return
//            }
//            Log.d(TAG, "Starting messages listener for publicId-based convoId=$publicConvoId")
//            messagesListener = attachListenerFor(publicConvoId) { msgs -> _messages.value = msgs }
//        }
//
//        // ensure presence listener is (re)started
//        startPresenceListener()
//    }

    // mark deliveredBy.{myUid} = true for any message not yet marked
    private fun markDeliveredForSnapshots(docs: List<DocumentSnapshot>) {
        if (myUid.isBlank()) return
        docs.forEach { doc ->
            val deliveredBy = doc.get("deliveredBy") as? Map<*, *>
            val already = deliveredBy?.get(myUid) == true
            if (!already) {
                val convoId = uidConvoId() ?: publicIdConvoId()
                if (convoId.isBlank()) return@forEach
                val docRef = db.collection("chats").document(convoId).collection("messages").document(doc.id)
                val map = mapOf("deliveredBy.${myUid}" to true)
                docRef.set(map, SetOptions.merge())
            }
        }
    }

    // call from UI when messages are visible/read (e.g., on scroll or when app in foreground)

    // add this at top of ChatViewModel.kt alongside other imports

// ...

    fun markMessagesReadUpTo(timestampMillis: Long) {
        if (myUid.isBlank()) return
        val convoId = uidConvoId() ?: publicIdConvoId()
        if (convoId.isBlank()) return

        val messagesRef = db.collection("chats").document(convoId).collection("messages")
        // find messages with timestamp <= timestampMillis and mark readBy.{myUid} = true
        messagesRef.whereLessThanOrEqualTo("timestamp", Timestamp(Date(timestampMillis)))
            .get()
            .addOnSuccessListener { snaps ->
                snaps.documents.forEach { doc ->
                    val readBy = doc.get("readBy") as? Map<*, *>
                    if (readBy?.get(myUid) != true) {
                        db.collection("chats").document(convoId).collection("messages").document(doc.id)
                            .set(mapOf("readBy.${myUid}" to true), SetOptions.merge())
                    }
                }
            }
            .addOnFailureListener { e -> Log.w(TAG, "markMessagesReadUpTo failed", e) }
    }


    // ------------------ PAGINATION ------------------

    /**
     * Load earlier messages (older than currently loaded earliest message)
     * - uses pageSize (default 25)
     */
    fun loadEarlierMessages(pageSize: Int = 25) {
        if (isLoadingEarlier || reachedStart) return
        val convoId = uidConvoId() ?: publicIdConvoId()
        if (convoId.isBlank()) return
        isLoadingEarlier = true
        this.pageSize = pageSize

        val messagesRef = db.collection("chats").document(convoId).collection("messages")
        val baseQuery = messagesRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(pageSize.toLong())

        val qry = lastLoadedSnapshot?.let { baseQuery.startAfter(it) } ?: baseQuery

        qry.get().addOnSuccessListener { snap ->
            val docs = snap.documents
            if (docs.isEmpty()) {
                reachedStart = true
            } else {
                // update lastLoadedSnapshot to last doc of this page (for next page)
                lastLoadedSnapshot = docs.last()
                // map to ChatUiMessage, reversed to ascending order, and prepend to current list
                val older = docs.mapNotNull { it.toChatUiMessage(myPublicId.ifBlank { myUid }) }.reversed()
                _messages.value = older + _messages.value
            }
            isLoadingEarlier = false
        }.addOnFailureListener { e ->
            Log.w(TAG, "loadEarlierMessages failed", e)
            isLoadingEarlier = false
        }
    }

    fun resetPagination() {
        lastLoadedSnapshot = null
        reachedStart = false
    }

    // ------------------ SEND (with profanity filter + retry) ------------------

    // very simple profanity replace (example). You can replace with a better library.
    private fun profanityFilter(text: String): String {
        var result = text
        profanityList.forEach { bad ->
            val regex = Regex("(?i)\\b${Regex.escape(bad)}\\b")
            result = result.replace(regex) { "*".repeat(min(3, it.value.length)) }
        }
        return result
    }



    fun sendMessage(text: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || myUid.isBlank()) {
            onComplete?.invoke(false, "Empty or invalid message")
            return
        }

        val cleaned = profanityFilter(trimmed)
        val localId = "local-${System.currentTimeMillis()}-${(0..999).random()}"
        pendingRetries[localId] = 0

        if (delayChatEnabled) {
            val seconds = delayChatSeconds.coerceIn(1, 3)
            // Add to pending list
            _pendingMessages.value = _pendingMessages.value + (localId to PendingMessage(localId, cleaned, seconds))

            // Countdown + send after delay
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                for (i in seconds downTo 1) {
                    val updated = _pendingMessages.value.toMutableMap()
                    updated[localId] = updated[localId]?.copy(remainingSeconds = i) ?: continue
                    _pendingMessages.emit(updated)
                    kotlinx.coroutines.delay(1000)
                }

                // 🟢 If still pending (not canceled), send
                if (_pendingMessages.value.containsKey(localId)) {
                    performSendWithRetry(cleaned, localId, onComplete)
                    val updated = _pendingMessages.value.toMutableMap()
                    updated.remove(localId)
                    _pendingMessages.emit(updated)
                }
            }
        } else {
            // Send instantly
            performSendWithRetry(cleaned, localId, onComplete)
        }
    }

    // 🔹 Cancel message before it's sent (during delay)

    fun cancelPendingMessage(localId: String) {
        val updated = _pendingMessages.value.toMutableMap()
        updated.remove(localId)
        _pendingMessages.value = updated
        Log.d(TAG, "❌ Canceled pending message: $localId")
    }






//    fun sendMessage(text: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
//        val trimmed = text.trim()
//        if (trimmed.isBlank()) {
//            onComplete?.invoke(false, "Empty message")
//            return
//        }
//        if (myUid.isBlank()) {
//            onComplete?.invoke(false, "My UID not available")
//            return
//        }
//
//        // apply profanity filter
//        val cleaned = profanityFilter(trimmed)
//
//        // create a temp local id for retry bookkeeping
//        val localId = "local-${System.currentTimeMillis()}-${(0..999).random()}"
//        // optimistic local insertion (UI can display status=SENDING for this local id — UI must support)
//        // We won't add local object to _messages here; the real-time listener will add the saved doc once server writes.
//        pendingRetries[localId] = 0
//        performSendWithRetry(cleaned, localId, onComplete)
//    }

    private fun performSendWithRetry(cleaned: String, localId: String, onComplete: ((Boolean, String?) -> Unit)?) {
        val attempt = pendingRetries[localId] ?: 0
        if (attempt >= 3) {
            pendingRetries.remove(localId)
            onComplete?.invoke(false, "Max retries reached")
            return
        }
        pendingRetries[localId] = attempt + 1
        performSend(cleaned, object : SendCallback {
            override fun onResult(success: Boolean, err: String?) {
                if (success) {
                    pendingRetries.remove(localId)
                    onComplete?.invoke(true, null)
                } else {
                    // schedule retry with exponential backoff (e.g., 1s, 2s, 4s)
                    val backoffMs = (2.0.pow(attempt.toDouble()) * 1000).toLong()
                    Log.d(TAG, "Send failed, scheduling retry #${attempt + 1} in ${backoffMs}ms: $err")
                    mainHandler.postDelayed({
                        performSendWithRetry(cleaned, localId, onComplete)
                    }, backoffMs)
                }
            }
        })
    }

    // low-level send that actually writes to Firestore
    private fun performSend(cleaned: String, cb: SendCallback?) {
        val convoId = uidConvoId() ?: publicIdConvoId()
        val chatDocRef = db.collection("chats").document(convoId)
        val messagesRef = chatDocRef.collection("messages")

        // Participants: prefer UIDs if available, fallback to publicIds
        val participants = if (!myUid.isBlank() && !otherUid.isNullOrBlank()) {
            listOf(myUid, otherUid!!)
        } else {
            listOf(myPublicId.ifBlank { myUid }, otherPublicId)
        }

        // ensure parent doc exists
        val chatMeta = mapOf<String, Any>(
            "participants" to participants,
            "lastMessage" to cleaned,
            "lastUpdated" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        chatDocRef.set(chatMeta, SetOptions.merge()).addOnSuccessListener {
            val payload = hashMapOf<String, Any>(
                "fromUid" to myUid,
                "toUid" to (otherUid ?: ""),
                "fromId" to (myPublicId.ifBlank { myUid }),
                "toId" to otherPublicId,
                "text" to cleaned,
                "timestamp" to FieldValue.serverTimestamp(),
                "starredBy" to mapOf<String, Boolean>(),
                // deliveredBy/readBy may be added later by recipients
            )
            messagesRef.add(payload)
                .addOnSuccessListener { docRef ->
                    Log.d(TAG, "Message sent: ${docRef.id}")
                    cb?.onResult(true, null)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to add message", e)
                    cb?.onResult(false, e.localizedMessage)
                }
        }.addOnFailureListener { e ->
            Log.w(TAG, "Failed to create/update chat parent doc", e)
            cb?.onResult(false, e.localizedMessage)
        }
    }

    private interface SendCallback { fun onResult(success: Boolean, err: String?) }

    // ------------------ EDIT / DELETE ------------------

    fun editMessage(messageId: String, newText: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
        if (myUid.isBlank()) {
            onComplete?.invoke(false, "My UID not available")
            return
        }
        val convoId = uidConvoId() ?: publicIdConvoId()
        if (convoId.isBlank()) { onComplete?.invoke(false, "ConvoId not available"); return }
        val docRef = db.collection("chats").document(convoId).collection("messages").document(messageId)
        val cleaned = profanityFilter(newText.trim())
        val update = mapOf(
            "text" to cleaned,
            "edited" to true,
            "editedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(update).addOnSuccessListener {
            onComplete?.invoke(true, null)
        }.addOnFailureListener { e ->
            Log.w(TAG, "editMessage failed", e)
            onComplete?.invoke(false, e.localizedMessage)
        }
    }

    fun deleteMessage(messageId: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val convoId = uidConvoId() ?: publicIdConvoId()
        if (convoId.isBlank()) { onComplete?.invoke(false, "ConvoId not available"); return }
        db.collection("chats").document(convoId).collection("messages").document(messageId)
            .delete()
            .addOnSuccessListener { onComplete?.invoke(true, null) }
            .addOnFailureListener { e -> Log.w(TAG, "deleteMessage failed", e); onComplete?.invoke(false, e.localizedMessage) }
    }

    // ------------------ REACTIONS ------------------

    /**
     * Toggle reaction for current user on message (emoji). Uses transaction to update reactions map safely.
     */
    fun toggleReaction(messageId: String, emoji: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val convoId = uidConvoId() ?: publicIdConvoId()
        if (convoId.isBlank() || myUid.isBlank()) { onComplete?.invoke(false, "Not ready"); return }

        val docRef = db.collection("chats").document(convoId).collection("messages").document(messageId)
        db.runTransaction { tx ->
            val snap = tx.get(docRef)
            val current = (snap.get("reactions") as? Map<String, List<String>>)?.toMutableMap() ?: mutableMapOf()
            val list = current[emoji]?.toMutableList() ?: mutableListOf()
            if (list.contains(myUid)) {
                list.remove(myUid)
            } else {
                list.add(myUid)
            }
            current[emoji] = list
            tx.update(docRef, "reactions", current)
        }.addOnSuccessListener { onComplete?.invoke(true, null) }
            .addOnFailureListener { e -> Log.w(TAG, "toggleReaction failed", e); onComplete?.invoke(false, e.localizedMessage) }
    }

    // ------------------ OTHER HELPERS ------------------

    // For manual retry via UI (retry button), re-run performSend once (no localId bookkeeping here)
    fun retrySend(text: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
        performSendWithRetry(text, "manual-${System.currentTimeMillis()}", onComplete)
    }

    override fun onCleared() {
        super.onCleared()
        clearListeners()
        presenceListener?.remove(); presenceListener = null
        otherUserListener?.remove(); otherUserListener = null
        myProfileListener?.remove(); myProfileListener = null
        delaySettingsListener?.remove(); delaySettingsListener = null



        // clear typing presence
        val convoId = uidConvoId() ?: publicIdConvoId()
        if (convoId.isNotBlank() && myUid.isNotBlank()) {
            presenceDocRefFor(convoId).set(mapOf("typing" to mapOf(myUid to false)), SetOptions.merge())
        }
    }
}

class ChatViewModelFactory(private val otherPublicId: String) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(otherPublicId) as T
    }
}

// 🔹 Data model for pending unsent messages
data class PendingMessage(
    val localId: String,
    val text: String,
    val remainingSeconds: Int
)


















//package com.chat.safeplay.chat.handler
//
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import com.google.firebase.Timestamp
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.*
//import com.google.firebase.firestore.FieldValue
//import com.google.firebase.firestore.SetOptions
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//
///**
// * ChatViewModel with robust listeners:
// * - Prefer UID-based convo path (for security rules)
// * - Fallback to publicId-based convo path if UID path yields no messages or UIDs aren't available
// */
//class ChatViewModel(private val otherPublicId: String) : ViewModel() {
//
//    private val TAG = "ChatViewModel"
//    private val db = FirebaseFirestore.getInstance()
//    private val auth = FirebaseAuth.getInstance()
//
//    private val _messages = MutableStateFlow<List<ChatUiMessage>>(emptyList())
//    val messages: StateFlow<List<ChatUiMessage>> = _messages.asStateFlow()
//
//    private val _otherUser = MutableStateFlow(ChatUserUi(publicId = otherPublicId))
//    val otherUser: StateFlow<ChatUserUi> = _otherUser.asStateFlow()
//
//    // my uid (from FirebaseAuth) — used for participants and convoId
//    var myUid: String = auth.currentUser?.uid ?: ""
//        private set
//
//    // my publicId (kept for UI / message payload)
//    var myPublicId: String = ""
//        private set
//
//    var myPhotoUrl: String? = auth.currentUser?.photoUrl?.toString()
//        private set
//
//    // other user's uid (discovered when loading the other user's profile)
//    private var otherUid: String? = null
//
//    private val _selected = MutableStateFlow<Set<String>>(emptySet())
//    val selected: StateFlow<Set<String>> = _selected.asStateFlow()
//
//    private var messagesListener: ListenerRegistration? = null
//    private var fallbackListener: ListenerRegistration? = null
//    private var otherUserListener: ListenerRegistration? = null
//    private var myProfileListener: ListenerRegistration? = null
//
//    init {
//        Log.d(TAG, "Init ChatViewModel: myUid=$myUid otherPublicId=$otherPublicId")
//
//        // 1) Load other user's profile and attempt to capture their UID
//        val otherDocRefById = db.collection("users").document(otherPublicId)
//        otherUserListener = otherDocRefById.addSnapshotListener { snap, err ->
//            if (err != null) {
//                Log.w(TAG, "otherUserListener error", err)
//                return@addSnapshotListener
//            }
//
//            if (snap != null && snap.exists()) {
//                val name = snap.getString("name")
//                val show = snap.getBoolean("showDisplayName") ?: false
//                val photo = snap.getString("photoUrl")
//                _otherUser.value = ChatUserUi(publicId = otherPublicId, name = name, showDisplayName = show, photoUrl = photo)
//
//                otherUid = when {
//                    // if doc.id looks like a uid and is different from the publicId, prefer it
//                    snap.id.isNotBlank() && snap.id.length >= 6 && snap.id != otherPublicId -> snap.id
//                    snap.getString("uid") != null -> snap.getString("uid")
//                    else -> null
//                } ?: snap.getString("uid")
//
//                val docPublicId = snap.getString("publicId")
//                if (!docPublicId.isNullOrBlank()) {
//                    _otherUser.value = _otherUser.value.copy(publicId = docPublicId)
//                }
//
//                // start listeners (the method will decide primary vs fallback)
//                startListeningMessagesIfReady()
//            } else {
//                // fallback query by publicId field
//                db.collection("users")
//                    .whereEqualTo("publicId", otherPublicId)
//                    .limit(1)
//                    .get()
//                    .addOnSuccessListener { docs ->
//                        if (!docs.isEmpty) {
//                            val doc = docs.documents[0]
//                            val name = doc.getString("name")
//                            val show = doc.getBoolean("showDisplayName") ?: false
//                            val photo = doc.getString("photoUrl")
//                            _otherUser.value = ChatUserUi(publicId = otherPublicId, name = name, showDisplayName = show, photoUrl = photo)
//
//                            otherUid = if (!doc.id.isNullOrBlank()) doc.id else doc.getString("uid")
//                            startListeningMessagesIfReady()
//                        } else {
//                            _otherUser.value = ChatUserUi(publicId = otherPublicId)
//                            Log.w(TAG, "Could not find other user doc for publicId=$otherPublicId")
//                            // still attempt to listen to publicId-based convo
//                            startListeningMessagesIfReady()
//                        }
//                    }
//                    .addOnFailureListener { e -> Log.w(TAG, "otherUser fallback query failed", e) }
//            }
//        }
//
//        // 2) Load my profile to populate myPublicId
//        if (myUid.isNotBlank()) {
//            val myDocRef = db.collection("users").document(myUid)
//            myProfileListener = myDocRef.addSnapshotListener { snap, err ->
//                if (err != null) {
//                    Log.w(TAG, "myProfileListener error", err)
//                    return@addSnapshotListener
//                }
//
//                if (snap != null && snap.exists()) {
//                    val newPublicId = snap.getString("publicId") ?: ""
//                    val newPhoto = snap.getString("photoUrl") ?: auth.currentUser?.photoUrl?.toString()
//                    val needRestart = myPublicId != newPublicId
//                    myPublicId = newPublicId
//                    myPhotoUrl = newPhoto
//                    Log.d(TAG, "Loaded my profile (doc-by-uid). publicId=$myPublicId")
//                    if (needRestart) startListeningMessagesIfReady() else startListeningMessagesIfReady()
//                } else {
//                    // fallback queries
//                    db.collection("users")
//                        .whereEqualTo("uid", myUid)
//                        .limit(1)
//                        .get()
//                        .addOnSuccessListener { docs ->
//                            if (!docs.isEmpty) {
//                                val doc = docs.documents[0]
//                                val newPublicId = doc.getString("publicId") ?: ""
//                                val newPhoto = doc.getString("photoUrl") ?: auth.currentUser?.photoUrl?.toString()
//                                myPublicId = newPublicId
//                                myPhotoUrl = newPhoto
//                                Log.d(TAG, "Loaded my profile (query uid). publicId=$myPublicId")
//                                startListeningMessagesIfReady()
//                            } else {
//                                db.collection("users")
//                                    .whereEqualTo("publicId", myUid)
//                                    .limit(1)
//                                    .get()
//                                    .addOnSuccessListener { results ->
//                                        if (!results.isEmpty) {
//                                            val doc = results.documents[0]
//                                            val newPublicId = doc.getString("publicId") ?: ""
//                                            val newPhoto = doc.getString("photoUrl") ?: auth.currentUser?.photoUrl?.toString()
//                                            myPublicId = newPublicId
//                                            myPhotoUrl = newPhoto
//                                            Log.d(TAG, "Loaded my profile (query publicId==uid). publicId=$myPublicId")
//                                        } else {
//                                            Log.w(TAG, "No users doc found for myUid=$myUid; myPublicId remains blank")
//                                        }
//                                        startListeningMessagesIfReady()
//                                    }
//                                    .addOnFailureListener { e -> Log.w(TAG, "fallback query (publicId==uid) failed", e) }
//                            }
//                        }
//                        .addOnFailureListener { e -> Log.w(TAG, "fallback query (uid) failed", e) }
//                }
//            }
//        } else {
//            Log.w(TAG, "myUid is blank; user may not be authenticated.")
//            // we still attempt to listen to publicId-based convo if possible
//            startListeningMessagesIfReady()
//        }
//    }
//
//    /**
//     * Helper: build a publicId-based convo id (fallback)
//     */
//    private fun publicIdConvoId(): String {
//        return listOf(myPublicId.ifBlank { myUid }, otherPublicId).sorted().joinToString("_")
//    }
//
//    /**
//     * Helper: build uid-based convo id (preferred when both UIDs available)
//     */
//    private fun uidConvoId(): String? {
//        val o = otherUid
//        return if (myUid.isNotBlank() && !o.isNullOrBlank()) {
//            listOf(myUid, o).sorted().joinToString("_")
//        } else null
//    }
//
//    private fun clearListeners() {
//        messagesListener?.remove()
//        messagesListener = null
//        fallbackListener?.remove()
//        fallbackListener = null
//    }
//
//    /**
//     * Start listeners robustly:
//     * - prefer UID convo if possible
//     * - if UID convo returns empty, attach fallback publicId listener
//     * - if UIDs aren't available, directly listen to publicId convo
//     */
//    private fun startListeningMessagesIfReady() {
//        // remove existing listeners first
//        clearListeners()
//
//        val uidConvo = uidConvoId()
//        val publicConvo = publicIdConvoId()
//
//        // attach function
//        fun attachListener(convoId: String, onUpdate: (List<ChatUiMessage>) -> Unit): ListenerRegistration {
//            return db.collection("chats")
//                .document(convoId)
//                .collection("messages")
//                .orderBy("timestamp", Query.Direction.ASCENDING)
//                .addSnapshotListener { snaps, err ->
//                    if (err != null) {
//                        Log.w(TAG, "messagesListener($convoId) error", err)
//                        return@addSnapshotListener
//                    }
//                    if (snaps != null) {
//                        val msgs = snaps.documents.mapNotNull { it.toChatUiMessage(myPublicId.ifBlank { myUid }) }
//                        Log.d(TAG, "messagesListener($convoId) update: ${msgs.size} messages")
//                        onUpdate(msgs)
//                    } else {
//                        Log.d(TAG, "messagesListener($convoId) got null snaps")
//                    }
//                }
//        }
//
//        if (uidConvo != null) {
//            Log.d(TAG, "Starting primary (UID) messages listener for convoId=$uidConvo")
//            messagesListener = attachListener(uidConvo) { msgs ->
//                _messages.value = msgs
//                // if UID-based convo is empty, try fallback to publicId-based convo (only once)
//                if (msgs.isEmpty() && publicConvo.isNotBlank() && publicConvo != uidConvo) {
//                    if (fallbackListener == null) {
//                        Log.d(TAG, "UID listener empty — attaching fallback listener for publicId-based convoId=$publicConvo")
//                        fallbackListener = attachListener(publicConvo) { fallbackMsgs ->
//                            if (fallbackMsgs.isNotEmpty()) {
//                                Log.d(TAG, "Fallback listener returned ${fallbackMsgs.size} messages — using them")
//                                _messages.value = fallbackMsgs
//                            } else {
//                                Log.d(TAG, "Fallback listener also empty for convoId=$publicConvo")
//                            }
//                        }
//                    }
//                } else {
//                    // if UID listener has messages, ensure fallback is removed
//                    fallbackListener?.remove()
//                    fallbackListener = null
//                }
//            }
//        } else {
//            // no UIDs available yet — listen to publicId-based convo directly
//            if (publicConvo.isBlank()) {
//                Log.d(TAG, "No valid convoId available (publicConvo blank). Will retry when data available.")
//                return
//            }
//            Log.d(TAG, "Starting messages listener for publicId-based convoId=$publicConvo")
//            messagesListener = attachListener(publicConvo) { msgs ->
//                _messages.value = msgs
//            }
//        }
//    }
//
//    /**
//     * Send a message. Ensures parent chat doc exists and writes participants as UIDs where possible.
//     * Message payload includes both UID and publicId fields to preserve compatibility with UI.
//     */
//    fun sendMessage(text: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
//        val TAG_SEND = "ChatViewModel-sendMessage"
//        val trimmed = text.trim()
//        if (trimmed.isBlank()) {
//            onComplete?.invoke(false, "Empty text")
//            return
//        }
//
//        // ensure we at least have myUid
//        if (myUid.isBlank()) {
//            onComplete?.invoke(false, "My UID not available; cannot send")
//            Log.w(TAG_SEND, "Attempted to send when myUid blank")
//            return
//        }
//
//        // ensure otherUid: if missing, try to resolve quickly; otherwise we'll create parent chat using publicId fallback
//        if (otherUid.isNullOrBlank()) {
//            // try one-time lookup by publicId
//            db.collection("users")
//                .whereEqualTo("publicId", otherPublicId)
//                .limit(1)
//                .get()
//                .addOnSuccessListener { docs ->
//                    if (!docs.isEmpty) {
//                        val doc = docs.documents[0]
//                        otherUid = if (!doc.id.isNullOrBlank()) doc.id else doc.getString("uid")
//                    }
//                    // continue with send even if otherUid still null — performSend will handle choosing convoId
//                    performSend(trimmed, onComplete)
//                }
//                .addOnFailureListener { e ->
//                    Log.w(TAG_SEND, "Failed to resolve otherUid", e)
//                    // still try to send with fallback
//                    performSend(trimmed, onComplete)
//                }
//        } else {
//            performSend(trimmed, onComplete)
//        }
//    }
//
//    // actual send implementation (tries to use UIDs for parent doc but will fall back to publicId convo id)
//    private fun performSend(trimmed: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
//        val TAG_SEND = "ChatViewModel-performSend"
//
//        val convoId = uidConvoId() ?: publicIdConvoId()
//        val chatDocRef = db.collection("chats").document(convoId)
//        val messagesRef = chatDocRef.collection("messages")
//
//        // Build participants. Prefer UIDs; fall back to publicIds if UIDs not available.
//        val participants = if (!myUid.isNullOrBlank() && !otherUid.isNullOrBlank()) {
//            listOf(myUid, otherUid!!)
//        } else {
//            listOf(myPublicId.ifBlank { myUid }, otherPublicId)
//        }
//
//        val chatMeta = mapOf<String, Any>(
//            "participants" to participants,
//            "lastMessage" to trimmed,
//            "lastUpdated" to FieldValue.serverTimestamp(),
//            "createdAt" to FieldValue.serverTimestamp()
//        )
//
//        chatDocRef.set(chatMeta, SetOptions.merge())
//            .addOnSuccessListener {
//                val payload = hashMapOf<String, Any>(
//                    "fromUid" to myUid,
//                    "toUid" to (otherUid ?: ""),
//                    "fromId" to (myPublicId.ifBlank { myUid }),
//                    "toId" to otherPublicId,
//                    "text" to trimmed,
//                    "timestamp" to FieldValue.serverTimestamp(),
//                    "starredBy" to mapOf<String, Boolean>()
//                )
//
//                messagesRef.add(payload)
//                    .addOnSuccessListener { docRef ->
//                        Log.d(TAG_SEND, "Message sent: ${docRef.id}")
//                        onComplete?.invoke(true, null)
//                    }
//                    .addOnFailureListener { e ->
//                        Log.w(TAG_SEND, "Failed to add message", e)
//                        onComplete?.invoke(false, e.localizedMessage)
//                    }
//            }
//            .addOnFailureListener { e ->
//                Log.w(TAG_SEND, "Failed to create/update chat parent doc", e)
//                onComplete?.invoke(false, e.localizedMessage)
//            }
//    }
//
//    // Delete message by ID
//    fun deleteMessage(messageId: String) {
//        val convoId = uidConvoId() ?: publicIdConvoId()
//        db.collection("chats").document(convoId).collection("messages").document(messageId).delete()
//    }
//
//    fun toggleStarMessage(messageId: String) {
//        val convoId = uidConvoId() ?: publicIdConvoId()
//        val docRef = db.collection("chats").document(convoId).collection("messages").document(messageId)
//
//        db.runTransaction { tx ->
//            val snap = tx.get(docRef)
//            val current = (snap.get("starredBy") as? Map<String, Boolean>)?.toMutableMap() ?: mutableMapOf()
//            val already = current[myUid] == true
//            if (already) current.remove(myUid) else current[myUid] = true
//            tx.update(docRef, "starredBy", current)
//        }
//    }
//
//    fun toggleSelect(messageId: String) {
//        val set = _selected.value.toMutableSet()
//        if (set.contains(messageId)) set.remove(messageId) else set.add(messageId)
//        _selected.value = set
//    }
//
//    fun clearSelection() {
//        _selected.value = emptySet()
//    }
//
//    fun deleteSelected() {
//        val convoId = uidConvoId() ?: publicIdConvoId()
//        val sel = _selected.value.toList()
//        sel.forEach { id ->
//            db.collection("chats").document(convoId).collection("messages").document(id).delete()
//        }
//        clearSelection()
//    }
//
//    fun toggleStarSelected() {
//        val convoId = uidConvoId() ?: publicIdConvoId()
//        val sel = _selected.value.toList()
//        sel.forEach { messageId ->
//            val docRef = db.collection("chats").document(convoId).collection("messages").document(messageId)
//            db.runTransaction { tx ->
//                val snap = tx.get(docRef)
//                val currentMap = (snap.get("starredBy") as? Map<String, Boolean>)?.toMutableMap() ?: mutableMapOf()
//                val currently = currentMap[myUid] == true
//                if (currently) currentMap.remove(myUid) else currentMap[myUid] = true
//                tx.update(docRef, "starredBy", currentMap)
//            }
//        }
//        clearSelection()
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        clearListeners()
//        otherUserListener?.remove()
//        myProfileListener?.remove()
//    }
//}
//
//class ChatViewModelFactory(private val otherPublicId: String) : ViewModelProvider.Factory {
//    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
//        return ChatViewModel(otherPublicId) as T
//    }
//}
