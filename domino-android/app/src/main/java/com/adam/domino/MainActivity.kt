package com.adam.domino

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import java.util.Collections

class MainActivity : Activity() {
    data class Tile(val a: Int, val b: Int) {
        fun matches(v: Int) = a == v || b == v
        fun other(v: Int) = if (a == v) b else a
        override fun toString() = "[$a|$b]"
    }

    private val player = mutableListOf<Tile>()
    private val cpu = mutableListOf<Tile>()
    private val boneyard = mutableListOf<Tile>()
    private val board = mutableListOf<Tile>()
    private var left = -1
    private var right = -1
    private var playerTurn = true
    private lateinit var status: TextView
    private lateinit var boardText: TextView
    private lateinit var handRow: LinearLayout
    private lateinit var drawButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        newGame()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 14, 20, 14)
            setBackgroundColor(Color.rgb(16, 42, 67))
        }
        val title = TextView(this).apply {
            text = "DOMINO"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        boardText = TextView(this).apply {
            textSize = 23f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8, 18, 8, 18)
        }
        val scroll = HorizontalScrollView(this)
        handRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        scroll.addView(handRow)
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        drawButton = Button(this).apply {
            text = "سحب"
            setOnClickListener { drawForPlayer() }
        }
        val newButton = Button(this).apply {
            text = "لعبة جديدة"
            setOnClickListener { newGame() }
        }
        actions.addView(drawButton)
        actions.addView(newButton)
        root.addView(title, LinearLayout.LayoutParams(-1, 0, 0.7f))
        root.addView(status, LinearLayout.LayoutParams(-1, 0, 0.8f))
        root.addView(boardText, LinearLayout.LayoutParams(-1, 0, 1.8f))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1.7f))
        root.addView(actions, LinearLayout.LayoutParams(-1, 0, 0.8f))
        setContentView(root)
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
        left = -1
        right = -1
        playerTurn = true
        refresh("دورك - اختر قطعة")
    }

    private fun playable(tile: Tile) = board.isEmpty() || tile.matches(left) || tile.matches(right)

    private fun playPlayer(index: Int) {
        if (!playerTurn || index !in player.indices) return
        val tile = player[index]
        if (!playable(tile)) {
            Toast.makeText(this, "القطعة لا تناسب طرفي السلسلة", Toast.LENGTH_SHORT).show()
            return
        }
        place(tile)
        player.removeAt(index)
        if (player.isEmpty()) {
            finishGame("فزت! 🎉")
            return
        }
        playerTurn = false
        refresh("دور الكمبيوتر...")
        handRow.postDelayed({ cpuTurn() }, 400)
    }

    private fun place(tile: Tile) {
        if (board.isEmpty()) {
            board += tile
            left = tile.a
            right = tile.b
        } else if (tile.matches(right)) {
            board += tile
            right = tile.other(right)
        } else if (tile.matches(left)) {
            board.add(0, tile)
            left = tile.other(left)
        }
    }

    private fun drawForPlayer() {
        if (!playerTurn) return
        if (player.any(::playable)) {
            Toast.makeText(this, "عندك قطعة قابلة للعب", Toast.LENGTH_SHORT).show()
            return
        }
        if (boneyard.isNotEmpty()) {
            player += boneyard.removeAt(0)
            refresh("سحبت قطعة")
        } else {
            playerTurn = false
            refresh("لا توجد قطع للسحب - دور الكمبيوتر")
            handRow.postDelayed({ cpuTurn() }, 400)
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
            if (cpu.isEmpty()) {
                finishGame("الكمبيوتر فاز")
                return
            }
        }
        if (boneyard.isEmpty() && player.none(::playable) && cpu.none(::playable)) {
            val playerScore = player.sumOf { it.a + it.b }
            val cpuScore = cpu.sumOf { it.a + it.b }
            finishGame(
                when {
                    playerScore < cpuScore -> "فزت بالنقاط!"
                    cpuScore < playerScore -> "الكمبيوتر فاز بالنقاط"
                    else -> "تعادل"
                }
            )
            return
        }
        playerTurn = true
        refresh("دورك - اختر قطعة")
    }

    private fun finishGame(message: String) {
        playerTurn = false
        refresh(message)
        drawButton.isEnabled = false
    }

    private fun refresh(message: String) {
        status.text = "$message   |   أنت: ${player.size}   الكمبيوتر: ${cpu.size}   السحب: ${boneyard.size}"
        boardText.text = if (board.isEmpty()) "ابدأ بأي قطعة" else board.joinToString("  ")
        handRow.removeAllViews()
        player.forEachIndexed { index, tile ->
            handRow.addView(Button(this).apply {
                text = tile.toString()
                textSize = 17f
                isAllCaps = false
                setOnClickListener { playPlayer(index) }
            })
        }
        drawButton.isEnabled = playerTurn
    }
}
