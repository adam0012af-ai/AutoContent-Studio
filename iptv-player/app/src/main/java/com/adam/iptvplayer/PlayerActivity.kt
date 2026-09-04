package com.adam.iptvplayer

import android.app.AlertDialog
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@UnstableApi
class PlayerActivity:AppCompatActivity(){
    private var player:ExoPlayer?=null
    private val handler=Handler(Looper.getMainLooper())
    private lateinit var controls:LinearLayout
    private lateinit var seek:SeekBar
    private lateinit var time:TextView
    private lateinit var playPause:TextView
    private lateinit var mute:TextView
    private lateinit var speed:TextView
    private lateinit var ratio:TextView
    private lateinit var lock:TextView
    private lateinit var playerView:PlayerView
    private var locked=false
    private var ratioMode=0
    private var speedIndex=2
    private val speeds=floatArrayOf(.5f,.75f,1f,1.25f,1.5f,2f)

    private val tick=object:Runnable{
        override fun run(){
            val p=player?:return
            val duration=p.duration
            if(duration>0){
                seek.max=1000
                seek.progress=((p.currentPosition*1000)/duration).toInt().coerceIn(0,1000)
                time.text="${fmt(p.currentPosition)}  /  ${fmt(duration)}"
            }else{
                time.text=if(p.isCurrentMediaItemLive)"LIVE" else fmt(p.currentPosition)
            }
            handler.postDelayed(this,500)
        }
    }

