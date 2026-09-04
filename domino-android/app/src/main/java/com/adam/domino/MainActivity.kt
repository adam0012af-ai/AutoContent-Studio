package com.adam.domino

import android.app.Activity
import android.graphics.*
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import okhttp3.*
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
    data class Tile(val a: Int, val b: Int)

    private val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
    private var socket: WebSocket? = null
    private var playerId = ""
    private var token = ""
    private var roomId = ""
    private var ownerId = ""
    private var target = 101
    private var coins = 0

    private lateinit var status: TextView
    private lateinit var identity: TextView
    private lateinit var serverInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var roomInput: EditText
    private lateinit var stakeInput: EditText
    private lateinit var targetButton: Button
    private lateinit var startButton: Button
    private lateinit var gameView: OnlineDominoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        buildUi()
    }

    private fun buildUi() {
        val prefs = getSharedPreferences("online", MODE_PRIVATE)
        playerId = prefs.getString("playerId", "") ?: ""
        token = prefs.getString("token", "") ?: ""

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 10, 18, 10)
            setBackgroundColor(Color.rgb(8, 16, 23))
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        identity = TextView(this).apply { setTextColor(Color.WHITE); textSize = 14f; text = if (playerId.isBlank()) "Player ID: --" else "Player ID: $playerId" }
        status = TextView(this).apply { setTextColor(Color.rgb(238, 205, 114)); textSize = 14f; gravity = Gravity.END; text = "غير متصل" }
        top.addView(identity, LinearLayout.LayoutParams(0, -2, 1f)); top.addView(status, LinearLayout.LayoutParams(0, -2, 1f))

        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        serverInput = field("ws://192.168.1.2:8080").apply { setText(prefs.getString("server", "ws://192.168.1.2:8080")) }
        nameInput = field("اسمك").apply { setText(prefs.getString("name", "Player")) }
        roomInput = field("Room ID")
        stakeInput = field("Coins").apply { setText("50"); inputType = 2 }
        val connect = button("اتصال") { connect() }
        val create = button("إنشاء روم") { createRoom() }
        val join = button("دخول روم") { joinRoom() }
        targetButton = button("101") { target = if (target == 101) 151 else 101; targetButton.text = target.toString() }
        startButton = button("ابدأ") { send(JSONObject().put("type", "startMatch").put("roomId", roomId)) }
        controls.addView(serverInput, LinearLayout.LayoutParams(0, 48, 1.7f))
        controls.addView(nameInput, LinearLayout.LayoutParams(0, 48, 0.8f))
        controls.addView(connect)
        controls.addView(roomInput, LinearLayout.LayoutParams(0, 48, 0.8f))
        controls.addView(stakeInput, LinearLayout.LayoutParams(0, 48, 0.55f))
        controls.addView(targetButton)
        controls.addView(create)
        controls.addView(join)
        controls.addView(startButton)

        gameView = OnlineDominoView()
        root.addView(top, LinearLayout.LayoutParams(-1, 42))
        root.addView(controls, LinearLayout.LayoutParams(-1, 54))
        root.addView(gameView, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun field(hintText: String) = EditText(this).apply {
        hint = hintText; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); textSize = 12f
        setSingleLine(true); setBackgroundColor(Color.rgb(24, 34, 44)); setPadding(10, 0, 10, 0)
    }
    private fun button(label: String, action: () -> Unit) = Button(this).apply { text = label; textSize = 11f; setOnClickListener { action() } }

    private fun connect() {
        val url = serverInput.text.toString().trim()
        if (!url.startsWith("ws://") && !url.startsWith("wss://")) { toast("اكتب عنوان ws:// أو wss://"); return }
        getSharedPreferences("online", MODE_PRIVATE).edit().putString("server", url).putString("name", nameInput.text.toString()).apply()
        status.text = "جاري الاتصال..."
        socket?.close(1000, null)
        socket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { runOnUiThread { status.text = "متصل بالسيرفر" } }
            override fun onMessage(webSocket: WebSocket, text: String) { runOnUiThread { handle(JSONObject(text)) } }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {}
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { runOnUiThread { status.text = "فشل الاتصال: ${t.message ?: "خطأ"}" } }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { runOnUiThread { status.text = "تم قطع الاتصال" } }
        })
    }

    private fun handle(j: JSONObject) {
        when (j.optString("type")) {
            "hello" -> {
                val m = JSONObject().put("type", "register").put("name", nameInput.text.toString().ifBlank { "Player" })
                if (playerId.isNotBlank() && token.isNotBlank()) m.put("playerId", playerId).put("token", token)
                send(m)
            }
            "session" -> {
                val p = j.getJSONObject("player"); playerId = p.getString("playerId"); coins = p.optInt("coins")
                token = j.getString("token")
                getSharedPreferences("online", MODE_PRIVATE).edit().putString("playerId", playerId).putString("token", token).apply()
                identity.text = "Player ID: $playerId   •   Coins: $coins"; status.text = "Online"
            }
            "room" -> {
                val r = j.getJSONObject("room"); roomId = r.getString("roomId"); ownerId = r.getString("ownerId"); target = r.optInt("target", 101)
                roomInput.setText(roomId); targetButton.text = target.toString()
                status.text = "Room $roomId • ${r.getJSONArray("players").length()}/2 • انتظار اللاعب"
            }
            "gameState" -> {
                roomId = j.getString("roomId"); target = j.optInt("target", 101)
                gameView.update(j, playerId)
                status.text = if (j.optString("turn") == playerId) "دورك • Room $roomId" else "دور الخصم • Room $roomId"
            }
            "roundFinished" -> {
                val winner = j.optString("winnerId"); val points = j.optInt("points"); status.text = if (winner == playerId) "كسبت الجولة +$points" else "الخصم كسب الجولة +$points"
            }
            "matchFinished" -> {
                val winner = j.optString("winnerId"); status.text = if (winner == playerId) "🏆 كسبت مباراة $target" else "انتهت المباراة - الخصم فاز"
                if (winner == playerId) coins = j.optInt("coins", coins)
                identity.text = "Player ID: $playerId   •   Coins: $coins"
            }
            "error" -> toast(errorText(j.optString("code")))
        }
    }

    private fun errorText(code: String) = when(code) {
        "ROOM_NOT_FOUND" -> "الروم غير موجود"
        "ROOM_FULL" -> "الروم مكتمل"
        "NEED_PLAYERS", "BAD_ROOM_STATE" -> "لازم يكون في لاعبين والروم جاهز"
        "NOT_YOUR_TURN" -> "مش دورك"
        "INVALID_TILE" -> "القطعة مش راكبة"
        "PLAY_AVAILABLE" -> "عندك قطعة تقدر تلعبها"
        "INSUFFICIENT_COINS" -> "الكوينز غير كافية"
        else -> code
    }

    private fun createRoom() {
        if (playerId.isBlank()) { toast("اتصل بالسيرفر الأول"); return }
        send(JSONObject().put("type", "createRoom").put("target", target).put("stake", stakeInput.text.toString().toIntOrNull() ?: 0))
    }
    private fun joinRoom() {
        roomId = roomInput.text.toString().trim().uppercase()
        if (roomId.isBlank()) { toast("اكتب Room ID"); return }
        send(JSONObject().put("type", "joinRoom").put("roomId", roomId))
    }
    private fun send(j: JSONObject) { socket?.send(j.toString()) }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    inner class OnlineDominoView : View(this@MainActivity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val text = Paint(Paint.ANTI_ALIAS_FLAG)
        private val hand = mutableListOf<Tile>()
        private val board = mutableListOf<Tile>()
        private val handRects = mutableListOf<RectF>()
        private val drawRect = RectF()
        private var myTurn = false
        private var opponentCount = 0
        private var boneyardCount = 0
        private var myScore = 0
        private var opponentScore = 0
        private var selected = -1

        fun update(j: JSONObject, me: String) {
            hand.clear(); board.clear()
            val h = j.optJSONArray("hand") ?: JSONArray(); for (i in 0 until h.length()) h.getJSONObject(i).let { hand += Tile(it.getInt("a"), it.getInt("b")) }
            val b = j.optJSONArray("board") ?: JSONArray(); for (i in 0 until b.length()) b.getJSONObject(i).let { board += Tile(it.getInt("a"), it.getInt("b")) }
            myTurn = j.optString("turn") == me; opponentCount = j.optInt("opponentCount"); boneyardCount = j.optInt("boneyardCount")
            val scores = j.optJSONObject("scores") ?: JSONObject(); myScore = scores.optInt(me)
            val keys = scores.keys(); while (keys.hasNext()) { val k = keys.next(); if (k != me) opponentScore = scores.optInt(k) }
            selected = -1; invalidate()
        }

        override fun onDraw(c: Canvas) {
            super.onDraw(c)
            val bg = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), Color.rgb(7, 43, 34), Color.rgb(4, 22, 20), Shader.TileMode.CLAMP)
            paint.shader = bg; c.drawRect(0f,0f,width.toFloat(),height.toFloat(),paint); paint.shader=null
            val table = RectF(20f, 15f, width-20f, height-90f); paint.color=Color.rgb(21,92,66); c.drawRoundRect(table,38f,38f,paint)
            text.color=Color.WHITE; text.textAlign=Paint.Align.CENTER; text.typeface=Typeface.DEFAULT_BOLD; text.textSize=22f
            c.drawText("$opponentScore / $target     الخصم: $opponentCount قطع", width/2f, 38f, text)
            drawBoard(c); drawHand(c)
            drawRect.set(24f,height-70f,150f,height-18f); paint.color=Color.rgb(35,130,92); c.drawRoundRect(drawRect,20f,20f,paint)
            text.textSize=16f; c.drawText("سحب • $boneyardCount", drawRect.centerX(),drawRect.centerY()+6f,text)
            text.textSize=18f; c.drawText("نقاطك: $myScore / $target", width/2f,height-38f,text)
            text.textAlign=Paint.Align.RIGHT; text.textSize=15f; c.drawText(if(myTurn) "دورك" else "دور الخصم", width-30f,height-38f,text)
        }

        private fun drawBoard(c: Canvas) {
            if(board.isEmpty()){ text.textAlign=Paint.Align.CENTER;text.textSize=18f;text.color=Color.argb(160,255,255,255);c.drawText("منتصف الطاولة للعب",width/2f,height/2f-20f,text);return }
            val w=72f; val h=38f; val gap=5f; val per=maxOf(4,((width-160)/(w+gap)).toInt()); var index=0
            val rows=(board.size+per-1)/per; val startY=height/2f-rows*(h+gap)/2f-20f
            for(r in 0 until rows){ val n=minOf(per,board.size-index); var x=width/2f-(n*w+(n-1)*gap)/2f; repeat(n){ drawTile(c,board[index++],RectF(x,startY+r*(h+gap),x+w,startY+r*(h+gap)+h),true);x+=w+gap } }
        }
        private fun drawHand(c: Canvas) {
            handRects.clear(); val w=54f; val h=86f; val gap=8f; val total=hand.size*w+(hand.size-1).coerceAtLeast(0)*gap; var x=width/2f-total/2f; val y=height-92f
            hand.forEachIndexed { i,t -> val lift=if(i==selected) 12f else 0f; val r=RectF(x,y-lift,x+w,y+h-lift);handRects+=r;drawTile(c,t,r,false);x+=w+gap }
        }
        private fun drawTile(c: Canvas,t:Tile,r:RectF,horizontal:Boolean){
            paint.color=Color.rgb(244,239,220);paint.setShadowLayer(7f,1f,3f,Color.BLACK);setLayerType(LAYER_TYPE_SOFTWARE,paint);c.drawRoundRect(r,8f,8f,paint);paint.clearShadowLayer();paint.color=Color.DKGRAY
            if(horizontal)c.drawLine(r.centerX(),r.top+4,r.centerX(),r.bottom-4,paint) else c.drawLine(r.left+4,r.centerY(),r.right-4,r.centerY(),paint)
            text.color=Color.rgb(30,30,30);text.textAlign=Paint.Align.CENTER;text.typeface=Typeface.DEFAULT_BOLD;text.textSize=if(horizontal)18f else 23f
            if(horizontal){c.drawText(t.a.toString(),r.left+r.width()/4,r.centerY()+6,text);c.drawText(t.b.toString(),r.right-r.width()/4,r.centerY()+6,text)} else {c.drawText(t.a.toString(),r.centerX(),r.top+r.height()/4+8,text);c.drawText(t.b.toString(),r.centerX(),r.bottom-r.height()/4+8,text)}
        }
        override fun onTouchEvent(e: MotionEvent): Boolean {
            if(e.action!=MotionEvent.ACTION_UP)return true
            if(drawRect.contains(e.x,e.y)){ if(myTurn) send(JSONObject().put("type","drawTile").put("roomId",roomId)); return true }
            handRects.forEachIndexed { i,r -> if(r.contains(e.x,e.y)){ if(!myTurn){toast("مش دورك");return true}; if(selected==i){ val t=hand[i]; send(JSONObject().put("type","playTile").put("roomId",roomId).put("side","right").put("tile",JSONObject().put("a",t.a).put("b",t.b))) } else {selected=i;invalidate()}; return true } }
            return true
        }
    }

    override fun onDestroy() { socket?.close(1000, "bye"); client.dispatcher.executorService.shutdown(); super.onDestroy() }
}
