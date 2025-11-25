package com.chat.safeplay.setting.manager

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.chat.safeplay.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDeletionScreen(
    navController: NavController,
    viewModel: AccountDeletionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val deletionCompleted by viewModel.deletionCompleted.collectAsState()
    val context = LocalContext.current

    // 🔄 All side-effects (toast, signOut, navigation) are handled here
    LaunchedEffect(deletionCompleted) {
        if (deletionCompleted) {
            Toast.makeText(context, "Account permanently deleted.", Toast.LENGTH_LONG).show()

            // Sign out locally
            FirebaseAuth.getInstance().signOut()

            // Small delay just for smooth UX (optional)
            delay(600)

            // Navigate to home & clear backstack
            navController.navigate("home") {
                popUpTo(0)
            }
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "SafePlay",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = Color.Transparent
                        ) {
                            // 🔁 Same animated video logo as before
                            VideoLogo(
                                resId = R.raw.account_delete,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Account DELETION",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (state.isDeleteRequested) {
                    // 🔴 Light-red timer layout (unchanged)
                    TimerInfoLightRed(
                        remainingTime = state.remainingTime,
                        scheduledTime = state.scheduledDeletionTime
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = {
                            vibrator.vibrate(VibrationEffect.createOneShot(40, 50))
                            viewModel.cancelDeletionProcess()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C853)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            "CANCEL ACCOUNT DELETION",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    Text(
                        text = "Once you delete your account, all your data and messages will be permanently deleted after 24 hours unless cancelled.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = {
                            vibrator.vibrate(VibrationEffect.createOneShot(35, 50))
                            showDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            "DELETE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Confirm Deletion") },
                    text = {
                        Text(
                            "Are you sure you want to delete your account? It will be deleted in 24 hours if not cancelled."
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            vibrator.vibrate(VibrationEffect.createOneShot(30, 60))
                            viewModel.startDeletionProcess()
                            showDialog = false
                        }) {
                            Text("Confirm", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    )
}

@Composable
fun TimerInfoLightRed(
    remainingTime: String,
    scheduledTime: String
) {
    val lightRed = Color(0xFFFF7B7B)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = "Time left until deletion:",
            fontSize = 15.sp,
            color = lightRed.copy(alpha = 0.85f),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = remainingTime,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = lightRed,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Deletion at: $scheduledTime",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = lightRed.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "After this time, your account and chats will be permanently deleted.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}























//package com.chat.safeplay.setting.manager
//
//import android.os.Build
//import android.os.VibrationEffect
//import android.os.Vibrator
//import android.widget.Toast
//import androidx.annotation.RequiresApi
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavController
//import com.chat.safeplay.R
//import com.google.firebase.auth.FirebaseAuth
//import kotlinx.coroutines.delay
//
//@RequiresApi(Build.VERSION_CODES.O)
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AccountDeletionScreen(
//    navController: NavController,
//    viewModel: AccountDeletionViewModel = viewModel()
//) {
//    val state by viewModel.state.collectAsState()
//
//    val context = LocalContext.current
//    val deletionCompleted by viewModel.deletionCompleted.collectAsState()
//
//    if (deletionCompleted) {
//        // 🔥 React when ViewModel finishes deletion
//        Toast.makeText(context, "Account permanently deleted.", Toast.LENGTH_LONG).show()
//
//        // Sign out locally
//        FirebaseAuth.getInstance().signOut()
//
//        // Navigate to home screen
//        navController.navigate("home") {
//            popUpTo(0)
//        }
//    }
//
//
//
//    var showDialog by remember { mutableStateOf(false) }
//    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
//
//    // Navigate when deletion completes
//    LaunchedEffect(deletionCompleted) {
//        if (deletionCompleted) {
//            delay(600)
//            navController.navigate("home") {
//                popUpTo(0)
//            }
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            SmallTopAppBar(
//                title = {
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        Text(
//                            "SafePlay",
//                            color = MaterialTheme.colorScheme.onBackground,
//                            fontSize = 19.sp,
//                            fontWeight = FontWeight.SemiBold
//                        )
//                        Spacer(Modifier.width(8.dp))
//                        Surface(
//                            modifier = Modifier.size(36.dp),
//                            shape = CircleShape,
//                            color = Color.Transparent
//                        ) {
//                            VideoLogo(
//                                resId = R.raw.account_delete,
//                                modifier = Modifier.fillMaxSize()
//                            )
//                        }
//                    }
//                },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        },
//        content = { innerPadding ->
//            Column(
//                modifier = Modifier
//                    .padding(innerPadding)
//                    .padding(horizontal = 24.dp, vertical = 32.dp)
//                    .fillMaxSize(),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//
//                Text(
//                    text = "Account DELETION",
//                    color = MaterialTheme.colorScheme.error,
//                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
//                )
//
//                Spacer(modifier = Modifier.height(32.dp))
//
//                if (state.isDeleteRequested) {
//                    // 🔴 Simple light-red timer layout
//                    TimerInfoLightRed(
//                        remainingTime = state.remainingTime,
//                        scheduledTime = state.scheduledDeletionTime
//                    )
//
//                    Spacer(modifier = Modifier.height(40.dp))
//
//                    Button(
//                        onClick = {
//                            vibrator.vibrate(VibrationEffect.createOneShot(40, 50))
//                            viewModel.cancelDeletionProcess()
//
//                        },
//                        shape = RoundedCornerShape(12.dp),
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = Color(0xFF00C853)
//                        ),
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(50.dp)
//                    ) {
//                        Text(
//                            "CANCEL ACCOUNT DELETION",
//                            fontWeight = FontWeight.Bold,
//                            color = MaterialTheme.colorScheme.onPrimary
//                        )
//                    }
//                } else {
//                    Text(
//                        text = "Once you delete your account, all your data and messages will be permanently deleted after 24 hours unless cancelled.",
//                        fontSize = 14.sp,
//                        textAlign = TextAlign.Center,
//                        color = MaterialTheme.colorScheme.onBackground
//                    )
//
//                    Spacer(modifier = Modifier.height(40.dp))
//
//                    Button(
//                        onClick = {
//                            vibrator.vibrate(VibrationEffect.createOneShot(35, 50))
//                            showDialog = true
//                        },
//                        shape = RoundedCornerShape(12.dp),
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = MaterialTheme.colorScheme.error
//                        ),
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(50.dp)
//                    ) {
//                        Text(
//                            "DELETE",
//                            fontWeight = FontWeight.Bold,
//                            fontSize = 16.sp,
//                            color = MaterialTheme.colorScheme.onPrimary
//                        )
//                    }
//                }
//            }
//
//            if (showDialog) {
//                AlertDialog(
//                    onDismissRequest = { showDialog = false },
//                    title = { Text("Confirm Deletion") },
//                    text = { Text("Are you sure you want to delete your account? It will be deleted in 24 hours if not cancelled.") },
//                    confirmButton = {
//                        TextButton(onClick = {
//                            vibrator.vibrate(VibrationEffect.createOneShot(30, 60))
//                            viewModel.startDeletionProcess()
//
//                            showDialog = false
//                        }) {
//                            Text("Confirm", color = MaterialTheme.colorScheme.error)
//                        }
//                    },
//                    dismissButton = {
//                        TextButton(onClick = { showDialog = false }) {
//                            Text("Cancel")
//                        }
//                    }
//                )
//            }
//        }
//    )
//}
//
//@Composable
//fun TimerInfoLightRed(
//    remainingTime: String,
//    scheduledTime: String
//) {
//    // Light red tone
//    val lightRed = Color(0xFFFF7B7B)
//
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 12.dp)
//    ) {
//        Text(
//            text = "Time left until deletion:",
//            fontSize = 15.sp,
//            color = lightRed.copy(alpha = 0.85f),
//            fontWeight = FontWeight.Medium,
//            textAlign = TextAlign.Center
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        Text(
//            text = remainingTime,
//            fontSize = 36.sp,
//            fontWeight = FontWeight.ExtraBold,
//            color = lightRed,
//            textAlign = TextAlign.Center
//        )
//
//        Spacer(modifier = Modifier.height(10.dp))
//
//        Text(
//            text = "Deletion at: $scheduledTime",
//            fontSize = 14.sp,
//            fontWeight = FontWeight.Medium,
//            color = lightRed.copy(alpha = 0.85f),
//            textAlign = TextAlign.Center
//        )
//
//        Spacer(modifier = Modifier.height(4.dp))
//
//        Text(
//            text = "After this time, your account and chats will be permanently deleted.",
//            fontSize = 13.sp,
//            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
//            textAlign = TextAlign.Center
//        )
//    }
//}
//
//
//
//
