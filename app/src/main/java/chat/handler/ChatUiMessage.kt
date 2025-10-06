package com.chat.safeplay.chat.handler

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

enum class MessageStatus { SENDING, SENT, FAILED }

data class ChatUiMessage(
    val id: String,
    val fromId: String,      // publicId if available, else fallback to uid
    val fromUid: String?,    // optional uid field
    val text: String,
    val timestamp: Long,
    val isNarration: Boolean = false,
    val starredByMe: Boolean = false,
    val delivered: Boolean = false,
    val read: Boolean = false,
    val reactions: Map<String, List<String>> = emptyMap(),
    val edited: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT
)

/**
 * Extension function for mapping Firestore → ChatUiMessage
 * Ensures `fromId` is always normalized (publicId preferred, else uid).
 */
fun DocumentSnapshot.toChatUiMessage(myPublicIdOrUid: String): ChatUiMessage? {
    // Normalize fromId: prefer fromId (publicId) field, fallback to fromUid
    val fromId = getString("fromId") ?: getString("fromUid") ?: return null
    val fromUid = getString("fromUid")
    val text = getString("text") ?: ""

    // Handle timestamp which might be Timestamp, Long, or Double
    val tsField = get("timestamp")
    val tsLong: Long = when (tsField) {
        is Timestamp -> tsField.toDate().time
        is Long -> tsField
        is Double -> tsField.toLong()
        else -> 0L
    }

    // Starred status per user
    val starredBy = get("starredBy") as? Map<*, *>
    val starredByMe = starredBy?.get(myPublicIdOrUid) == true

    // Delivered / Read markers
    val deliveredBy = get("deliveredBy") as? Map<*, *>
    val readBy = get("readBy") as? Map<*, *>
    val delivered = deliveredBy?.get(myPublicIdOrUid) == true
    val read = readBy?.get(myPublicIdOrUid) == true

    // Reactions map: emoji -> list of userIds
    val reactionsRaw = get("reactions") as? Map<*, *>
    val reactions = reactionsRaw?.mapNotNull { entry ->
        val k = entry.key as? String ?: return@mapNotNull null
        val arr = entry.value as? List<*>
        val uids = arr?.mapNotNull { it as? String } ?: emptyList()
        k to uids
    }?.toMap() ?: emptyMap()

    val edited = getBoolean("edited") ?: false

    return ChatUiMessage(
        id = id,
        fromId = fromId, // normalized (publicId preferred, else uid)
        fromUid = fromUid,
        text = text,
        timestamp = tsLong,
        isNarration = false,
        starredByMe = starredByMe,
        delivered = delivered,
        read = read,
        reactions = reactions,
        edited = edited,
        status = MessageStatus.SENT
    )
}




//package com.chat.safeplay.chat.handler
//
//import com.google.firebase.Timestamp
//import com.google.firebase.firestore.DocumentSnapshot
//
//enum class MessageStatus { SENDING, SENT, FAILED }
//
//data class ChatUiMessage(
//    val id: String,
//    val fromId: String,      // publicId OR fallback to uid
//    val fromUid: String?,    // optional
//    val text: String,
//    val timestamp: Long,
//    val isNarration: Boolean = false,
//    val starredByMe: Boolean = false,
//    val delivered: Boolean = false,
//    val read: Boolean = false,
//    val reactions: Map<String, List<String>> = emptyMap(),
//    val edited: Boolean = false,
//    val status: MessageStatus = MessageStatus.SENT
//)
//
//// Extension function for mapping Firestore → ChatUiMessage
//fun DocumentSnapshot.toChatUiMessage(myPublicIdOrUid: String): ChatUiMessage? {
//    val fromId = getString("fromId") ?: getString("fromUid") ?: return null
//    val fromUid = getString("fromUid")
//    val text = getString("text") ?: ""
//
//    val tsField = get("timestamp")
//    val tsLong: Long = when (tsField) {
//        is Timestamp -> tsField.toDate().time
//        is Long -> tsField
//        is Double -> tsField.toLong()
//        else -> 0L
//    }
//
//    val starredBy = get("starredBy") as? Map<*, *>
//    val starredByMe = starredBy?.get(myPublicIdOrUid) == true
//
//    val deliveredBy = get("deliveredBy") as? Map<*, *>
//    val readBy = get("readBy") as? Map<*, *>
//    val delivered = deliveredBy?.get(myPublicIdOrUid) == true
//    val read = readBy?.get(myPublicIdOrUid) == true
//
//    val reactionsRaw = get("reactions") as? Map<*, *>
//    val reactions = reactionsRaw?.mapNotNull { entry ->
//        val k = entry.key as? String ?: return@mapNotNull null
//        val arr = entry.value as? List<*>
//        val uids = arr?.mapNotNull { it as? String } ?: emptyList()
//        k to uids
//    }?.toMap() ?: emptyMap()
//
//    val edited = getBoolean("edited") ?: false
//
//    return ChatUiMessage(
//        id = id,
//        fromId = fromId,
//        fromUid = fromUid,
//        text = text,
//        timestamp = tsLong,
//        isNarration = false,
//        starredByMe = starredByMe,
//        delivered = delivered,
//        read = read,
//        reactions = reactions,
//        edited = edited,
//        status = MessageStatus.SENT
//    )
//}
