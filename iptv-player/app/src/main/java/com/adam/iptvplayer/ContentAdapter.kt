package com.adam.iptvplayer

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class ContentAdapter(
    private val mode:String,
    private val items:MutableList<IptvRepository.Item>,
    private val onClick:(IptvRepository.Item)->Unit,
    private val onLong:(IptvRepository.Item)->Unit
): RecyclerView.Adapter<ContentAdapter.H>() {
    class H(val root:LinearLayout,val image:ImageView,val title:TextView,val meta:TextView):RecyclerView.ViewHolder(root)
    override fun onCreateViewHolder(parent:ViewGroup,viewType:Int):H{
        val c=parent.context; fun dp(v:Int)=(v*c.resources.displayMetrics.density).toInt()
        val root=LinearLayout(c).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(8),dp(8),dp(8),dp(10));background=GradientDrawable().apply{setColor(Color.rgb(17,20,30));cornerRadius=dp(18).toFloat()};isFocusable=true}
        val img=ImageView(c).apply{scaleType=ImageView.ScaleType.CENTER_CROP;background=GradientDrawable().apply{setColor(Color.rgb(28,32,44));cornerRadius=dp(14).toFloat()}}
        val title=TextView(c).apply{setTextColor(Color.WHITE);textSize=14f;typeface=Typeface.DEFAULT_BOLD;maxLines=2;setPadding(dp(4),dp(10),dp(4),0)}
        val meta=TextView(c).apply{setTextColor(Color.rgb(135,143,160));textSize=11f;setPadding(dp(4),dp(4),dp(4),0)}
        root.addView(img,LinearLayout.LayoutParams(-1,if(mode=="live")dp(100) else dp(185)))
        root.addView(title);root.addView(meta)
        return H(root,img,title,meta)
    }
    override fun onBindViewHolder(h:H,p:Int){val i=items[p];h.title.text=i.name;h.meta.text=when(i.kind){"live"->"LIVE CHANNEL";"vod"->"MOVIE";else->"SERIES"};if(i.icon.isNullOrBlank()){h.image.setImageDrawable(null)}else h.image.load(i.icon){crossfade(true)};h.root.setOnClickListener{onClick(i)};h.root.setOnLongClickListener{onLong(i);true}}
    override fun getItemCount()=items.size
    fun replace(list:List<IptvRepository.Item>){items.clear();items.addAll(list);notifyDataSetChanged()}
}
