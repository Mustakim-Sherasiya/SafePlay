package com.chat.safeplay

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.firebase.FirebaseException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import android.util.Log
import com.google.firebase.auth.*
import com.google.firebase.firestore.QuerySnapshot
import kotlin.random.Random
import com.chat.safeplay.ensureUniquePublicId
import com.google.firebase.firestore.SetOptions



// Helper to get Activity from a Context (handles ContextWrapper)
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return if (this is Activity) this else null
}

/**
 * Safer sendPhoneOtp - will call onFailure with a clear message if it can't obtain an Activity.
 *
 * @param phoneNumber full phone number (with +countryCode e.g. +9199xxxx)
 * @param context any Context (LocalContext.current is ok)
 * @param onCodeSent called with verificationId when code is sent
 * @param onFailure called with a readable error message
 */
fun sendPhoneOtp(
    phoneNumber: String,
    context: Context,
    onCodeSent: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    val activity = context.findActivity()
    if (activity == null) {
        onFailure("Unable to start phone verification: activity not available from context.")
        return
    }

    try {
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                    // optional: auto verification (your PhoneOtpScreen handles this case if needed)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    onFailure("OTP sending failed: ${e.message ?: "Unknown error"}")
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    onCodeSent(verificationId)
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    } catch (e: Exception) {
        onFailure("Failed to send OTP: ${e.message ?: "Unknown error"}")
    }
}
// ------------------ verifyPhoneOtp (fixed & complete) ------------------
/**
 * Called after OTP entry. This:
 * 1) signs in with phone credential (verifies OTP),
 * 2) creates email/password auth user,
 * 3) generates unique publicId,
 * 4) writes users/{uid} doc in Firestore,
 * 5) sends email verification and signs out,
 * 6) calls onSuccess() or onFailure(msg).
 */
/**
 * Verifies phone OTP, creates email/password auth user and writes a user document with a unique publicId.
 * Callbacks:
 *  - onSuccess() when flow completed (user created in Auth & Firestore and email verification requested)
 *  - onFailure(message) on any error (attempts to clean up newly created auth user when appropriate)
 */

