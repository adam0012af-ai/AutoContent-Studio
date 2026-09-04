package com.adam.iptvplayer

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val view = PlayerView(this).apply { useController = true; setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING) }
        setContentView(view)
        val url = intent.getStringExtra("url") ?: return finish()
        player = ExoPlayer.Builder(this).build().also {
            view.player = it
            it.setMediaItem(MediaItem.fromUri(url))
            it.prepare(); it.playWhenReady = true
        }
    }
    override fun onStop() { super.onStop(); player?.release(); player = null }
}
