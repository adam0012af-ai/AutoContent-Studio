package com.adam.domino

import android.app.Activity
import android.graphics.*
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import java.util.Collections
import kotlin.math.min

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(DominoTableView())
    }

    inner class DominoTableView : View(this) {
        data class Tile(val a: Int, val b: Int) {
            fun matches(v: Int) = a == v || b == v
            fun other(v: Int) = if (a == v) b else a
        }

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val player = mutableListOf<Tile>()
        private val cpu = mutableListOf<Tile>()
        private val boneyard = mutableListOf<Tile>()
        private val board = mutableListOf<Tile>()
        private val handRects = mutableListOf<RectF>()
        private val handler = Handler(Looper.getMainLooper())
        private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 55)

        private var left = -1
        private var right = -1
        private var playerTurn = true
        private var message = "دورك"
        private var selected = -1
        private var playerCoins = 1250
        private var cpuCoins = 980
        private var playerScore = 0
        private var cpuScore = 0
        private var matchTarget = 101

        private val drawRect = RectF()
        private val newRect = RectF()
        private val modeRect = RectF()

        init { newGame() }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            tone.release()
        }

        private fun newGame() {
            val deck = mutableListOf<Tile>()
            for (a in 0..6) for (b in a..6) deck += Tile(a, b)
            Collections.shuffle(deck)
            player.clear(); cpu.clear(); boneyard.clear(); board.clear()
            repeat(7) {
                player += deck.removeAt(0)
                cpu += deck.removeAt(0)
            }
            boneyard += deck
            left = -1; right = -1
            playerTurn = true
            selected = -1
            message = "دورك - اختار قطعة"
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            drawBackground(canvas)
            drawTopHud(canvas)
            drawTable(canvas)
            drawCpuHand(canvas)
            drawBoard(canvas)
            drawPlayerHand(canvas)
            drawBottomControls(canvas)
        }

        private fun drawBackground(canvas: Canvas) {
            val shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), Color.rgb(10, 20, 28), Color.rgb(5, 12, 18), Shader.TileMode.CLAMP)
            paint.shader = shader
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.shader = null
        }

        private fun drawTopHud(canvas: Canvas) {
            val pad = dp(18f)
            val top = dp(12f)
            drawAvatarChip(canvas, pad, top, "YOU", playerCoins, playerScore, true)
            drawAvatarChip(canvas, width - dp(220f), top, "CPU", cpuCoins, cpuScore, false)

            textPaint.textAlign = Paint.Align.CENTER
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = sp(22f)
            textPaint.color = Color.rgb(245, 210, 118)
            canvas.drawText("DOMINO CLUB", width / 2f, top + dp(24f), textPaint)
            textPaint.textSize = sp(13f)
            textPaint.color = Color.argb(220, 240, 240, 240)
            canvas.drawText("$message  •  هدف المباراة $matchTarget", width / 2f, top + dp(48f), textPaint)
        }

        private fun drawAvatarChip(canvas: Canvas, x: Float, y: Float, name: String, coins: Int, score: Int, playerSide: Boolean) {
            val r = RectF(x, y, x + dp(200f), y + dp(56f))
            paint.color = Color.argb(190, 20, 29, 38)
            paint.setShadowLayer(dp(10f), 0f, dp(3f), Color.argb(120, 0, 0, 0))
            setLayerType(LAYER_TYPE_SOFTWARE, paint)
            canvas.drawRoundRect(r, dp(14f), dp(14f), paint)
            paint.clearShadowLayer()

            paint.color = if (playerSide) Color.rgb(40, 190, 132) else Color.rgb(210, 82, 82)
            canvas.drawCircle(x + dp(28f), y + dp(28f), dp(18f), paint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = sp(12f)
            textPaint.color = Color.WHITE
            canvas.drawText(name.take(1), x + dp(28f), y + dp(32f), textPaint)

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.textSize = sp(13f)
            canvas.drawText(name, x + dp(56f), y + dp(21f), textPaint)
            textPaint.textSize = sp(11f)
            textPaint.color = Color.rgb(245, 210, 118)
            canvas.drawText("◉ $coins", x + dp(56f), y + dp(40f), textPaint)
            textPaint.color = Color.LTGRAY
            canvas.drawText("PTS $score", x + dp(125f), y + dp(40f), textPaint)
        }

        private fun drawTable(canvas: Canvas) {
            val table = RectF(dp(20f), dp(78f), width - dp(20f), height - dp(118f))
            paint.color = Color.rgb(64, 38, 24)
            paint.setShadowLayer(dp(18f), 0f, dp(6f), Color.BLACK)
            canvas.drawRoundRect(table, dp(34f), dp(34f), paint)
            paint.clearShadowLayer()

            val inner = RectF(table.left + dp(14f), table.top + dp(14f), table.right - dp(14f), table.bottom - dp(14f))
            val felt = RadialGradient(inner.centerX(), inner.centerY(), inner.width() * .7f, Color.rgb(27, 112, 77), Color.rgb(10, 64, 48), Shader.TileMode.CLAMP)
            paint.shader = felt
            canvas.drawRoundRect(inner, dp(26f), dp(26f), paint)
            paint.shader = null

            paint.color = Color.argb(28, 255, 255, 255)
            for (i in 0..22) {
                val x = inner.left + (i * 71 % inner.width().toInt())
                canvas.drawCircle(x, inner.top + ((i * 47) % inner.height().toInt()), dp(1.3f), paint)
            }
        }

        private fun drawCpuHand(canvas: Canvas) {
            val count = cpu.size
            val w = dp(34f)
            val gap = dp(7f)
            val total = count * w + (count - 1).coerceAtLeast(0) * gap
            var x = width / 2f - total / 2f
            val y = dp(94f)
            repeat(count) {
                drawDominoBack(canvas, RectF(x, y, x + w, y + dp(58f)))
                x += w + gap
            }
        }

        private fun drawBoard(canvas: Canvas) {
            if (board.isEmpty()) {
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.textSize = sp(17f)
                textPaint.color = Color.argb(170, 255, 255, 255)
                canvas.drawText("ابدأ بأي قطعة", width / 2f, height / 2f, textPaint)
                return
            }

            val tileW = dp(68f)
            val tileH = dp(34f)
            val gap = dp(5f)
            val maxPerRow = maxOf(4, ((width - dp(120f)) / (tileW + gap)).toInt())
            val rows = (board.size + maxPerRow - 1) / maxPerRow
            var index = 0
            val startY = height / 2f - (rows * (tileH + gap)) / 2f
            repeat(rows) { row ->
                val n = min(maxPerRow, board.size - index)
                val rowWidth = n * tileW + (n - 1) * gap
                var x = width / 2f - rowWidth / 2f
                repeat(n) {
                    drawDominoFace(canvas, board[index], RectF(x, startY + row * (tileH + gap), x + tileW, startY + row * (tileH + gap) + tileH), horizontal = true, glow = false)
                    index++
                    x += tileW + gap
                }
            }
        }

        private fun drawPlayerHand(canvas: Canvas) {
            handRects.clear()
            val tileW = dp(54f)
            val tileH = dp(90f)
            val gap = dp(9f)
            val total = player.size * tileW + (player.size - 1).coerceAtLeast(0) * gap
            var x = width / 2f - total / 2f
            val baseY = height - dp(112f)
            player.forEachIndexed { index, tile ->
                val lift = if (selected == index) dp(13f) else 0f
                val rect = RectF(x, baseY - lift, x + tileW, baseY + tileH - lift)
                handRects += rect
                drawDominoFace(canvas, tile, rect, horizontal = false, glow = playable(tile) && playerTurn)
                x += tileW + gap
            }
        }

        private fun drawBottomControls(canvas: Canvas) {
            val y = height - dp(50f)
            drawRect.set(dp(20f), y, dp(130f), y + dp(38f))
            newRect.set(width - dp(142f), y, width - dp(20f), y + dp(38f))
            modeRect.set(width / 2f - dp(74f), y, width / 2f + dp(74f), y + dp(38f))
            drawPill(canvas, drawRect, "سحب • ${boneyard.size}", Color.rgb(31, 142, 96))
            drawPill(canvas, modeRect, "101 POINTS", Color.rgb(38, 52, 68))
            drawPill(canvas, newRect, "لعبة جديدة", Color.rgb(110, 68, 42))
        }

        private fun drawPill(canvas: Canvas, rect: RectF, label: String, color: Int) {
            paint.color = color
            paint.setShadowLayer(dp(7f), 0f, dp(2f), Color.argb(100, 0, 0, 0))
            canvas.drawRoundRect(rect, dp(19f), dp(19f), paint)
            paint.clearShadowLayer()
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = sp(12f)
            textPaint.color = Color.WHITE
            canvas.drawText(label, rect.centerX(), rect.centerY() + dp(4f), textPaint)
        }

        private fun drawDominoBack(canvas: Canvas, r: RectF) {
            paint.color = Color.rgb(22, 27, 32)
            paint.setShadowLayer(dp(6f), dp(1f), dp(3f), Color.argb(130, 0, 0, 0))
            canvas.drawRoundRect(r, dp(7f), dp(7f), paint)
            paint.clearShadowLayer()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(1.2f)
            paint.color = Color.rgb(194, 151, 69)
            canvas.drawRoundRect(RectF(r.left + dp(3f), r.top + dp(3f), r.right - dp(3f), r.bottom - dp(3f)), dp(5f), dp(5f), paint)
            paint.style = Paint.Style.FILL
        }

        private fun drawDominoFace(canvas: Canvas, tile: Tile, r: RectF, horizontal: Boolean, glow: Boolean) {
            if (glow) {
                paint.color = Color.argb(85, 77, 230, 168)
                paint.setShadowLayer(dp(13f), 0f, 0f, Color.rgb(72, 220, 155))
                canvas.drawRoundRect(r, dp(8f), dp(8f), paint)
                paint.clearShadowLayer()
            }

            paint.color = Color.rgb(246, 241, 224)
            paint.setShadowLayer(dp(7f), dp(1f), dp(3f), Color.argb(150, 0, 0, 0))
            canvas.drawRoundRect(r, dp(8f), dp(8f), paint)
            paint.clearShadowLayer()
            paint.color = Color.rgb(214, 205, 181)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(1.2f)
            canvas.drawRoundRect(r, dp(8f), dp(8f), paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.rgb(70, 64, 55)
            paint.strokeWidth = dp(1.2f)
            if (horizontal) canvas.drawLine(r.centerX(), r.top + dp(4f), r.centerX(), r.bottom - dp(4f), paint)
            else canvas.drawLine(r.left + dp(4f), r.centerY(), r.right - dp(4f), r.centerY(), paint)

            if (horizontal) {
                drawPips(canvas, tile.a, RectF(r.left, r.top, r.centerX(), r.bottom))
                drawPips(canvas, tile.b, RectF(r.centerX(), r.top, r.right, r.bottom))
            } else {
                drawPips(canvas, tile.a, RectF(r.left, r.top, r.right, r.centerY()))
                drawPips(canvas, tile.b, RectF(r.left, r.centerY(), r.right, r.bottom))
            }
        }

        private fun drawPips(canvas: Canvas, value: Int, r: RectF) {
            if (value == 0) return
            val xs = floatArrayOf(r.left + r.width() * .27f, r.centerX(), r.right - r.width() * .27f)
            val ys = floatArrayOf(r.top + r.height() * .25f, r.centerY(), r.bottom - r.height() * .25f)
            val pts = when (value) {
                1 -> listOf(1 to 1)
                2 -> listOf(0 to 0, 2 to 2)
                3 -> listOf(0 to 0, 1 to 1, 2 to 2)
                4 -> listOf(0 to 0, 2 to 0, 0 to 2, 2 to 2)
                5 -> listOf(0 to 0, 2 to 0, 1 to 1, 0 to 2, 2 to 2)
                else -> listOf(0 to 0, 0 to 1, 0 to 2, 2 to 0, 2 to 1, 2 to 2)
            }
            paint.color = Color.rgb(37, 39, 42)
            pts.forEach { (ix, iy) -> canvas.drawCircle(xs[ix], ys[iy], dp(2.6f), paint) }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action != MotionEvent.ACTION_UP) return true
            val x = event.x; val y = event.y
            if (drawRect.contains(x, y)) { drawForPlayer(); return true }
            if (newRect.contains(x, y)) { tone.startTone(ToneGenerator.TONE_PROP_BEEP, 70); newGame(); return true }
            if (modeRect.contains(x, y)) {
                matchTarget = if (matchTarget == 101) 151 else 101
                message = "نظام المباراة $matchTarget نقطة"
                invalidate(); return true
            }
            handRects.forEachIndexed { index, rect ->
                if (rect.contains(x, y)) {
                    if (selected == index) playPlayer(index) else { selected = index; tone.startTone(ToneGenerator.TONE_PROP_ACK, 45); invalidate() }
                    return true
                }
            }
            return true
        }

        private fun playable(tile: Tile) = board.isEmpty() || tile.matches(left) || tile.matches(right)

        private fun playPlayer(index: Int) {
            if (!playerTurn || index !in player.indices) return
            val tile = player[index]
            if (!playable(tile)) {
                message = "القطعة دي مش راكبة"
                tone.startTone(ToneGenerator.TONE_PROP_NACK, 90)
                Toast.makeText(this@MainActivity, "اختار قطعة مناسبة", Toast.LENGTH_SHORT).show()
                invalidate(); return
            }
            place(tile)
            player.removeAt(index)
            selected = -1
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 70)
            if (player.isEmpty()) { finishRound(true); return }
            playerTurn = false
            message = "دور الخصم..."
            invalidate()
            handler.postDelayed({ cpuTurn() }, 650)
        }

        private fun place(tile: Tile) {
            if (board.isEmpty()) {
                board += tile; left = tile.a; right = tile.b
            } else if (tile.matches(right)) {
                board += tile; right = tile.other(right)
            } else if (tile.matches(left)) {
                board.add(0, tile); left = tile.other(left)
            }
        }

        private fun drawForPlayer() {
            if (!playerTurn) return
            if (player.any(::playable)) {
                message = "عندك قطعة قابلة للعب"
                tone.startTone(ToneGenerator.TONE_PROP_NACK, 70)
                invalidate(); return
            }
            if (boneyard.isNotEmpty()) {
                player += boneyard.removeAt(0)
                message = "سحبت قطعة"
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 55)
                invalidate()
            } else {
                playerTurn = false
                message = "مفيش سحب - دور الخصم"
                invalidate()
                handler.postDelayed({ cpuTurn() }, 500)
            }
        }

        private fun cpuTurn() {
            var index = cpu.indexOfFirst(::playable)
            while (index == -1 && boneyard.isNotEmpty()) {
                cpu += boneyard.removeAt(0)
                index = cpu.indexOfFirst(::playable)
            }
            if (index != -1) {
                val tile = cpu.removeAt(index)
                place(tile)
                if (cpu.isEmpty()) { finishRound(false); return }
            }
            if (boneyard.isEmpty() && player.none(::playable) && cpu.none(::playable)) {
                val p = player.sumOf { it.a + it.b }
                val c = cpu.sumOf { it.a + it.b }
                finishRound(p <= c)
                return
            }
            playerTurn = true
            message = "دورك - اختار قطعة"
            invalidate()
        }

        private fun finishRound(playerWon: Boolean) {
            playerTurn = false
            val loserPips = if (playerWon) cpu.sumOf { it.a + it.b } else player.sumOf { it.a + it.b }
            if (playerWon) {
                playerScore += loserPips
                playerCoins += 50
                cpuCoins = (cpuCoins - 50).coerceAtLeast(0)
                message = "كسبت الجولة +$loserPips نقطة  •  +50 كوين"
                tone.startTone(ToneGenerator.TONE_PROP_PROMPT, 180)
            } else {
                cpuScore += loserPips
                cpuCoins += 50
                playerCoins = (playerCoins - 50).coerceAtLeast(0)
                message = "الخصم كسب الجولة +$loserPips نقطة"
                tone.startTone(ToneGenerator.TONE_PROP_NACK, 180)
            }
            val matchOver = playerScore >= matchTarget || cpuScore >= matchTarget
            if (matchOver) {
                message = if (playerScore >= matchTarget) "🏆 كسبت المباراة!" else "انتهت المباراة - الخصم فاز"
            } else {
                handler.postDelayed({ newGame() }, 1700)
            }
            invalidate()
        }

        private fun dp(v: Float) = v * resources.displayMetrics.density
        private fun sp(v: Float) = v * resources.displayMetrics.scaledDensity
    }
}
