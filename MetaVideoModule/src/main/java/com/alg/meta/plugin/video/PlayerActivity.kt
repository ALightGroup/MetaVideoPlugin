package com.alg.meta.plugin.video

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alg.meta.plugin.metaframe.base.BaseActivity

/**
 * @Author laoyuyu
 * @Description
 * @Date 8:38 PM 2023/3/8
 **/
class PlayerActivity:BaseActivity() {
  override fun initData(savedInstanceState: Bundle?) {
    setContent {
      Greeting("123")
    }
  }
}

@Composable
fun Greeting(name: String) {
  Text (text = "Hello $name!11wdsdsdss")
}

@Preview
@Composable
fun PreviewGreeting() {
  Greeting("Android")
}