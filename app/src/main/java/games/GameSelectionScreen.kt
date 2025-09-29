import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.chat.safeplay.games.SafeRunnerActivity
import games.ColorMemoryActivity
import games.TapTheDotActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSelectionScreen(navController: NavHostController) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("🎮 Game Selection") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Choose a Game", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = {
                context.startActivity(Intent(context, TapTheDotActivity::class.java))
            }) {
                Text("🎯 Tap the Dot")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                context.startActivity(Intent(context, ColorMemoryActivity::class.java))
            }) {
                Text("🔴 Color Memory")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                context.startActivity(Intent(context, SafeRunnerActivity::class.java))
            }) {
                Text("🏃 Safe Runner")
            }
        }
    }
}













//package games
//
//
//
//import android.content.Intent
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavHostController
//import com.chat.safeplay.games.SafeRunnerActivity
//
//@Composable
//fun GameSelectionScreen(navController: NavHostController) {
//    val context = LocalContext.current
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(32.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text("🎮 Select a Game", fontSize = 24.sp)
//        Spacer(modifier = Modifier.height(32.dp))
//
//        Button(onClick = {
//            context.startActivity(Intent(context, TapTheDotActivity::class.java))
//        }) {
//            Text("🎯 Tap the Dot")
//        }
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Button(onClick = {
//            context.startActivity(Intent(context, ColorMemoryActivity::class.java))
//        }) {
//            Text("🔴 Color Memory")
//
//        }
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Button(onClick = {
//            context.startActivity(Intent(context, SafeRunnerActivity::class.java))
//        }) {
//            Text("🏃 Safe Runner")
//        }
//
//    }
//}
