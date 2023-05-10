package com.alg.meta.plugin.video

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alg.meta.plugin.metaframe.base.BaseActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory

/**
 * @Author laoyuyu
 * @Description
 * @Date 8:38 PM 2023/3/8
 **/
class PlayerActivity : BaseActivity() {
  override fun initData(savedInstanceState: Bundle?) {
    setContent {
      ExoUi()
    }
  }
}

@Composable
fun ExoUi(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val surfaceType by rememberSaveable { mutableStateOf(SurfaceType.SurfaceView) }
  val resizeMode by rememberSaveable { mutableStateOf(ResizeMode.Fit) }
  val keepContentOnPlayerReset by rememberSaveable { mutableStateOf(false) }
  val useArtwork by rememberSaveable { mutableStateOf(true) }
  val showBuffering by rememberSaveable { mutableStateOf(ShowBuffering.Always) }

  val controllerHideOnTouch by rememberSaveable { mutableStateOf(true) }
  val controllerAutoShow by rememberSaveable { mutableStateOf(true) }
  val controllerType by rememberSaveable { mutableStateOf(ControllerType.Simple) }

  val playWhenReady by rememberSaveable { mutableStateOf(true) }
  val setPlayer by rememberSaveable { mutableStateOf(true) }

  val player by rememberManagedExoPlayer()
  DisposableEffect(player, playWhenReady) {
    player?.playWhenReady = playWhenReady
    onDispose {}
  }
  val mediaItem = remember {
    // 本地视频
    // val fileMediaItem: MediaItem = MediaItem.fromUri(Uri.parse("${context.filesDir.path}/test.avi"))
    //val fileMediaItem: MediaItem = MediaItem.fromUri(Uri.parse("${context.filesDir.path}/1681871925088636.avi"))
   val url = " http://devimages.apple.com/iphone/samples/bipbop/bipbopall.m3u8"
   //val url = "https://storage.googleapis.com/downloads.webmproject.org/av1/exoplayer/bbb-av1-480p.mp4"
   MediaItem.Builder().setMediaId(url).setUri(url).build()
  }
  var rememberedMediaItemIdAndPosition: Pair<String, Long>? by remember { mutableStateOf(null) }
  DisposableEffect(mediaItem, player) {
    player?.run {
      val mediaSource = DefaultMediaSourceFactory(context).createMediaSource(mediaItem)
      (player as? ExoPlayer)?.setMediaSource(mediaSource)
      rememberedMediaItemIdAndPosition?.let { (id, position) ->
        if (id == mediaItem.mediaId) seekTo(position)
      }?.also { rememberedMediaItemIdAndPosition = null }
      prepare()
    }
    onDispose {}
  }

  val state = rememberMediaState(player = player.takeIf { setPlayer })
  val content = remember {
    movableContentOf{
           Media(state = state,
             modifier = Modifier
               .aspectRatio(16f / 9f)
               .background(Color.Black),
             surfaceType = surfaceType,
             resizeMode = resizeMode,
             keepContentOnPlayerReset = keepContentOnPlayerReset,
             useArtwork = useArtwork,
             showBuffering = showBuffering,
             buffering = {
               Box(Modifier.fillMaxSize(), Alignment.Center) {
                 CircularProgressIndicator()
               }
             },
             errorMessage = { error ->
               Box(Modifier.fillMaxSize(), Alignment.Center) {
                 Text(
                   error.message ?: "",
                   modifier = Modifier
                     .background(Color(0x80808080), RoundedCornerShape(16.dp))
                     .padding(horizontal = 12.dp, vertical = 4.dp),
                   color = Color.White,
                   textAlign = TextAlign.Center
                 )
               }
             },
             controllerHideOnTouch = controllerHideOnTouch,
             controllerAutoShow = controllerAutoShow,
             controller = when (controllerType) {
               ControllerType.None -> null
               ControllerType.Simple -> @Composable { state ->
                 SimpleController(state, Modifier.fillMaxSize())
               }
               ControllerType.PlayerControlView -> @Composable { state ->
                 //PlayerControlViewController(state, Modifier.fillMaxSize())
               }
             }
           )
    }
  }

  val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
  if (!isLandscape) Column(modifier) { content() } else Row(modifier) { content() }
}

private enum class ControllerType {
  None, Simple, PlayerControlView
}
