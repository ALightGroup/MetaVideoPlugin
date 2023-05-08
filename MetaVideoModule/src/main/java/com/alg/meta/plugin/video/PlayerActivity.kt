package com.alg.meta.plugin.video

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import com.alg.meta.plugin.metaframe.base.BaseActivity
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.ffmpeg.FfmpegLibrary
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource

/**
 * @Author laoyuyu
 * @Description
 * @Date 8:38 PM 2023/3/8
 **/
class PlayerActivity : BaseActivity() {
  override fun initData(savedInstanceState: Bundle?) {
    setContent {
      // Greeting("123")
      exoUi()
    }
  }
}

@Composable
fun exoUi(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val exoPlayer = remember {
    Log.d("Check", "======${FfmpegLibrary.isAvailable()}")
    ExoPlayer.Builder(context, AlgFFmpegRenderFactory(context).apply {
      setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
    }).build().apply {
      this.prepare()
    }
  }

  val mediaSource = remember {
    val httpDataSource =
      DefaultHttpDataSource.Factory()
        .setConnectTimeoutMs(5 * 1000)
        .setReadTimeoutMs(5 * 1000)



    val mediaItem =
      MediaItem.Builder()
        .setUri("http://devimages.apple.com/iphone/samples/bipbop/bipbopall.m3u8")
        .build()

    // HlsMediaSource.Factory(httpDataSource)
    //   .createMediaSource(mediaItem)

    // ProgressiveMediaSource.Factory {
    //   hlsMediaDataSource
    // }.createMediaSource(mediaItem)

    // 本地视频
    // val fileMediaItem: MediaItem = MediaItem.fromUri(Uri.parse("${context.filesDir.path}/test.avi"))
    val fileMediaItem: MediaItem = MediaItem.fromUri(Uri.parse("${context.filesDir.path}/1681871925088636.avi"))

    DefaultMediaSourceFactory(context).createMediaSource(fileMediaItem)

  }

  exoPlayer.setMediaSource(mediaSource)
  exoPlayer.prepare()

  ConstraintLayout(modifier = modifier) {
    val (title, videoPlayer) = createRefs()
    // video title
    Text(
      text = "Current Title",
      color = Color.White,
      modifier =
      Modifier.padding(16.dp)
        .fillMaxWidth()
        .wrapContentHeight()
        .constrainAs(title) {
          top.linkTo(parent.top)
          start.linkTo(parent.start)
          end.linkTo(parent.end)
        }
    )

    // player view
    DisposableEffect(
      AndroidView(
        modifier =
        Modifier.testTag("VideoPlayer")
          .constrainAs(videoPlayer) {
            top.linkTo(parent.top)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            bottom.linkTo(parent.bottom)
          },
        factory = {

          // exo player view for our video player
          StyledPlayerView(context).apply {
            player = exoPlayer
            layoutParams =
              FrameLayout.LayoutParams(
                ViewGroup.LayoutParams
                  .MATCH_PARENT,
                ViewGroup.LayoutParams
                  .MATCH_PARENT
              )
          }
        }
      )
    ) {
      onDispose {
        // relase player when no longer needed
        exoPlayer.release()
      }
    }
  }
}


@Preview
@Composable
fun PreviewGreeting() {
  // Greeting("Android")
  // exoUi()
}