fun verifyPhoneOtp(
    verificationId: String,
    otpCode: String,
    email: String,
    password: String,
    context: Context,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)

    // 1) Sign in with phone credential (verifies OTP)
    auth.signInWithCredential(credential)
        .addOnCompleteListener { phoneSignInTask ->
            if (!phoneSignInTask.isSuccessful) {
                val msg = phoneSignInTask.exception?.localizedMessage ?: "Phone verification failed"
                Log.e("verifyPhoneOtp", "Phone sign-in failed: $msg")
                onFailure(msg)
                return@addOnCompleteListener
            }

            // Capture phone number from the phone sign-in result (do not rely on auth.currentUser later)
            val phoneUser = phoneSignInTask.result?.user
            val phoneNumberCaptured = phoneUser?.phoneNumber ?: ""
            Log.d("verifyPhoneOtp", "Phone verified, captured phone=$phoneNumberCaptured")

            // 2) Create email/password auth user
            auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener { createTask ->
                    if (!createTask.isSuccessful) {
                        val msg = createTask.exception?.localizedMessage ?: "Account creation failed"
                        Log.e("verifyPhoneOtp", "createUserWithEmailAndPassword failed: $msg")
                        onFailure(msg)
                        return@addOnCompleteListener
                    }

                    val createdUser = createTask.result?.user
                    if (createdUser == null) {
                        Log.e("verifyPhoneOtp", "createUser succeeded but user is null")
                        onFailure("Account created but user is null")
                        return@addOnCompleteListener
                    }

                    val uid = createdUser.uid
                    Log.d("verifyPhoneOtp", "Auth created uid=$uid")

                    // Prepare combined contact string for displayName + Firestore
                    val contactCombined = "${email.trim()} • $phoneNumberCaptured"

                    // Update Auth user profile displayName so Firebase Auth shows "email • phone"
                    val profile = UserProfileChangeRequest.Builder()
                        .setDisplayName(contactCombined)
                        .build()

                    createdUser.updateProfile(profile)
                        .addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                Log.d("verifyPhoneOtp", "Auth displayName updated to: $contactCombined")
                            } else {
                                Log.w("verifyPhoneOtp", "Failed to update displayName: ${profileTask.exception?.message}")
                                // continue anyway — displayName is convenience, Firestore is the source of truth
                            }

                            // 3) Generate unique publicId and write user doc to Firestore
                            ensureUniquePublicIdUnique { success, publicId ->
                                if (success && publicId != null) {
                                    val firestore = FirebaseFirestore.getInstance()

                                    val userData = hashMapOf(
                                        "email" to email.trim(),
                                        "phone" to phoneNumberCaptured,
                                        "contact" to contactCombined,   // combined field for UI
                                        "role" to "user",
                                        "publicId" to publicId,
                                        "createdAt" to System.currentTimeMillis(),
                                        "emailVerified" to createdUser.isEmailVerified
                                    )

                                    // Use merge so later writes (pin, settings) don't get overwritten
                                    firestore.collection("users").document(uid)
                                        .set(userData, SetOptions.merge())
                                        .addOnSuccessListener {
                                            Log.d("verifyPhoneOtp", "Firestore write SUCCESS for uid=$uid publicId=$publicId")

                                            // Send email verification (best-effort). Then sign out.
                                            createdUser.sendEmailVerification()
                                                .addOnCompleteListener { emailTask ->
                                                    if (emailTask.isSuccessful) {
                                                        Log.d("verifyPhoneOtp", "Verification email sent for uid=$uid")
                                                    } else {
                                                        Log.w("verifyPhoneOtp", "Failed to send verification email: ${emailTask.exception?.message}")
                                                    }
                                                    // Sign out until user verifies email
                                                    auth.signOut()
                                                    onSuccess()
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("verifyPhoneOtp", "Firestore write FAILED for uid=$uid : ${e.message}", e)
                                            // Try to delete the newly created auth user to avoid orphan accounts
                                            createdUser.delete().addOnCompleteListener {
                                                onFailure("Failed to save user data: ${e.message}")
                                            }
                                        }
                                } else {
                                    Log.e("verifyPhoneOtp", "Failed to generate unique publicId")
                                    // cleanup created auth user
                                    createdUser.delete().addOnCompleteListener {
                                        onFailure("Failed to generate unique user id")
                                    }
                                }
                            } // end ensureUniquePublicIdUnique callback
                        } // end updateProfile listener
                } // end createUserWithEmailAndPassword listener
        } // end phone sign-in listener
        .addOnFailureListener { e ->
            val msg = e.localizedMessage ?: "Phone verification failed"
            Log.e("verifyPhoneOtp", "Phone credential sign-in failed: $msg")
            onFailure(msg)
        }
}

// ------------------ ensureUniquePublicIdUnique helper ------------------
fun ensureUniquePublicIdUnique(
    maxAttempts: Int = 6,
    length: Int = 6,
    callback: (Boolean, String?) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    var attempts = 0

    fun tryOnce() {
        attempts++
        val candidate = randomPatternId(length)

        firestore.collection("users")
            .whereEqualTo("publicId", candidate)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    callback(true, candidate)
                } else {
                    if (attempts >= maxAttempts) callback(false, null) else tryOnce()
                }
            }
            .addOnFailureListener {
                if (attempts >= maxAttempts) callback(false, null) else tryOnce()
            }
    }

    tryOnce()
}

// pattern A1B2C3...
private fun randomPatternId(length: Int = 6): String {
    val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val digits = "0123456789"
    val sb = StringBuilder()
    for (i in 0 until length) {
        if (i % 2 == 0) sb.append(letters.random()) else sb.append(digits.random())
    }
    return sb.toString()
}









/**
 * Linking-safe verifyPhoneOtp:
 * - signs in with phone credential
 * - links email+password to the same user (avoids "email already in use")
 * - writes users/{uid} if missing
 * - sends email verification and signs out
 */
