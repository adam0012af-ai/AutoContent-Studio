package com.adam.iptvplayer

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import java.util.concurrent.Executors

class LiveActivity : AppCompatActivity() {
    private val io=Executors.newFixedThreadPool(3)
    private lateinit var repo:IptvRepository
    private var all=emptyList<IptvRepository.Item>()
    private var category="all"; private var search=""; private var chosen:IptvRepository.Item?=null
    private var previewPlayer:ExoPlayer?=null
    private lateinit var adapter:ChannelAdapter; private lateinit var playerView:PlayerView
    private lateinit var title:TextView; private lateinit var logo:ImageView; private lateinit var now:TextView; private lateinit var next:TextView
    private val bg=Color.rgb(5,7,12); private val panel=Color.rgb(15,18,27); private val soft=Color.rgb(27,32,44); private val gold=Color.rgb(229,184,92)

    override fun onCreate(b:Bundle?){super.onCreate(b);val p=getSharedPreferences("iptv_profile_v1",MODE_PRIVATE);val s=p.getString("server","").orEmpty();val u=p.getString("user","").orEmpty();val pw=p.getString("pass","").orEmpty();if(s.isBlank()||u.isBlank()||pw.isBlank())return finish();repo=IptvRepository(s,u,pw);ui();load()}

    private fun ui(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(14),dp(10),dp(14),dp(10))}
        val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};val back=text("‹",28,true,Color.WHITE).apply{gravity=Gravity.CENTER;background=round(soft,14);setOnClickListener{finish()}};top.addView(back,LinearLayout.LayoutParams(dp(50),dp(44)));top.addView(text("Live TV",25,true,Color.WHITE),LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(14)})
        val q=EditText(this).apply{hint="Search channels";setHintTextColor(Color.rgb(100,108,125));setTextColor(Color.WHITE);setSingleLine();textSize=14f;background=round(soft,14);setPadding(dp(15),0,dp(15),0);addTextChangedListener(object:TextWatcher{override fun afterTextChanged(e:Editable?){search=e?.toString().orEmpty();filter()};override fun beforeTextChanged(s:CharSequence?,a:Int,c:Int,d:Int){};override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}})};top.addView(q,LinearLayout.LayoutParams(dp(300),dp(44)));root.addView(top)
        val body=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};root.addView(body,LinearLayout.LayoutParams(-1,0,1f).apply{topMargin=dp(10)})
        val cats=RecyclerView(this).apply{layoutManager=LinearLayoutManager(this@LiveActivity);clipToPadding=false};body.addView(cats,LinearLayout.LayoutParams(dp(205),-1).apply{marginEnd=dp(8)})
        adapter=ChannelAdapter{select(it,true)};val channels=RecyclerView(this).apply{layoutManager=LinearLayoutManager(this@LiveActivity);adapter=this@LiveActivity.adapter;clipToPadding=false};body.addView(channels,LinearLayout.LayoutParams(0,-1,1f).apply{marginEnd=dp(10)})
        val side=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(12),dp(12),dp(12));background=round(panel,18)};body.addView(side,LinearLayout.LayoutParams(0,-1,1.18f))
        playerView=PlayerView(this).apply{useController=false;resizeMode=AspectRatioFrameLayout.RESIZE_MODE_FIT;setShutterBackgroundColor(Color.BLACK);setBackgroundColor(Color.BLACK)};side.addView(playerView,LinearLayout.LayoutParams(-1,0,1f))
        val info=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(0,dp(10),0,0)};logo=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_INSIDE;background=round(soft,12)};info.addView(logo,LinearLayout.LayoutParams(dp(62),dp(62)).apply{marginEnd=dp(10)});val words=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};title=text("Select a channel",18,true,Color.WHITE);now=text("Now • —",12,false,Color.rgb(180,186,199));next=text("Next • —",12,false,Color.rgb(125,132,149));words.addView(title);words.addView(now);words.addView(next);info.addView(words,LinearLayout.LayoutParams(0,-2,1f));val full=text("FULLSCREEN  ›",11,true,Color.BLACK).apply{gravity=Gravity.CENTER;background=round(gold,18);setOnClickListener{chosen?.let{openFull(it)}}};info.addView(full,LinearLayout.LayoutParams(dp(125),dp(42)));side.addView(info)
        io.execute{try{val c=repo.categories("live");runOnUiThread{cats.adapter=CategoryAdapter(c){category=it.id;filter()}}}catch(_:Exception){}}
    }

    private fun load(){io.execute{try{val data=repo.items("live");runOnUiThread{all=data;filter();data.firstOrNull()?.let{select(it,false)}}}catch(_:Exception){runOnUiThread{Toast.makeText(this,"Unable to load live channels",Toast.LENGTH_LONG).show()}}}}
    private fun filter(){if(!::adapter.isInitialized)return;adapter.replace(all.filter{(category=="all"||it.categoryId==category)&&it.name.contains(search,true)})}
    private fun select(i:IptvRepository.Item,play:Boolean){
        chosen=i;title.text=i.name
        if(i.icon.isNullOrBlank())logo.setImageDrawable(null)else logo.load(i.icon){crossfade(true)}
        now.text="Now • Loading guide…";next.text="Next • —"
        io.execute{try{val guide=repo.shortEpg(i.id);runOnUiThread{if(chosen?.id==i.id){now.text="Now • ${guide.getOrNull(0)?.title?.ifBlank{"No guide data"}?:"No guide data"}";next.text="Next • ${guide.getOrNull(1)?.title?.ifBlank{"—"}?:"—"}"}}}catch(_:Exception){runOnUiThread{if(chosen?.id==i.id)now.text="Now • No guide data"}}}
        if(play){previewPlayer?.release();previewPlayer=ExoPlayer.Builder(this).build().also{p->playerView.player=p;p.setMediaItem(MediaItem.fromUri(repo.streamUrl(i)));p.prepare();p.playWhenReady=true;p.volume=.35f}}
    }
    private fun openFull(i:IptvRepository.Item){startActivity(Intent(this,PlayerActivity::class.java).putExtra("url",repo.streamUrl(i)).putExtra("title",i.name))}
    override fun onStop(){super.onStop();previewPlayer?.release();previewPlayer=null}
    override fun onDestroy(){super.onDestroy();io.shutdownNow()}
    private fun text(s:String,z:Int,b:Boolean,c:Int)=TextView(this).apply{text=s;textSize=z.toFloat();setTextColor(c);if(b)typeface=Typeface.DEFAULT_BOLD}
    private fun round(c:Int,r:Int)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat()};private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    private inner class CategoryAdapter(private val data:List<IptvRepository.Category>,private val click:(IptvRepository.Category)->Unit):RecyclerView.Adapter<CategoryAdapter.H>(){
        inner class H(val t:TextView):RecyclerView.ViewHolder(t)
        override fun onCreateViewHolder(p:ViewGroup,v:Int)=H(text("",12,true,Color.WHITE).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),0,dp(10),0);background=round(soft,13)})
        override fun onBindViewHolder(h:H,p:Int){val i=data[p];h.t.text=i.name;h.t.setOnClickListener{click(i)};h.t.layoutParams=RecyclerView.LayoutParams(-1,dp(48)).apply{bottomMargin=dp(6)}};override fun getItemCount()=data.size
    }
    private inner class ChannelAdapter(private val click:(IptvRepository.Item)->Unit):RecyclerView.Adapter<ChannelAdapter.H>(){
        private val data=mutableListOf<IptvRepository.Item>();inner class H(val r:LinearLayout,val im:ImageView,val n:TextView):RecyclerView.ViewHolder(r)
        override fun onCreateViewHolder(p:ViewGroup,v:Int):H{val r=LinearLayout(this@LiveActivity).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),dp(6),dp(8),dp(6));background=round(panel,13);isFocusable=true};val im=ImageView(this@LiveActivity).apply{scaleType=ImageView.ScaleType.CENTER_INSIDE;background=round(soft,10)};val n=text("",13,true,Color.WHITE).apply{setPadding(dp(10),0,dp(6),0);maxLines=2};r.addView(im,LinearLayout.LayoutParams(dp(50),dp(50)));r.addView(n,LinearLayout.LayoutParams(0,-2,1f));r.addView(text("›",24,true,gold).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(28),dp(50)));return H(r,im,n)}
        override fun onBindViewHolder(h:H,p:Int){val i=data[p];h.n.text=i.name;if(i.icon.isNullOrBlank())h.im.setImageDrawable(null)else h.im.load(i.icon){crossfade(true)};h.r.setOnClickListener{click(i)};h.r.layoutParams=RecyclerView.LayoutParams(-1,dp(64)).apply{bottomMargin=dp(6)}};override fun getItemCount()=data.size;fun replace(x:List<IptvRepository.Item>){data.clear();data.addAll(x);notifyDataSetChanged()}
    }
}
