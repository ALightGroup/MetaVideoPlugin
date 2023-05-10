package com.alg.meta.plugin.video

import android.content.Context
import android.os.Build.VERSION
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource

@Composable
fun rememberManagedExoPlayer(): State<Player?> = rememberManagedPlayer {
  val builder = ExoPlayer.Builder(it, AlgFFmpegRenderFactory(it).apply {
    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
  })
  builder.setMediaSourceFactory(ProgressiveMediaSource.Factory(DefaultDataSource.Factory(it)))
  builder.build().apply {
    playWhenReady = true
  }
}

@Composable
fun rememberManagedPlayer(
  lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
  factory: (Context) -> Player
): State<Player?> {
  val currentContext = LocalContext.current.applicationContext
  val playerManager = remember { PlayerManager { factory(currentContext) } }
  DisposableEffect(lifecycle) {
    val observer = LifecycleEventObserver { _, event ->
      when {
        (event == ON_START && VERSION.SDK_INT > 23)
          || (event == ON_RESUME && VERSION.SDK_INT <= 23) -> {
          playerManager.initialize()
        }
        (event == ON_PAUSE && VERSION.SDK_INT <= 23)
          || (event == ON_STOP && VERSION.SDK_INT > 23) -> {
          playerManager.release()
        }
      }
    }
    lifecycle.addObserver(observer)
    onDispose {
      lifecycle.removeObserver(observer)
    }
  }
  return playerManager.player
}

@Stable
internal class PlayerManager(
  private val factory: () -> Player,
) : RememberObserver {
  var player = mutableStateOf<Player?>(null)
  private var rememberedState: Triple<String, Int, Long>? = null
  private val window: Timeline.Window = Timeline.Window()

  override fun onAbandoned() {
    release()
  }

  override fun onForgotten() {
    release()
  }

  override fun onRemembered() {
  }

  internal fun initialize() {
    if (player.value != null) return
    player.value = factory().also { player ->
      player.addListener(object : Player.Listener {
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
          // recover the remembered state if media id matched
          rememberedState
            ?.let { (id, index, position) ->
              if (!timeline.isEmpty
                && timeline.windowCount > index
                && id == timeline.getWindow(index, window).mediaItem.mediaId
              ) {
                player.seekTo(index, position)
              }
            }
            ?.also { rememberedState = null }
        }
      })
    }
  }

  fun release() {
    player.value?.let { player ->
      // remember the current state before release
      player.currentMediaItem?.let { mediaItem ->
        rememberedState = Triple(
          mediaItem.mediaId,
          player.currentMediaItemIndex,
          player.currentPosition
        )
      }
      player.release()
    }
    player.value = null
  }
}