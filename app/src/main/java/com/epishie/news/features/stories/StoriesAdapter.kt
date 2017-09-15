package com.epishie.news.features.stories

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.epishie.news.R
import com.epishie.news.features.common.inflate
import com.epishie.news.features.common.into
import com.jakewharton.rxbinding2.view.clicks
import com.squareup.picasso.Picasso
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.story_item.view.*

class StoriesAdapter(context: Context, private val storyClicks: Consumer<StoriesViewModel.Story>)
    : RecyclerView.Adapter<StoriesAdapter.ViewHolder>() {
    val stories = Consumer<List<StoriesViewModel.Story>> { stories ->
        val callback = object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return list[oldItemPosition].url == stories[newItemPosition].url
            }

            override fun getOldListSize(): Int = list.size

            override fun getNewListSize(): Int = stories.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return list[oldItemPosition] == stories[newItemPosition]
            }
        }
        DiffUtil.calculateDiff(callback, true).dispatchUpdatesTo(this)
        list = stories
    }
    private var list: List<StoriesViewModel.Story> = emptyList()
    private val defaultImages: List<Int>

    init {
        val array = context.resources.obtainTypedArray(R.array.default_images)
        val count = array.length()
        defaultImages = (0 until count).map { index ->
            array.getResourceId(index, 0)
        }
        array.recycle()
    }

    override fun getItemCount(): Int = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = parent.inflate(R.layout.story_item, false)
        return ViewHolder(view, defaultImages)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val story = list[position]
        holder.bind(story)
        holder.itemView.clicks()
                .map { story }
                .subscribe(storyClicks)
    }

    class ViewHolder(view: View, private val defaultImages: List<Int>)
        : RecyclerView.ViewHolder(view) {
        fun bind(story: StoriesViewModel.Story) {
            itemView.title.text = story.title
            itemView.source.text = story.source
            if (!story.thumbnail.isNullOrEmpty()) {
                Picasso.with(itemView.context)
                        .load(story.thumbnail)
                        .fit()
                        .centerCrop()
                        .placeholder(R.drawable.default_thumbnail)
                        .error(R.drawable.default_thumbnail)
                        .into(itemView.thumbnail) {
                            onFailed {
                                loadFallbackImage(story.url)
                            }
                        }
            } else {
                loadFallbackImage(story.url)
            }
        }

        private fun loadFallbackImage(url: String) {
            itemView.thumbnail.setImageDrawable(ContextCompat.getDrawable(itemView.context,
                    defaultImages[Math.abs(url.hashCode()).rem(defaultImages.size)]))
        }
    }
}