//fun verifyPhoneOtp(
//    verificationId: String,
//    otpCode: String,
//    email: String,
//    password: String,
//    context: Context,
//    onSuccess: () -> Unit,
//    onFailure: (String) -> Unit
//) {
//    val auth = FirebaseAuth.getInstance()
//    val firestore = FirebaseFirestore.getInstance()
//    val phoneCred = PhoneAuthProvider.getCredential(verificationId, otpCode)
//
//    // 1) Sign in with phone credential
//    auth.signInWithCredential(phoneCred)
//        .addOnCompleteListener { phoneTask ->
//            if (!phoneTask.isSuccessful) {
//                onFailure("Phone verification failed: ${phoneTask.exception?.message ?: "Unknown error"}")
//                return@addOnCompleteListener
//            }
//
//            val currentUser = auth.currentUser
//            val emailCred = EmailAuthProvider.getCredential(email.trim(), password)
//
//            if (currentUser != null) {
//                // 2) Link email/password to the phone-authenticated user
//                currentUser.linkWithCredential(emailCred)
//                    .addOnSuccessListener { authResult ->
//                        val user = authResult.user
//                        if (user == null) {
//                            onFailure("Linking succeeded but user is null.")
//                            auth.signOut()
//                            return@addOnSuccessListener
//                        }
//
//                        // 3) Ensure Firestore user doc exists (create it if missing)
//                        val userDoc = firestore.collection("users").document(user.uid)
//                        userDoc.get()
//                            .addOnSuccessListener { snap ->
//                                if (!snap.exists()) {
//                                    val userData = hashMapOf(
//                                        "email" to email.trim(),
//                                        "phone" to (user.phoneNumber ?: ""),
//                                        "role" to "user"
//                                    )
//                                    userDoc.set(userData)
//                                        .addOnSuccessListener {
//                                            // 4) Send verification email
//                                            user.sendEmailVerification()
//                                                .addOnCompleteListener { vTask ->
//                                                    if (vTask.isSuccessful) {
//                                                        auth.signOut()
//                                                        onSuccess()
//                                                    } else {
//                                                        onFailure("Failed to send verification email: ${vTask.exception?.message}")
//                                                    }
//                                                }
//                                        }
//                                        .addOnFailureListener { e ->
//                                            onFailure("Failed to save user data: ${e.message}")
//                                        }
//                                } else {
//                                    // doc exists -> still send verification and finish
//                                    user.sendEmailVerification()
//                                        .addOnCompleteListener { vTask ->
//                                            if (vTask.isSuccessful) {
//                                                auth.signOut()
//                                                onSuccess()
//                                            } else {
//                                                onFailure("Failed to send verification email: ${vTask.exception?.message}")
//                                            }
//                                        }
//                                }
//                            }
//                            .addOnFailureListener { e ->
//                                onFailure("Failed to check user document: ${e.message}")
//                            }
//                    }
//                    .addOnFailureListener { e ->
//                        // Collision: email already in use
//                        if (e is FirebaseAuthUserCollisionException) {
//                            onFailure("This email is already registered. Please login with that email or recover the account.")
//                        } else {
//                            onFailure("Failed to link email: ${e.message}")
//                        }
//                        auth.signOut()
//                    }
//            } else {
//                // Rare fallback: no currentUser after phone sign-in. Check email and create account if safe
//                auth.fetchSignInMethodsForEmail(email.trim())
//                    .addOnCompleteListener { fetchTask ->
//                        if (!fetchTask.isSuccessful) {
//                            onFailure("Failed to check email: ${fetchTask.exception?.message}")
//                            return@addOnCompleteListener
//                        }
//                        val providers = fetchTask.result?.signInMethods ?: emptyList()
//                        if (providers.isNotEmpty()) {
//                            onFailure("Email already in use. Try logging in with that email.")
//                            return@addOnCompleteListener
//                        }
//
//                        // Safe to create the email account (fallback)
//                        auth.createUserWithEmailAndPassword(email.trim(), password)
//                            .addOnSuccessListener { createResult ->
//                                val createdUser = createResult.user
//                                if (createdUser == null) {
//                                    onFailure("User creation failed.")
//                                    return@addOnSuccessListener
//                                }
//                                val userDoc = firestore.collection("users").document(createdUser.uid)
//                                val userData = hashMapOf(
//                                    "email" to email.trim(),
//                                    "phone" to (createdUser.phoneNumber ?: ""),
//                                    "role" to "user"
//                                )
//                                userDoc.set(userData)
//                                    .addOnSuccessListener {
//                                        createdUser.sendEmailVerification()
//                                            .addOnCompleteListener { vTask ->
//                                                if (vTask.isSuccessful) {
//                                                    auth.signOut()
//                                                    onSuccess()
//                                                } else {
//                                                    onFailure("Failed to send verification email: ${vTask.exception?.message}")
//                                                }
//                                            }
//                                    }
//                                    .addOnFailureListener { e ->
//                                        onFailure("Failed to save user data: ${e.message}")
//                                    }
//                            }
//                            .addOnFailureListener { e ->
//                                onFailure("Failed to create user: ${e.message}")
//                            }
//                    }
//            }
//        }
//}
