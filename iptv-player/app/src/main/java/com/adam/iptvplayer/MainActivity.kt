package com.adam.iptvplayer

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val io = Executors.newFixedThreadPool(3)
    private lateinit var prefs: android.content.SharedPreferences
    private var repo: IptvRepository? = null
    private var server = ""; private var user = ""; private var pass = ""
    private val bg = Color.rgb(7,16,24); private val panel = Color.rgb(15,29,40); private val accent = Color.rgb(40,215,161)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("iptv_profile_v1", MODE_PRIVATE)
        server = prefs.getString("server", "") ?: ""; user = prefs.getString("user", "") ?: ""; pass = prefs.getString("pass", "") ?: ""
        if (server.isBlank() || user.isBlank() || pass.isBlank()) showLogin() else { repo = IptvRepository(server,user,pass); showHome() }
    }

    private fun showLogin() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(bg); setPadding(dp(42),dp(30),dp(42),dp(30)) }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(24),0,dp(50),0) }
        brand.addView(txt("ADAM IPTV",34,true,Color.WHITE)); brand.addView(txt("Your premium IPTV player",17,false,Color.LTGRAY)); brand.addView(txt("Live TV  •  Movies  •  Series  •  Favorites",14,false,accent))
        root.addView(brand, LinearLayout.LayoutParams(0,-1,1.15f))
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(32),dp(24),dp(32),dp(24)); background = rounded(panel,24) }
        card.addView(txt("Add Xtream account",25,true,Color.WHITE)); card.addView(txt("Enter details from your IPTV provider",13,false,Color.GRAY))
        val eServer = edit("Server URL", server, false); val eUser = edit("Username", user, false); val ePass = edit("Password", pass, true)
        card.addView(eServer); card.addView(eUser); card.addView(ePass)
        val status = txt("",13,false,Color.LTGRAY); card.addView(status)
        val login = button("CONNECT", accent)
        login.setOnClickListener {
            val s=eServer.text.toString().trim(); val u=eUser.text.toString().trim(); val p=ePass.text.toString()
            if (s.isBlank()||u.isBlank()||p.isBlank()) { status.text="Complete all fields"; return@setOnClickListener }
            status.text="Checking account…"; login.isEnabled=false
            io.execute {
                try {
                    val test = IptvRepository(s,u,p).authenticate(); val auth = test.optJSONObject("user_info")?.optInt("auth",0) ?: 0
                    if (auth != 1) error("Account was not accepted")
                    prefs.edit().putString("server",s.trimEnd('/')).putString("user",u).putString("pass",p).apply()
                    server=s.trimEnd('/'); user=u; pass=p; repo=IptvRepository(server,user,pass)
                    runOnUiThread { showHome() }
                } catch(e:Exception) { runOnUiThread { login.isEnabled=true; status.text="Connection failed: ${e.message}" } }
            }
        }
        card.addView(login, LinearLayout.LayoutParams(-1,dp(54)).apply{topMargin=dp(12)})
        root.addView(card, LinearLayout.LayoutParams(0,-1,0.85f))
        setContentView(root)
    }

    private fun showHome() {
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding(dp(34),dp(20),dp(34),dp(24)) }
        val top = LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        val titles = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        titles.addView(txt("ADAM IPTV",26,true,Color.WHITE)); titles.addView(txt("Premium Player • v1.0.0",12,false,Color.GRAY))
        top.addView(titles, LinearLayout.LayoutParams(0,-2,1f))
        val account = txt("● CONNECTED   $user",13,true,accent).apply{gravity=Gravity.END}
        top.addView(account, LinearLayout.LayoutParams(0,-2,1f)); root.addView(top)
        root.addView(txt("What do you want to watch?",21,true,Color.WHITE).apply{setPadding(0,dp(26),0,dp(14))})
        val cards = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER }
        cards.addView(homeCard("LIVE TV","Watch channels now","▶") { showBrowser("live") }, LinearLayout.LayoutParams(0,dp(180),1f).apply{marginEnd=dp(12)})
        cards.addView(homeCard("MOVIES","Your VOD library","▣") { showBrowser("vod") }, LinearLayout.LayoutParams(0,dp(180),1f).apply{marginEnd=dp(12)})
        cards.addView(homeCard("SERIES","Episodes & seasons","▤") { showBrowser("series") }, LinearLayout.LayoutParams(0,dp(180),1f).apply{marginEnd=dp(12)})
        cards.addView(homeCard("FAVORITES","Saved channels & titles","★") { showFavorites() }, LinearLayout.LayoutParams(0,dp(180),1f))
        root.addView(cards)
        val bottom=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL; setPadding(0,dp(24),0,0)}
        bottom.addView(txt("Tip: long-press any channel or title to add/remove Favorite",13,false,Color.GRAY),LinearLayout.LayoutParams(0,-2,1f))
        val settings=button("ACCOUNT",Color.rgb(40,54,66)); settings.setOnClickListener{showSettings()}; bottom.addView(settings,LinearLayout.LayoutParams(dp(150),dp(46))); root.addView(bottom)
        setContentView(root)
    }

    private fun showBrowser(kind:String) {
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(26),dp(16),dp(26),dp(18))}
        val bar=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        val back=button("‹ HOME",Color.rgb(40,54,66));back.setOnClickListener{showHome()};bar.addView(back,LinearLayout.LayoutParams(dp(120),dp(44)))
        val title=txt(when(kind){"live"->"LIVE TV";"vod"->"MOVIES";else->"SERIES"},24,true,Color.WHITE);bar.addView(title,LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(18)})
        val search=edit("Search…","",false);bar.addView(search,LinearLayout.LayoutParams(dp(250),dp(48)));root.addView(bar)
        val content=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}; val cats=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}; val scrollCats=ScrollView(this).apply{addView(cats)}
        val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}; val scroll=ScrollView(this).apply{addView(list)}
        content.addView(scrollCats,LinearLayout.LayoutParams(dp(230),0,1f).apply{topMargin=dp(15);marginEnd=dp(14)}); content.addView(scroll,LinearLayout.LayoutParams(0,0,1f).apply{topMargin=dp(15)})
        root.addView(content,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
        var allItems:List<IptvRepository.Item> = emptyList(); var selectedCat="all"
        fun render(q:String="") { list.removeAllViews(); allItems.filter{ (selectedCat=="all"||it.categoryId==selectedCat) && it.name.contains(q,true)}.forEach{item-> list.addView(itemRow(item),LinearLayout.LayoutParams(-1,dp(66)).apply{bottomMargin=dp(7)}) } }
        search.setOnKeyListener{_,key,event-> if(event.action==KeyEvent.ACTION_UP){render(search.text.toString()); false}else false}
        io.execute {
            try { val categories=repo!!.categories(kind); allItems=repo!!.items(kind)
                runOnUiThread {
                    categories.forEach{cat-> val b=button(cat.name,Color.rgb(25,42,54));b.gravity=Gravity.START or Gravity.CENTER_VERTICAL;b.setOnClickListener{selectedCat=cat.id;render(search.text.toString())};cats.addView(b,LinearLayout.LayoutParams(-1,dp(50)).apply{bottomMargin=dp(6)})}; render()
                }
            } catch(e:Exception){runOnUiThread{list.addView(txt("Could not load content: ${e.message}",16,false,Color.LTGRAY))}}
        }
    }

    private fun itemRow(item:IptvRepository.Item):View {
        val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(18),0,dp(16),0);background=rounded(panel,14);isFocusable=true}
        val star=if(isFav(item))"★" else "☆"; row.addView(txt(star,22,true,accent),LinearLayout.LayoutParams(dp(42),-2))
        row.addView(txt(item.name,16,true,Color.WHITE),LinearLayout.LayoutParams(0,-2,1f))
        row.addView(txt(if(item.kind=="live")"PLAY LIVE" else if(item.kind=="vod")"PLAY" else "EPISODES",12,true,Color.LTGRAY))
        row.setOnClickListener{ if(item.kind=="series") showEpisodes(item) else play(repo!!.streamUrl(item),item.name) }
        row.setOnLongClickListener{toggleFav(item);Toast.makeText(this,if(isFav(item))"Added to favorites" else "Removed from favorites",Toast.LENGTH_SHORT).show();true}
        return row
    }

    private fun showEpisodes(series:IptvRepository.Item){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(26),dp(18),dp(26),dp(18))}; val b=button("‹ SERIES",Color.rgb(40,54,66));b.setOnClickListener{showBrowser("series")};root.addView(b,LinearLayout.LayoutParams(dp(130),dp(44)));root.addView(txt(series.name,24,true,Color.WHITE).apply{setPadding(0,dp(16),0,dp(12))});val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};val scroll=ScrollView(this).apply{addView(list)};root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
        io.execute{try{val eps=repo!!.episodes(series.id);runOnUiThread{eps.forEach{ep->val r=button(ep.title,Color.rgb(18,34,46));r.gravity=Gravity.START or Gravity.CENTER_VERTICAL;r.setOnClickListener{play(repo!!.episodeUrl(ep),ep.title)};list.addView(r,LinearLayout.LayoutParams(-1,dp(58)).apply{bottomMargin=dp(6)})}}}catch(e:Exception){runOnUiThread{list.addView(txt("Could not load episodes: ${e.message}",16,false,Color.LTGRAY))}}}
    }

    private fun showFavorites(){
        val all=prefs.getStringSet("favorites", emptySet()) ?: emptySet(); if(all.isEmpty()){Toast.makeText(this,"No favorites yet",Toast.LENGTH_SHORT).show();showHome();return}
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(28),dp(18),dp(28),dp(18))};val b=button("‹ HOME",Color.rgb(40,54,66));b.setOnClickListener{showHome()};root.addView(b,LinearLayout.LayoutParams(dp(120),dp(44)));root.addView(txt("FAVORITES",24,true,Color.WHITE).apply{setPadding(0,dp(16),0,dp(12))});val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};val scroll=ScrollView(this).apply{addView(list)};root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
        all.sorted().forEach{raw->try{val o=JSONObject(raw);val item=IptvRepository.Item(o.getString("id"),o.getString("name"),null,o.optString("ext"),o.optString("cat"),o.getString("kind"));list.addView(itemRow(item),LinearLayout.LayoutParams(-1,dp(66)).apply{bottomMargin=dp(7)})}catch(_:Exception){}}
    }

    private fun showSettings(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setBackgroundColor(bg);setPadding(dp(80),dp(30),dp(80),dp(30))};root.addView(txt("ACCOUNT & SETTINGS",28,true,Color.WHITE));root.addView(txt("Server: $server\nUsername: $user\n\nApp ID: com.adam.iptvplayer\nVersion: 1.0.0 (1)",16,false,Color.LTGRAY).apply{setPadding(0,dp(24),0,dp(24))});val home=button("BACK TO HOME",Color.rgb(40,54,66));home.setOnClickListener{showHome()};root.addView(home,LinearLayout.LayoutParams(dp(260),dp(52)));val logout=button("REMOVE ACCOUNT",Color.rgb(158,55,63));logout.setOnClickListener{prefs.edit().remove("server").remove("user").remove("pass").apply();server="";user="";pass="";repo=null;showLogin()};root.addView(logout,LinearLayout.LayoutParams(dp(260),dp(52)).apply{topMargin=dp(12)});setContentView(root)
    }

    private fun play(url:String,title:String){startActivity(Intent(this,PlayerActivity::class.java).putExtra("url",url).putExtra("title",title))}
    private fun favJson(i:IptvRepository.Item)=JSONObject().put("id",i.id).put("name",i.name).put("ext",i.extension).put("cat",i.categoryId).put("kind",i.kind).toString()
    private fun isFav(i:IptvRepository.Item)=prefs.getStringSet("favorites",emptySet())?.any{try{val o=JSONObject(it);o.optString("id")==i.id&&o.optString("kind")==i.kind}catch(_:Exception){false}}==true
    private fun toggleFav(i:IptvRepository.Item){val set=(prefs.getStringSet("favorites",emptySet())?:emptySet()).toMutableSet();val old=set.firstOrNull{try{val o=JSONObject(it);o.optString("id")==i.id&&o.optString("kind")==i.kind}catch(_:Exception){false}};if(old!=null)set.remove(old)else set.add(favJson(i));prefs.edit().putStringSet("favorites",set).apply()}

    private fun homeCard(title:String,sub:String,icon:String,action:()->Unit):View{val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(12),dp(12),dp(12),dp(12));background=rounded(panel,20);isFocusable=true;setOnClickListener{action()}};c.addView(txt(icon,38,true,accent));c.addView(txt(title,20,true,Color.WHITE));c.addView(txt(sub,12,false,Color.GRAY));return c}
    private fun txt(s:String,size:Int,bold:Boolean,color:Int)=TextView(this).apply{text=s;textSize=size.toFloat();setTextColor(color);if(bold)typeface=Typeface.DEFAULT_BOLD}
    private fun edit(hint:String,value:String,secret:Boolean)=EditText(this).apply{this.hint=hint;setHintTextColor(Color.GRAY);setTextColor(Color.WHITE);setText(value);singleLine=true;background=rounded(Color.rgb(22,39,51),12);setPadding(dp(16),0,dp(16),0);if(secret)inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD;layoutParams=LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(10)}}
    private fun button(s:String,color:Int)=Button(this).apply{text=s;setTextColor(Color.WHITE);textSize=13f;typeface=Typeface.DEFAULT_BOLD;background=rounded(color,12);isAllCaps=false;isFocusable=true}
    private fun rounded(color:Int,r:Int)=GradientDrawable().apply{setColor(color);cornerRadius=dp(r).toFloat()}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onDestroy(){super.onDestroy();io.shutdownNow()}
}
