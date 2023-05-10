package com.alg.meta.plugin.video

import android.graphics.BitmapFactory
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.video.VideoSize
import kotlin.math.absoluteValue

@Composable
fun rememberMediaState(
  player: Player?
): MediaState = remember { MediaState(initPlayer = player) }.apply {
  this.player = player
}

@Stable
class MediaState(
  initPlayer: Player? = null
) {

  var player: Player?
    set(current) {
      require(current == null || current.applicationLooper == Looper.getMainLooper()) {
        "Only players which are accessed on the main thread are supported."
      }
      val previous = _player
      if (current !== previous) {
        _player = current
        onPlayerChanged(previous, current)
      }
    }
    get() = _player


  val playerState: PlayerState? get() = stateOfPlayerState.value

  var isControllerShowing: Boolean
    get() = controllerVisibility.isShowing
    set(value) {
      controllerVisibility = if (value) ControllerVisibility.Visible
      else ControllerVisibility.Invisible
    }

  var controllerVisibility: ControllerVisibility by mutableStateOf(ControllerVisibility.Invisible)

  val shouldShowControllerIndefinitely: Boolean by derivedStateOf {
    playerState?.run {
      controllerAutoShow
        && !timeline.isEmpty
        && (playbackState == Player.STATE_IDLE
        || playbackState == Player.STATE_ENDED
        || !playWhenReady)
    } ?: true
  }

  internal var controllerAutoShow: Boolean by mutableStateOf(true)

  internal fun maybeShowController() {
    if (shouldShowControllerIndefinitely) {
      controllerVisibility = ControllerVisibility.Visible
    }
  }

  // internally used properties and functions
  private val listener = object : Player.Listener {
    override fun onRenderedFirstFrame() {
      closeShutter = false
      usingArtworkPainter = null
    }

    override fun onEvents(player: Player, events: Player.Events) {
      if (events.containsAny(
          Player.EVENT_PLAYBACK_STATE_CHANGED,
          Player.EVENT_PLAY_WHEN_READY_CHANGED
        )
      ) {
        maybeShowController()
      }
    }
  }
  private var _player: Player? by mutableStateOf(initPlayer)
  private fun onPlayerChanged(previous: Player?, current: Player?) {
    previous?.removeListener(listener)
    stateOfPlayerState.value?.dispose()
    stateOfPlayerState.value = current?.state()
    current?.addListener(listener)
    if (current == null) {
      controllerVisibility = ControllerVisibility.Invisible
    }
  }

  internal val stateOfPlayerState = mutableStateOf(initPlayer?.state())

  internal val contentAspectRatioRaw by derivedStateOf {
    usingArtworkPainter?.aspectRatio
      ?: (playerState?.videoSize ?: VideoSize.UNKNOWN).aspectRatio
  }
  private var _contentAspectRatio by mutableStateOf(0f)
  internal var contentAspectRatio
    internal set(value) {
      val aspectDeformation: Float = value / contentAspectRatio - 1f
      if (aspectDeformation.absoluteValue > 0.01f) {
        // Not within the allowed tolerance, populate the new aspectRatio.
        _contentAspectRatio = value
      }
    }
    get() = _contentAspectRatio

  // true: video track is selected
  // false: non video track is selected
  // null: there isn't any track
  internal val isVideoTrackSelected: Boolean? by derivedStateOf {
    playerState?.tracks
      ?.takeIf { it.groups.isNotEmpty() }
      ?.isTypeSelected(C.TRACK_TYPE_VIDEO)
  }

  internal var closeShutter by mutableStateOf(true)

  internal val artworkPainter: BitmapPainter? by derivedStateOf {
    playerState?.mediaMetadata?.artworkData
      ?.run { BitmapFactory.decodeByteArray(this, 0, size)?.asImageBitmap() }
      ?.run { BitmapPainter(this) }
  }
  internal var usingArtworkPainter by mutableStateOf<Painter?>(null)

  internal val playerError: PlaybackException? by derivedStateOf {
    playerState?.playerError
  }

  init {
    initPlayer?.addListener(listener)
  }
}
enum class ControllerVisibility(
  val isShowing: Boolean,
) {
  /**
   * All UI controls are visible.
   */
  Visible(true),

  /**
   * A part of UI controls are visible.
   */
  PartiallyVisible(true),

  /**
   * All UI controls are hidden.
   */
  Invisible(false)
}

private val VideoSize.aspectRatio
  get() = if (height == 0) 0f else width * pixelWidthHeightRatio / height
private val Painter.aspectRatio
  get() = intrinsicSize.run {
    if (this == Size.Unspecified || width.isNaN() || height.isNaN() || height == 0f) 0f
    else width / height
  }

