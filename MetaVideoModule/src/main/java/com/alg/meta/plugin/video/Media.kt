package com.alg.meta.plugin.video

import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.alg.meta.plugin.video.ControllerVisibility.Invisible
import com.alg.meta.plugin.video.ControllerVisibility.PartiallyVisible
import com.alg.meta.plugin.video.ControllerVisibility.Visible
import com.alg.meta.plugin.video.ShowBuffering.Always
import com.alg.meta.plugin.video.ShowBuffering.WhenPlaying
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.text.CueGroup
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class SurfaceType {
  None,
  SurfaceView,
  TextureView;
}

enum class ShowBuffering {
  Never,
  WhenPlaying,
  Always;
}

@Composable
fun Media(state: MediaState,
  modifier: Modifier = Modifier,
  surfaceType: SurfaceType = SurfaceType.SurfaceView,
  resizeMode: ResizeMode = ResizeMode.Fit,
  shutterColor: Color = Color.Black,
  keepContentOnPlayerReset: Boolean = false,
  useArtwork: Boolean = true,
  defaultArtworkPainter: Painter? = null,
  subtitles: @Composable ((CueGroup) -> Unit)? = null, // TODO
  showBuffering: ShowBuffering = ShowBuffering.Never,
  buffering: @Composable (() -> Unit)? = null,
  errorMessage: @Composable ((PlaybackException) -> Unit)? = null,
  overlay: @Composable (() -> Unit)? = null,
  controllerHideOnTouch: Boolean = true,
  controllerAutoShow: Boolean = true,
  controller: @Composable ((MediaState) -> Unit)? = null,
  audioVolumeChange:@Composable ((Float,Float) -> Unit)? = null,
  brightnessChange:@Composable ((Float,Float) -> Unit)? = null){

  if (showBuffering != ShowBuffering.Never) require(buffering != null) {
    "buffering should not be null if showBuffering is 'ShowBuffering.$showBuffering'"
  }
  LaunchedEffect(Unit) {
    snapshotFlow { state.contentAspectRatioRaw }
      .collect { contentAspectRatioRaw ->
        state.contentAspectRatio = contentAspectRatioRaw
      }
  }

  SideEffect {
    state.controllerAutoShow = controllerAutoShow
  }
  var offset by remember { mutableStateOf(0f) }
  val isVolume = remember { mutableStateOf(false)}
  var boxRect = remember { Rect.Zero }

  Box(
    modifier = modifier
      .onGloballyPositioned {
        boxRect = it.boundsInParent()
      }
      .clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() }
      ) {
        if (controller != null && state.player != null) {
          state.controllerVisibility = when (state.controllerVisibility) {
            Visible -> {
              if (controllerHideOnTouch) Invisible
              else Visible
            }
            PartiallyVisible -> Visible
            Invisible -> Visible
          }
        }
      }
      .pointerInput(Unit) {
        detectVerticalDragGestures(
          onDragStart = {

          },
          onDragEnd = {
            offset = 0f
          }
        ) { change, dragAmount ->
          isVolume.value = change.position.x>(boxRect.width/2)
          offset += dragAmount
        }
      }
  ){
    // this is video
    Box(modifier = Modifier
      .align(Alignment.Center)
      .run {
        if (state.contentAspectRatio <= 0) fillMaxSize()
        else resize(state.contentAspectRatio, resizeMode)
      }
    ){

      VideoSurface(
        state = state,
        surfaceType = surfaceType,
        modifier = Modifier
          .testTag(TestTag_VideoSurface)
          .fillMaxSize()
      )

      // shutter
      if (state.closeShutter) {
        Spacer(
          modifier = Modifier
            .testTag(TestTag_Shutter)
            .fillMaxSize()
            .background(shutterColor)
        )
      }

      LaunchedEffect(keepContentOnPlayerReset) {
        snapshotFlow { state.isVideoTrackSelected }
          .collect { isVideoTrackSelected ->
            when (isVideoTrackSelected) {
              // non video track is selected, so the shutter must be closed
              false -> state.closeShutter = true
              // no track
              // If keepContentOnPlayerReset is false, close shutter
              // Otherwise, open it
              null -> state.closeShutter = !keepContentOnPlayerReset
              true -> {}
            }
          }
      }

      LaunchedEffect(Unit) {
        snapshotFlow { state.player }
          .collect { player ->
            val isNewPlayer = player != null
            if (isNewPlayer && !keepContentOnPlayerReset) {
              // hide any video from the previous player.
              state.closeShutter = true
            }
          }
      }

    }

    val value = 180.dp.value
    val changeAbs = min(abs(offset),value) /value
    if (offset>0){
      if (isVolume.value){
        audioVolumeChange?.invoke(offset,-changeAbs)
      }else{
        brightnessChange?.invoke(offset,-changeAbs)
      }

    }else{
      if (isVolume.value){
        audioVolumeChange?.invoke(offset,changeAbs)
      }else{
        brightnessChange?.invoke(offset,changeAbs)
      }
    }

   // artwork in audio stream
    val artworkPainter = when {
      // non video track is selected, can use artwork
      state.isVideoTrackSelected == false -> {
        if (!useArtwork) null
        else state.artworkPainter ?: defaultArtworkPainter
      }
      keepContentOnPlayerReset -> state.usingArtworkPainter
      else -> null
    }
    if (artworkPainter != null) {
      Image(
        painter = artworkPainter,
        contentDescription = null,
        modifier = Modifier
          .testTag(TestTag_Artwork)
          .fillMaxSize(),
        contentScale = resizeMode.contentScale
      )
    }

    SideEffect {
      state.usingArtworkPainter = artworkPainter
    }

    // subtitles
    if (subtitles != null) {
      val cues = state.playerState?.cues ?: CueGroup.EMPTY_TIME_ZERO
      subtitles(cues)
    }

    // buffering
    val isBufferingShowing by remember(showBuffering) {
      derivedStateOf {
        state.playerState?.run {
          playbackState == Player.STATE_BUFFERING
            && (showBuffering == Always
            || (showBuffering == WhenPlaying && playWhenReady))
        } ?: false
      }
    }

    if (isBufferingShowing) buffering?.invoke()

    // error message
    if (errorMessage != null) {
      state.playerError?.run { errorMessage(this) }
    }
    // overlay
    overlay?.invoke()
    // controller
    if (controller != null) {
      LaunchedEffect(Unit) {
        snapshotFlow { state.player }.collect { player ->
          if (player != null) {
            state.maybeShowController()
          }
        }
      }
      controller(state)
    }
  }
}

