package com.youtubeapis.stack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.youtubeapis.R

class TabAdapter(
    private val tabs: List<String>,
   // private val onTabClick: (Int) -> Unit,
   // private val onTabClose: (Int) -> Unit
) : RecyclerView.Adapter<TabAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tab_title)
        val url: TextView = itemView.findViewById(R.id.tab_url)
        val favicon: ImageView = itemView.findViewById(R.id.tab_favicon)
        val close: ImageView = itemView.findViewById(R.id.tab_close)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tab, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tab = tabs[position]
        
        holder.title.text = "${position + 1}"
       // holder.url.text = tab.url
        // Load favicon here
        
       /* holder.itemView.setOnClickListener {
            onTabClick(position)
        }
        
        holder.close.setOnClickListener {
            onTabClose(tab)
        }*/
    }

    override fun getItemCount(): Int = tabs.size

    /*fun closeTab(position: Int) {
        if (position < tabs.size) {
            onTabClose(tabs[position])
        }
    }*/
}