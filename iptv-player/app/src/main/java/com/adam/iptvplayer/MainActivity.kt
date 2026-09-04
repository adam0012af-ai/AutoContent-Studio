package com.adam.iptvplayer

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.util.concurrent.Executors

class MainActivity:AppCompatActivity(){
    private val io=Executors.newFixedThreadPool(4)
    private lateinit var prefs:android.content.SharedPreferences
    private var repo:IptvRepository?=null; private var server=""; private var user=""; private var pass=""; private var mainScreen=true
    private val bg=Color.rgb(5,7,12); private val panel=Color.rgb(15,18,27); private val soft=Color.rgb(27,32,44); private val gold=Color.rgb(229,184,92)

    override fun onCreate(b:Bundle?){super.onCreate(b);prefs=getSharedPreferences("iptv_profile_v1",MODE_PRIVATE);server=prefs.getString("server","").orEmpty();user=prefs.getString("user","").orEmpty();pass=prefs.getString("pass","").orEmpty();if(server.isBlank()||user.isBlank()||pass.isBlank())login()else{repo=IptvRepository(server,user,pass);home()}}

    private fun login(){mainScreen=true
        val root=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(bg);setPadding(dp(30),dp(20),dp(30),dp(20))}
        val hero=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(24),0,dp(50),0)}
        hero.addView(t("ADAM IPTV",36,true,Color.WHITE));hero.addView(t("Premium player for your own IPTV service",16,false,Color.rgb(153,160,177)).apply{setPadding(0,dp(8),0,dp(20))});hero.addView(t("LIVE  •  MOVIES  •  SERIES",12,true,gold));root.addView(hero,LinearLayout.LayoutParams(0,-1,1.15f))
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(28),dp(22),dp(28),dp(22));background=round(panel,22)}
        box.addView(t("Sign in",26,true,Color.WHITE));box.addView(t("Use the credentials from your provider",12,false,Color.rgb(125,132,150)))
        val es=e("Server URL",server,false);val eu=e("Username",user,false);val ep=e("Password",pass,true);box.addView(es);box.addView(eu);box.addView(ep);val status=t("",12,false,Color.LTGRAY);box.addView(status);val go=btn("CONTINUE",Color.WHITE,Color.BLACK)
        go.setOnClickListener{val s=es.text.toString().trim();val u=eu.text.toString().trim();val p=ep.text.toString();if(s.isBlank()||u.isBlank()||p.isBlank()){status.text="Complete all fields";return@setOnClickListener};go.isEnabled=false;status.text="Connecting…";io.execute{try{val j=IptvRepository(s,u,p).authenticate();if((j.optJSONObject("user_info")?.optInt("auth",0)?:0)!=1)error("Account not accepted");server=s.trimEnd('/');user=u;pass=p;prefs.edit().putString("server",server).putString("user",user).putString("pass",pass).apply();repo=IptvRepository(server,user,pass);runOnUiThread{home()}}catch(x:Exception){runOnUiThread{go.isEnabled=true;status.text="Connection failed • ${x.message}"}}}}
        box.addView(go,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(12)});root.addView(box,LinearLayout.LayoutParams(0,-1,.85f));setContentView(root)
    }

    private fun home(){mainScreen=true
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(22),dp(12),dp(22),dp(10))}
        val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};top.addView(t("ADAM IPTV",20,true,Color.WHITE),LinearLayout.LayoutParams(0,-2,1f));top.addView(nav("LIVE"){openLive()});top.addView(nav("MOVIES"){browser("vod")});top.addView(nav("SERIES"){browser("series")});top.addView(nav("FAVORITES"){favorites()});top.addView(nav("SETTINGS"){settings()});root.addView(top)
        val content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};content.addView(t("Your library",27,true,Color.WHITE).apply{setPadding(0,dp(18),0,dp(3))});content.addView(t("Continue with live channels, movies and series",13,false,Color.rgb(114,121,138)).apply{setPadding(0,0,0,dp(10))});addShelf(content,"Live now","live");addShelf(content,"Movies","vod");addShelf(content,"Series","series")
        val scroll=ScrollView(this).apply{isFillViewport=true;addView(content)};root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
    }

    private fun addShelf(parent:LinearLayout,title:String,kind:String){val head=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};head.addView(t(title,18,true,Color.WHITE),LinearLayout.LayoutParams(0,-2,1f));head.addView(t("VIEW ALL  ›",11,true,gold).apply{setPadding(dp(12),dp(8),dp(12),dp(8));setOnClickListener{if(kind=="live")openLive()else browser(kind)}});parent.addView(head);val rv=RecyclerView(this).apply{layoutManager=LinearLayoutManager(this@MainActivity,RecyclerView.HORIZONTAL,false);clipToPadding=false};val ad=ContentAdapter(kind,mutableListOf(),{if(it.kind=="live")openLive()else open(it)},{toggle(it)});rv.adapter=ad;parent.addView(rv,LinearLayout.LayoutParams(-1,if(kind=="live")dp(170) else dp(265)));io.execute{try{val data=repo!!.items(kind).take(14);runOnUiThread{ad.replace(data)}}catch(_:Exception){}}}
    private fun openLive(){startActivity(Intent(this,LiveActivity::class.java))}

    private fun browser(kind:String){if(kind=="live"){openLive();return};mainScreen=false
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(18),dp(10),dp(18),dp(10))};val bar=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};bar.addView(nav("‹ BACK"){home()});bar.addView(t(if(kind=="vod")"Movies" else "Series",25,true,Color.WHITE),LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(14)});val search=e("Search","",false);bar.addView(search,LinearLayout.LayoutParams(dp(280),dp(44)));root.addView(bar)
        val cats=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};root.addView(HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;addView(cats)},LinearLayout.LayoutParams(-1,dp(54)).apply{topMargin=dp(8)})
        val rv=RecyclerView(this).apply{layoutManager=GridLayoutManager(this@MainActivity,5);clipToPadding=false};root.addView(rv,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
        var all=emptyList<IptvRepository.Item>();var selected="all";val ad=ContentAdapter(kind,mutableListOf(),{open(it)},{toggle(it)});rv.adapter=ad;fun render(){ad.replace(all.filter{(selected=="all"||it.categoryId==selected)&&it.name.contains(search.text.toString(),true)})};search.addTextChangedListener(object:TextWatcher{override fun afterTextChanged(s:Editable?){render()};override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){};override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}})
        io.execute{try{val c=repo!!.categories(kind);val data=repo!!.items(kind);runOnUiThread{all=data;c.forEach{x->cats.addView(chip(x.name).apply{setOnClickListener{selected=x.id;render()}},LinearLayout.LayoutParams(-2,dp(38)).apply{marginEnd=dp(7)})};render()}}catch(x:Exception){runOnUiThread{Toast.makeText(this,"Unable to load content",Toast.LENGTH_LONG).show()}}}
    }

    private fun open(i:IptvRepository.Item){if(i.kind=="series")episodes(i)else play(repo!!.streamUrl(i),i.name)}
    private fun episodes(s:IptvRepository.Item){mainScreen=false;val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(20),dp(10),dp(20),dp(10))};val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};top.addView(nav("‹ SERIES"){browser("series")});top.addView(t(s.name,24,true,Color.WHITE),LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(14)});root.addView(top);val rv=RecyclerView(this).apply{layoutManager=LinearLayoutManager(this@MainActivity)};root.addView(rv,LinearLayout.LayoutParams(-1,0,1f).apply{topMargin=dp(10)});setContentView(root);io.execute{try{val eps=repo!!.episodes(s.id);runOnUiThread{rv.adapter=EpisodeAdapter(eps){play(repo!!.episodeUrl(it),it.title)}}}catch(_:Exception){runOnUiThread{Toast.makeText(this,"Unable to load episodes",Toast.LENGTH_LONG).show()}}}}

    private fun favorites(){mainScreen=false;val raw=prefs.getStringSet("favorites",emptySet())?:emptySet();val items=raw.mapNotNull{try{val o=JSONObject(it);IptvRepository.Item(o.getString("id"),o.getString("name"),o.optString("icon"),o.optString("ext"),o.optString("cat"),o.getString("kind"))}catch(_:Exception){null}};val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(20),dp(10),dp(20),dp(10))};val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};top.addView(nav("‹ HOME"){home()});top.addView(t("Favorites",25,true,Color.WHITE),LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(14)});root.addView(top);root.addView(RecyclerView(this).apply{layoutManager=GridLayoutManager(this@MainActivity,5);adapter=ContentAdapter("vod",items.toMutableList(),{open(it)},{toggle(it)})},LinearLayout.LayoutParams(-1,0,1f).apply{topMargin=dp(10)});setContentView(root)}

    private fun settings(){mainScreen=false
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(24),dp(14),dp(24),dp(14))};val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};top.addView(nav("‹ HOME"){home()});top.addView(t("Settings",25,true,Color.WHITE),LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(14)});root.addView(top)
        val body=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};root.addView(body,LinearLayout.LayoutParams(-1,0,1f).apply{topMargin=dp(12)})
        val playback=section("PLAYBACK");playback.addView(setting("Default aspect ratio",prefs.getString("aspect","FIT")?:"FIT"){cycleAspect(it)});playback.addView(setting("Auto play preview",if(prefs.getBoolean("preview",true))"ON" else "OFF"){togglePref("preview",it)});playback.addView(setting("Remember playback position",if(prefs.getBoolean("remember_pos",true))"ON" else "OFF"){togglePref("remember_pos",it)});playback.addView(setting("Preferred audio",prefs.getString("audio","AUTO")?:"AUTO"){cycleText(it,"audio",listOf("AUTO","ORIGINAL","ARABIC","ENGLISH"))});playback.addView(setting("Subtitles",prefs.getString("subs","AUTO")?:"AUTO"){cycleText(it,"subs",listOf("AUTO","OFF","ARABIC","ENGLISH"))});body.addView(playback,LinearLayout.LayoutParams(0,-1,1f).apply{marginEnd=dp(10)})
        val app=section("APP & ACCOUNT");app.addView(setting("Live preview volume","35%",null));app.addView(setting("Clear cached images","READY"){Toast.makeText(this,"Image cache refreshes automatically",Toast.LENGTH_SHORT).show()});app.addView(setting("Reload playlist","RELOAD"){repo=IptvRepository(server,user,pass);Toast.makeText(this,"Playlist will refresh on next screen",Toast.LENGTH_SHORT).show()});app.addView(setting("Account",user,null));app.addView(setting("Version","1.2.0",null));val remove=btn("REMOVE ACCOUNT",Color.rgb(103,36,47),Color.WHITE);remove.setOnClickListener{AlertDialog.Builder(this).setTitle("Remove account?").setNegativeButton("CANCEL",null).setPositiveButton("REMOVE"){_,_->prefs.edit().remove("server").remove("user").remove("pass").apply();server="";user="";pass="";repo=null;login()}.show()};app.addView(remove,LinearLayout.LayoutParams(-1,dp(46)).apply{topMargin=dp(10)});body.addView(app,LinearLayout.LayoutParams(0,-1,1f));setContentView(root)
    }
    private fun section(title:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(14),dp(16),dp(14));background=round(panel,18);addView(t(title,12,true,gold).apply{setPadding(0,0,0,dp(8))})}
    private fun setting(name:String,value:String,click:((TextView)->Unit)?):LinearLayout{val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(7),dp(12),dp(7));background=round(soft,12)};row.addView(t(name,13,true,Color.WHITE),LinearLayout.LayoutParams(0,-2,1f));val v=t(value,11,true,gold);row.addView(v);if(click!=null)row.setOnClickListener{click(v)};row.layoutParams=LinearLayout.LayoutParams(-1,dp(48)).apply{bottomMargin=dp(7)};return row}
    private fun togglePref(key:String,v:TextView){val n=!prefs.getBoolean(key,true);prefs.edit().putBoolean(key,n).apply();v.text=if(n)"ON" else "OFF"}
    private fun cycleAspect(v:TextView){cycleText(v,"aspect",listOf("FIT","FILL","ZOOM"))}
    private fun cycleText(v:TextView,key:String,values:List<String>){val cur=prefs.getString(key,values[0])?:values[0];val next=values[(values.indexOf(cur).coerceAtLeast(0)+1)%values.size];prefs.edit().putString(key,next).apply();v.text=next}

    private fun play(url:String,title:String){startActivity(Intent(this,PlayerActivity::class.java).putExtra("url",url).putExtra("title",title))}
    private fun toggle(i:IptvRepository.Item){val s=(prefs.getStringSet("favorites",emptySet())?:emptySet()).toMutableSet();val old=s.firstOrNull{try{val o=JSONObject(it);o.optString("id")==i.id&&o.optString("kind")==i.kind}catch(_:Exception){false}};if(old==null)s.add(JSONObject().put("id",i.id).put("name",i.name).put("icon",i.icon).put("ext",i.extension).put("cat",i.categoryId).put("kind",i.kind).toString())else s.remove(old);prefs.edit().putStringSet("favorites",s).apply();Toast.makeText(this,if(old==null)"Added to favorites" else "Removed from favorites",Toast.LENGTH_SHORT).show()}
    private fun nav(s:String,a:()->Unit)=TextView(this).apply{text=s;textSize=11f;setTextColor(Color.rgb(204,209,220));typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setPadding(dp(13),dp(9),dp(13),dp(9));setOnClickListener{a()}}
    private fun chip(s:String)=TextView(this).apply{text=s;textSize=11f;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;background=round(soft,18);setPadding(dp(15),0,dp(15),0)}
    private fun t(s:String,z:Int,b:Boolean,c:Int)=TextView(this).apply{text=s;textSize=z.toFloat();setTextColor(c);if(b)typeface=Typeface.DEFAULT_BOLD}
    private fun e(h:String,v:String,secret:Boolean)=EditText(this).apply{hint=h;setHintTextColor(Color.rgb(95,102,119));setTextColor(Color.WHITE);setText(v);setSingleLine();textSize=14f;background=round(soft,12);setPadding(dp(15),0,dp(15),0);if(secret)inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD;layoutParams=LinearLayout.LayoutParams(-1,dp(50)).apply{topMargin=dp(9)}}
    private fun btn(s:String,bg:Int,fg:Int)=Button(this).apply{text=s;setTextColor(fg);textSize=12f;typeface=Typeface.DEFAULT_BOLD;isAllCaps=false;background=round(bg,12)}
    private fun round(c:Int,r:Int)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat()};private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    @Deprecated("Deprecated in Java") override fun onBackPressed(){if(!mainScreen){home();return};AlertDialog.Builder(this).setTitle("Exit Adam IPTV?").setMessage("Are you sure you want to close the application?").setNegativeButton("CANCEL",null).setPositiveButton("EXIT"){_,_->finishAffinity()}.show()}
    override fun onDestroy(){super.onDestroy();io.shutdownNow()}
}
