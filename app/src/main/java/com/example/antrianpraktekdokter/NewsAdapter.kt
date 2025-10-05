package com.example.antrianpraktekdokter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide



class NewsAdapter(private val newsList: List<NewsItem>) :
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivNews: ImageView = itemView.findViewById(R.id.ivNews)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val news = newsList[position]
        holder.tvTitle.text = news.title

        // Convert possible HTML in description to plain/styled text
        holder.tvDescription.text =
            HtmlCompat.fromHtml(news.description, HtmlCompat.FROM_HTML_MODE_LEGACY)

        val context = holder.itemView.context

        // Load image (fallback ke placeholder kalau url kosong / error)
        if (news.imageUrl.isBlank()) {
            holder.ivNews.setImageResource(R.drawable.ic_placeholder)
        } else {
            Glide.with(context)
                .load(news.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(holder.ivNews)
        }

        // buka link berita di browser saat item diklik
        holder.itemView.setOnClickListener {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(news.link))
                context.startActivity(browserIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getItemCount(): Int = newsList.size
}
