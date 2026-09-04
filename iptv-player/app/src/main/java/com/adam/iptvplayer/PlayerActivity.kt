package com.adam.iptvplayer

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity:AppCompatActivity(){
    private var player:ExoPlayer?=null;private val h=Handler(Looper.getMainLooper());private lateinit var controls:LinearLayout;private lateinit var seek:SeekBar;private lateinit var time:TextView
    private val tick=object:Runnable{override fun run(){val p=player?:return;if(p.duration>0){seek.max=1000;seek.progress=((p.currentPosition*1000)/p.duration).toInt();time.text="${fmt(p.currentPosition)} / ${fmt(p.duration)}"};h.postDelayed(this,500)}}
    override fun onCreate(b:Bundle?){super.onCreate(b);window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val root=FrameLayout(this).apply{setBackgroundColor(Color.BLACK)};val video=PlayerView(this).apply{useController=false;setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)};root.addView(video,FrameLayout.LayoutParams(-1,-1))
        controls=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.BOTTOM;setPadding(dp(24),dp(18),dp(24),dp(18));setBackgroundColor(Color.argb(135,0,0,0))}
        val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};val back=label("‹  BACK",13,true);back.setOnClickListener{finish()};top.addView(back,LinearLayout.LayoutParams(dp(120),dp(44)));val title=label(intent.getStringExtra("title")?:"Now Playing",18,true);top.addView(title,LinearLayout.LayoutParams(0,-2,1f));controls.addView(top)
        seek=SeekBar(this);controls.addView(seek,LinearLayout.LayoutParams(-1,dp(42)));val bottom=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};time=label("00:00",12,false);bottom.addView(time,LinearLayout.LayoutParams(0,-2,1f));val rewind=label("−10",14,true).apply{gravity=Gravity.CENTER};val play=label("PAUSE",14,true).apply{gravity=Gravity.CENTER;background=round(Color.WHITE,24);setTextColor(Color.BLACK)};val forward=label("+10",14,true).apply{gravity=Gravity.CENTER};bottom.addView(rewind,LinearLayout.LayoutParams(dp(72),dp(48)));bottom.addView(play,LinearLayout.LayoutParams(dp(100),dp(48)).apply{marginStart=dp(10);marginEnd=dp(10)});bottom.addView(forward,LinearLayout.LayoutParams(dp(72),dp(48)));controls.addView(bottom)
        root.addView(controls,FrameLayout.LayoutParams(-1,-1));setContentView(root)
        val url=intent.getStringExtra("url")?:return finish();player=ExoPlayer.Builder(this).build().also{p->video.player=p;p.setMediaItem(MediaItem.fromUri(url));p.prepare();p.playWhenReady=true;p.addListener(object:Player.Listener{override fun onIsPlayingChanged(isPlaying:Boolean){play.text=if(isPlaying)"PAUSE" else "PLAY"}})}
        play.setOnClickListener{player?.let{if(it.isPlaying)it.pause()else it.play()}};rewind.setOnClickListener{player?.let{it.seekTo((it.currentPosition-10000).coerceAtLeast(0))}};forward.setOnClickListener{player?.let{it.seekTo(it.currentPosition+10000)}}
        seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,v:Int,from:Boolean){if(from)player?.let{p->if(p.duration>0)p.seekTo(p.duration*v/1000)}};override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}})
        video.setOnClickListener{controls.visibility=if(controls.visibility==View.VISIBLE)View.GONE else View.VISIBLE};h.post(tick)
    }
    private fun label(s:String,z:Int,b:Boolean)=TextView(this).apply{text=s;textSize=z.toFloat();setTextColor(Color.WHITE);if(b)typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER_VERTICAL}
    private fun round(c:Int,r:Int)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat()};private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt();private fun fmt(ms:Long):String{val t=(ms/1000).coerceAtLeast(0);return "%02d:%02d".format(t/60,t%60)}
    override fun onStop(){super.onStop();h.removeCallbacks(tick);player?.release();player=null}
}
