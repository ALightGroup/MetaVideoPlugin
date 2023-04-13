package com.alg.meta.plugin.video

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
  }

  fun onClick(v: View){
    when(v.id){
      R.id.btnPlay -> {
        startActivity(Intent(this, PlayerActivity::class.java))
      }
    }
  }
}