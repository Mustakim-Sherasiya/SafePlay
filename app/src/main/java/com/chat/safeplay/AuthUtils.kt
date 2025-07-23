package com.chat.safeplay

import android.content.Context
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

fun sendPhoneOtp(
phoneNumber: String,
context: Context,
onCodeSent: (String) -> Unit,
onFailure: (String) -> Unit
) {
    val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
        .setPhoneNumber(phoneNumber)
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(context as android.app.Activity)
        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Optional: Auto verification callback
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
}

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
    auth.signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // OTP verified — now create user with email & password
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        it.user?.sendEmailVerification()
                        onSuccess()
                    }
                    .addOnFailureListener {
                        onFailure("Account creation failed: ${it.message}")
                    }
            } else {
                onFailure("Invalid OTP or verification failed.")
            }
        }
}