@Composable
private fun VideoSurface(
  state: MediaState,
  surfaceType: SurfaceType,
  modifier: Modifier
) {
  val context = LocalContext.current
  key(surfaceType, context) {
    if (surfaceType != SurfaceType.None) {
      fun Player.clearVideoView(view: View) {
        when (surfaceType) {
          SurfaceType.None -> throw IllegalStateException()
          SurfaceType.SurfaceView -> clearVideoSurfaceView(view as SurfaceView)
          SurfaceType.TextureView -> clearVideoTextureView(view as TextureView)
        }
      }

      fun Player.setVideoView(view: View) {
        when (surfaceType) {
          SurfaceType.None -> throw IllegalStateException()
          SurfaceType.SurfaceView -> setVideoSurfaceView(view as SurfaceView)
          SurfaceType.TextureView -> setVideoTextureView(view as TextureView)
        }
      }

      val videoView = remember {
        when (surfaceType) {
          SurfaceType.None -> throw IllegalStateException()
          SurfaceType.SurfaceView -> SurfaceView(context)
          SurfaceType.TextureView -> TextureView(context)
        }
      }
      AndroidView(
        factory = { videoView },
        modifier = modifier,
      ) {
        // update player
        val currentPlayer = state.player
        val previousPlayer = it.tag as? Player
        if (previousPlayer === currentPlayer) return@AndroidView

        previousPlayer?.clearVideoView(it)

        it.tag = currentPlayer?.apply {
          setVideoView(it)
        }
      }
      DisposableEffect(Unit) {
        onDispose {
          (videoView.tag as? Player)?.clearVideoView(videoView)
        }
      }
    }
  }
}


internal const val TestTag_VideoSurface = "VideoSurface"
internal const val TestTag_Shutter = "Shutter"
internal const val TestTag_Artwork = "Artwork"