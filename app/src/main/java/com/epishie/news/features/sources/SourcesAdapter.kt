package com.epishie.news.features.sources

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.epishie.news.R
import com.epishie.news.features.common.inflate
import com.epishie.news.features.sources.SourcesViewModel.Source
import com.jakewharton.rxrelay2.PublishRelay
import com.squareup.picasso.Picasso
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.source_item.view.*

class SourcesAdapter : RecyclerView.Adapter<SourcesAdapter.ViewHolder>() {
    val sources = Consumer<List<SourcesViewModel.Source>> { sources ->
        list = sources
        notifyDataSetChanged()
    }
    private var list: List<SourcesViewModel.Source> = emptyList()
    private var _selections = PublishRelay.create<SourcesViewModel.Source>()
    val selections: Flowable<Source> = _selections.toFlowable(BackpressureStrategy.BUFFER)

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = parent.inflate(R.layout.source_item, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val source = list[position]
        holder.bind(source) {
            _selections.accept(source.copy(selected = !source.selected))
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val picasso = Picasso.with(view.context)

        fun bind(source: Source, onToggleSelection: () -> Unit) {
            itemView.name.text = source.name
            picasso.load(source.logo)
                    .centerCrop()
                    .fit()
                    .into(itemView.logo)
            itemView.check.isChecked = source.selected
            itemView.check.setOnClickListener { onToggleSelection() }
        }
    }
}