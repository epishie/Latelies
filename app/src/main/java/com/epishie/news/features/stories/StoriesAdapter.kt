package com.epishie.news.features.stories

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.epishie.news.R
import com.epishie.news.features.common.inflate
import com.squareup.picasso.Picasso
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.story_item.view.*

class StoriesAdapter : RecyclerView.Adapter<StoriesAdapter.ViewHolder>() {
    val stories = Consumer<List<StoriesViewModel.Story>> { stories ->
        list = stories
        notifyDataSetChanged()
    }
    private var list: List<StoriesViewModel.Story> = emptyList()

    override fun getItemCount(): Int = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = parent.inflate(R.layout.story_item, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val story = list[position]
        holder.bind(story)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(story: StoriesViewModel.Story) {
            itemView.title.text = story.title
            itemView.source.text = story.source
            itemView.description.text = story.description
            if (!
            story.thumbnail.isNullOrEmpty()) {
                Picasso.with(itemView.context)
                        .load(story.thumbnail)
                        .fit()
                        .centerCrop()
                        .into(itemView.thumbnail)
                itemView.thumbnail.visibility = View.VISIBLE
            } else {
                itemView.thumbnail.visibility = View.GONE
            }
            Picasso.with(itemView.context)
                    .load(story.sourceLogo)
                    .fit()
                    .centerCrop()
                    .into(itemView.sourceLogo)
        }
    }
}