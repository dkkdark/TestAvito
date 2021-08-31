package com.kseniabl.weatherpredict

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kseniabl.weatherpredict.models.RecyclerViewItem

// simple recycler view
class RecyclerViewAdapter(private var items: List<RecyclerViewItem>, private val context: Context): RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder>() {

    inner class RecyclerViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var time: TextView = itemView.findViewById(R.id.item_time)
        var icon: ImageView = itemView.findViewById(R.id.item_image)
        var temperature: TextView = itemView.findViewById(R.id.item_text)
        var description: TextView = itemView.findViewById(R.id.item_descr)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        return RecyclerViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.weather_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val item = items[position]

        holder.time.text = item.time

        Glide.with(context)
            .load(item.weatherIcon)
            .into(holder.icon)
        holder.temperature.text = item.temp
        holder.description.text = item.weatherDescription
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
