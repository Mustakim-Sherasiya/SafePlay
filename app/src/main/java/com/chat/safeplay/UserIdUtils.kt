package com.chat.safeplay

import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

/**
 * Generate a short human-friendly public id like A1B2C3.
 * You can tune `letters` / `digits` or length.
 */
fun generatePublicId(lengthPairs: Int = 3): String {
    val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val digits = "0123456789"
    val sb = StringBuilder()
    repeat(lengthPairs) {
        sb.append(letters.random())
        sb.append(digits.random())
    }
    return sb.toString() // e.g. "A1B2C3"
}

/**
 * Tries to create a unique publicId by checking Firestore.
 * - onResult(true, id) when unique id found
 * - onResult(false, null) on failure after retries
 */
fun ensureUniquePublicId(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    maxRetries: Int = 6,
    onResult: (success: Boolean, id: String?) -> Unit
) {
    fun attempt(remaining: Int) {
        val id = generatePublicId()
        firestore.collection("users")
            .whereEqualTo("publicId", id)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    onResult(true, id)
                } else {
                    if (remaining > 0) attempt(remaining - 1)
                    else onResult(false, null)
                }
            }
            .addOnFailureListener {
                if (remaining > 0) attempt(remaining - 1)
                else onResult(false, null)
            }
    }
    attempt(maxRetries)
}
