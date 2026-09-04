package com.adam.iptvplayer

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EpisodeAdapter(private val data:List<IptvRepository.Episode>,private val click:(IptvRepository.Episode)->Unit):RecyclerView.Adapter<EpisodeAdapter.H>(){
    class H(val v:TextView):RecyclerView.ViewHolder(v)
    override fun onCreateViewHolder(p:ViewGroup,t:Int):H{val c=p.context;val d=c.resources.displayMetrics.density;return H(TextView(c).apply{setTextColor(Color.WHITE);textSize=14f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER_VERTICAL;setPadding((18*d).toInt(),0,(18*d).toInt(),0);background=GradientDrawable().apply{setColor(Color.rgb(19,22,32));cornerRadius=14*d};layoutParams=ViewGroup.MarginLayoutParams(-1,(58*d).toInt()).apply{bottomMargin=(7*d).toInt()}})}
    override fun onBindViewHolder(h:H,p:Int){val e=data[p];h.v.text="${p+1}.  ${e.title}";h.v.setOnClickListener{click(e)}}
    override fun getItemCount()=data.size
}
