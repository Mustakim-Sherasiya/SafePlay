// SettingsScreenUsingVideoView.kt
package setting.manager // adjust if your package differs

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.chat.safeplay.R
import android.view.View
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenPasswordManager: () -> Unit = {},
    onRequestPinChange: () -> Unit = {},
    onChangeChatBackground: () -> Unit = {},
    onDelayChat: () -> Unit = {},
    onRequestAccountDeletion: () -> Unit = {},
    navController: NavHostController
) {
    val ctx = LocalContext.current
    val items = listOf(
        SettingItemData("Password manager", rawResourceUri(ctx, R.raw.pass_change)),
        SettingItemData("Request PIN change", rawResourceUri(ctx, R.raw.pin_change)),
        SettingItemData("Change chat background", rawResourceUri(ctx, R.raw.change_chat_background)),
        SettingItemData("Delay Chat", rawResourceUri(ctx, R.raw.delay_chat)),
        SettingItemData("Request Account Deletion", rawResourceUri(ctx, R.raw.account_delete))
    )

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Circular container for the live mini-logo, same style as your ProfileScreen
                        Surface(
                            modifier = Modifier.size(48.dp), // match ProfileScreen outer size
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.06f),
                            tonalElevation = 0.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                // VideoLogo from your ProfileScreen (uses ExoPlayer Media3)
                                VideoLogo(
                                    resId = R.raw.small_live_logo, // replace with your top-bar mp4 raw file
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))
                        Text(
                            "SafePlay",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF121212))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            items.forEachIndexed { idx, item ->
                val clickAction = when (idx) {
                    0 -> onOpenPasswordManager
                    1 -> onRequestPinChange
                    2 -> onChangeChatBackground
                    3 -> onDelayChat
                    4 -> onRequestAccountDeletion
                    else -> {}
                }

                // Row styled like your screenshot: rounded dark tile + circular icon at left
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E1E1E))
                        // Put clickable on the Row (outer) so VideoView doesn't intercept touch.
                        .clickable {
                            // show toast with exact title
                            Toast.makeText(ctx, item.title, Toast.LENGTH_SHORT).show()
                            // then perform passed action
                            clickAction()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VideoViewIcon(uriString = item.uri ,size = 40) ///// CHANGE SIZE HERE TO CHANGE LOGO SIZE
                    Spacer(modifier = Modifier.width(7.dp))
                    Column(modifier = Modifier.weight(0.06f)) {
                        Text(
                            text = item.title,
                            color = Color(0xFFDCDCDC),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

private fun ColumnScope.clickAction() {
    TODO("Not yet implemented")
}

data class SettingItemData(val title: String, val uri: String)

/**
 * VideoLogo: uses Media3 ExoPlayer like your ProfileScreen.
 * Keep this exactly as in your ProfileScreen so the look matches.
 */
@Composable
fun VideoLogo(resId: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaUri = "android.resource://${context.packageName}/$resId"
            setMediaItem(MediaItem.fromUri(mediaUri))
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        factory = {
            PlayerView(context).apply {
                useController = false
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                this.player = player
                // ensure it doesn't show controls or intercept touches
                isClickable = false
                isFocusable = false
                setOnTouchListener { _, _ -> false }
            }
        },
        modifier = modifier
    )
}

/**
 * Circular video icon backed by VideoView + MediaPlayer.
 * - uriString: "android.resource://<pkg>/<resId>" or "https://..."
 * Keeps VideoView non-clickable so the Row click receives taps.
 */
@Composable
fun VideoViewIcon(uriString: String, size: Int = 56) {
    val context = LocalContext.current
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                val container = FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val vv = VideoView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Make sure VideoView itself doesn't intercept clicks
                    isClickable = false
                    isFocusable = false
                    setOnTouchListener { _, _ -> false }
                }

                container.addView(vv)
                videoViewRef = vv
                container
            },
            update = { container ->
                val vv = (container.getChildAt(0) as? VideoView) ?: return@AndroidView
                val uri = try {
                    Uri.parse(uriString)
                } catch (e: Exception) {
                    null
                } ?: return@AndroidView

                try { vv.stopPlayback() } catch (_: Exception) {}
                try { vv.setVideoURI(uri) } catch (e: Exception) { e.printStackTrace() }

                vv.setOnPreparedListener { mp ->
                    try {
                        mp.isLooping = true
                        mp.setVolume(0f, 0f)
                        mediaPlayerRef = mp
                    } catch (e: Exception) { e.printStackTrace() }
                    vv.start()
                }

                vv.setOnErrorListener { _, _, _ -> true }
                vv.start()
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )

        DisposableEffect(Unit) {
            onDispose {
                try { videoViewRef?.stopPlayback() } catch (_: Exception) {}
                try { mediaPlayerRef?.reset(); mediaPlayerRef?.release() } catch (_: Exception) {}
                videoViewRef = null
                mediaPlayerRef = null
            }
        }
    }
}

/**
 * Helper to build a raw resource URI:
 * e.g. "android.resource://<package>/<resId>"
 */
fun rawResourceUri(context: Context, rawResId: Int): String {
    return "android.resource://${context.packageName}/$rawResId"
}
