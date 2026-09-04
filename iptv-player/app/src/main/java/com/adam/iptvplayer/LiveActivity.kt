package com.adam.iptvplayer

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LiveActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(5,7,12))
            setPadding(20,20,20,20)
        }
        val title = TextView(this).apply {
            text = "Live TV"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
        }
        root.addView(title, LinearLayout.LayoutParams(-1, 64))
        val body = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val categories = RecyclerView(this).apply { layoutManager = LinearLayoutManager(this@LiveActivity) }
        val channels = RecyclerView(this).apply { layoutManager = LinearLayoutManager(this@LiveActivity) }
        val preview = LinearLayout(this).apply { setBackgroundColor(Color.rgb(15,18,27)) }
        body.addView(categories, LinearLayout.LayoutParams(220, -1))
        body.addView(channels, LinearLayout.LayoutParams(0, -1, 1f))
        body.addView(preview, LinearLayout.LayoutParams(0, -1, 1.2f))
        root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }
}