    override fun onCreate(b:Bundle?){
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        val root=FrameLayout(this).apply{setBackgroundColor(Color.BLACK)}
        playerView=PlayerView(this).apply{
            useController=false
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            resizeMode=AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        root.addView(playerView,FrameLayout.LayoutParams(-1,-1))

        controls=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            gravity=Gravity.BOTTOM
            setPadding(dp(24),dp(18),dp(24),dp(18))
            background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.argb(35,0,0,0),Color.argb(210,0,0,0)))
        }

        val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        val back=control("‹  BACK",13,true)
        back.setOnClickListener{finish()}
        top.addView(back,LinearLayout.LayoutParams(dp(120),dp(44)))
        val title=control(intent.getStringExtra("title")?:"Now Playing",18,true)
        top.addView(title,LinearLayout.LayoutParams(0,-2,1f))
        lock=control("LOCK",11,true).apply{gravity=Gravity.CENTER}
        lock.setOnClickListener{locked=!locked;lock.text=if(locked)"UNLOCK" else "LOCK";setControlsLocked()}
        top.addView(lock,LinearLayout.LayoutParams(dp(90),dp(44)))
        controls.addView(top)

        seek=SeekBar(this)
        controls.addView(seek,LinearLayout.LayoutParams(-1,dp(42)))

        val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        time=control("00:00",12,false)
        row.addView(time,LinearLayout.LayoutParams(0,-2,1f))
        val rewind=control("−10s",13,true).apply{gravity=Gravity.CENTER}
        playPause=control("PAUSE",13,true).apply{gravity=Gravity.CENTER;background=round(Color.WHITE,24);setTextColor(Color.BLACK)}
        val forward=control("+10s",13,true).apply{gravity=Gravity.CENTER}
        mute=control("MUTE",11,true).apply{gravity=Gravity.CENTER}
        speed=control("1.0×",11,true).apply{gravity=Gravity.CENTER}
        ratio=control("FIT",11,true).apply{gravity=Gravity.CENTER}
        val info=control("INFO",11,true).apply{gravity=Gravity.CENTER}

        row.addView(rewind,LinearLayout.LayoutParams(dp(76),dp(48)))
        row.addView(playPause,LinearLayout.LayoutParams(dp(100),dp(48)).apply{marginStart=dp(8);marginEnd=dp(8)})
        row.addView(forward,LinearLayout.LayoutParams(dp(76),dp(48)))
        row.addView(mute,LinearLayout.LayoutParams(dp(76),dp(48)).apply{marginStart=dp(12)})
        row.addView(speed,LinearLayout.LayoutParams(dp(76),dp(48)))
        row.addView(ratio,LinearLayout.LayoutParams(dp(76),dp(48)))
        row.addView(info,LinearLayout.LayoutParams(dp(76),dp(48)))
        controls.addView(row)
        root.addView(controls,FrameLayout.LayoutParams(-1,-1))
        setContentView(root)

        val url=intent.getStringExtra("url")?:return finish()
        player=ExoPlayer.Builder(this).build().also{p->
            playerView.player=p
            p.setMediaItem(MediaItem.fromUri(url))
            p.prepare()
            p.playWhenReady=true
            p.addListener(object:Player.Listener{
                override fun onIsPlayingChanged(isPlaying:Boolean){playPause.text=if(isPlaying)"PAUSE" else "PLAY"}
            })
        }

        playPause.setOnClickListener{player?.let{if(it.isPlaying)it.pause()else it.play()}}
        rewind.setOnClickListener{player?.let{it.seekTo((it.currentPosition-10000).coerceAtLeast(0))}}
        forward.setOnClickListener{player?.let{it.seekTo(it.currentPosition+10000)}}
        mute.setOnClickListener{player?.let{p->p.volume=if(p.volume>0f)0f else 1f;mute.text=if(p.volume==0f)"UNMUTE" else "MUTE"}}
        speed.setOnClickListener{player?.let{p->speedIndex=(speedIndex+1)%speeds.size;val s=speeds[speedIndex];p.setPlaybackSpeed(s);speed.text="${s}×"}}
        ratio.setOnClickListener{ratioMode=(ratioMode+1)%3;when(ratioMode){0->{playerView.resizeMode=AspectRatioFrameLayout.RESIZE_MODE_FIT;ratio.text="FIT"};1->{playerView.resizeMode=AspectRatioFrameLayout.RESIZE_MODE_FILL;ratio.text="FILL"};else->{playerView.resizeMode=AspectRatioFrameLayout.RESIZE_MODE_ZOOM;ratio.text="ZOOM"}}}
        info.setOnClickListener{showStreamInfo()}

        seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(s:SeekBar?,v:Int,from:Boolean){if(from)player?.let{p->if(p.duration>0)p.seekTo(p.duration*v/1000)}}
            override fun onStartTrackingTouch(s:SeekBar?){}
            override fun onStopTrackingTouch(s:SeekBar?){}
        })
        playerView.setOnClickListener{if(!locked)controls.visibility=if(controls.visibility==View.VISIBLE)View.GONE else View.VISIBLE}
        handler.post(tick)
    }

    private fun setControlsLocked(){
        val enabled=!locked
        seek.isEnabled=enabled
        playPause.isEnabled=enabled
        mute.isEnabled=enabled
        speed.isEnabled=enabled
        ratio.isEnabled=enabled
    }

    private fun showStreamInfo(){
        val p=player?:return
        val lines=mutableListOf<String>()
        lines += "Title: ${intent.getStringExtra("title")?:"Now Playing"}"
        lines += "Playback: ${if(p.isPlaying)"Playing" else "Paused"}"
        lines += "Speed: ${p.playbackParameters.speed}×"
        lines += "Volume: ${(p.volume*100).toInt()}%"
        lines += if(p.isCurrentMediaItemLive)"Type: Live stream" else "Type: On-demand"
        AlertDialog.Builder(this).setTitle("Playback information").setMessage(lines.joinToString("\n")).setPositiveButton("OK",null).show()
    }

    private fun control(s:String,z:Int,b:Boolean)=TextView(this).apply{text=s;textSize=z.toFloat();setTextColor(Color.WHITE);if(b)typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER_VERTICAL;isFocusable=true}
    private fun round(c:Int,r:Int)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat()}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun fmt(ms:Long):String{val t=(ms/1000).coerceAtLeast(0);val h=t/3600;val m=(t%3600)/60;val s=t%60;return if(h>0)"%02d:%02d:%02d".format(h,m,s) else "%02d:%02d".format(m,s)}
    override fun onStop(){super.onStop();handler.removeCallbacks(tick);player?.release();player=null}
}
