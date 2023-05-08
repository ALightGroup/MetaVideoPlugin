package com.alg.meta.plugin.video

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.AudioSink
import com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.video.VideoRendererEventListener
import java.util.ArrayList

/**
 * @Author laoyuyu
 * @Description
 * @Date 9:43 PM 2023/4/26
 **/
class AlgFFmpegRenderFactory(context:Context): DefaultRenderersFactory(context) {
  init {
    setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)
  }


  override fun buildAudioRenderers(
    context: Context,
    extensionRendererMode: Int,
    mediaCodecSelector: MediaCodecSelector,
    enableDecoderFallback: Boolean,
    audioSink: AudioSink,
    eventHandler: Handler,
    eventListener: AudioRendererEventListener,
    out: ArrayList<Renderer>
  ) {
    out.add(FfmpegAudioRenderer())
    super.buildAudioRenderers(
      context,
      extensionRendererMode,
      mediaCodecSelector,
      enableDecoderFallback,
      audioSink,
      eventHandler,
      eventListener,
      out
    )
  }
}