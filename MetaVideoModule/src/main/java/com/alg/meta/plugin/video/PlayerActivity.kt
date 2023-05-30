package com.alg.meta.plugin.video

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
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
import kotlin.math.abs

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
  val audioService = remember {
    context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  }

  val maxAudioVolume = remember {
    audioService?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?:0
  }
  var volume = remember {
    audioService?.getStreamVolume(AudioManager.STREAM_MUSIC)?:0
  }

  var screenBrightness = remember {
    (context as? Activity)?.window?.attributes?.screenBrightness?:0f
  }

  val window = remember {
    (context as? Activity)?.window
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
             },
             audioVolumeChange = {offset,changeAbs->
               if (abs(offset)>5.dp.value) {
                 Box(Modifier.fillMaxSize(), Alignment.Center) {
                   Slider(
                     modifier = Modifier
                       .align(Alignment.Center)
                       .size(160.dp, 2.dp),
                     value = volume.toFloat(),
                     onValueChange = {
                     },
                     valueRange = 0f..maxAudioVolume.toFloat()
                   )
                 }
                 var value = (maxAudioVolume*changeAbs).toInt()
                 if (value<0){
                   value = 0
                 }else if (value>maxAudioVolume){
                   value = maxAudioVolume
                 }
                 Log.e("www","++++++setStreamVolume")
                 audioService?.setStreamVolume(AudioManager.STREAM_MUSIC, volume,0)
                 volume= value
               }
             },
             brightnessChange = {offset,changeAbs->
               if (abs(offset)>5.dp.value) {

                 Box(Modifier.fillMaxSize(), Alignment.Center) {
                   Slider(
                     modifier = Modifier
                       .align(Alignment.Center)
                       .size(160.dp, 2.dp),
                     value = screenBrightness,
                     onValueChange = {
                     },
                     valueRange = 0f..255f
                   )
                 }
                 var value = 255*changeAbs
                 if (value<0){
                   value = 0f
                 }else if (value>255){
                   value = 255f
                 }
                 screenBrightness = value
                 window?.attributes?.screenBrightness = value
                 window?.attributes = window?.attributes
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
