package com.epishie.news.features.stories

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.epishie.news.R
import com.epishie.news.component
import com.epishie.news.features.common.BaseActivity
import com.epishie.news.features.sources.SourcesFragment
import com.epishie.news.features.story.StoryActivity
import com.jakewharton.rxbinding2.support.v4.widget.refreshes
import com.jakewharton.rxbinding2.support.v4.widget.refreshing
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.stories_activity.*
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

class StoriesActivity : BaseActivity() {
    @field:[Inject Named("ui")]
    lateinit var ui: Scheduler
    private  lateinit var vm: StoriesViewModel
    private lateinit var storiesAdapter: StoriesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        vm = ViewModelProviders.of(this, component.vmFactory())[StoriesViewModel::class.java]

        setContentView(R.layout.stories_activity)
        setupView()
    }

    private fun setupView() {
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(R.string.lbl_headlines)

        storiesAdapter = StoriesAdapter(this, Consumer { (url) ->
            val intent = StoryActivity.storyIntent(this, url)
            startActivity(intent)
        })
        storyList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL,
                false)
        storyList.adapter = storiesAdapter
        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.story_space))
        storyList.addItemDecoration(divider)

        filterButton.clicks()
                .subscribeOn(ui)
                .subscribe {
                    SourcesFragment().show(supportFragmentManager, null)
                }
        val events = refresher.refreshes()
                .toFlowable(BackpressureStrategy.BUFFER)
                .subscribeOn(ui)
                .map {
                    StoriesViewModel.Event.Refresh as StoriesViewModel.Event
                }
                .startWith(StoriesViewModel.Event.Refresh)
        val states = vm.update(events)
                .observeOn(ui)
                .publish()
        states.map { (progress) -> progress }
                .subscribe(refresher.refreshing())
        states.map { state -> state.stories }
                .subscribe(storiesAdapter.stories)
        states.map { (progress, error, stories) -> stories.isEmpty() && !progress && error == null }
                .subscribe(emptyState.visibility(View.GONE))
        states.subscribe { state ->
            if (state.error != null) {
                val error = when (state.error) {
                    is IOException -> getString(R.string.error_no_internet)
                    else -> getString(R.string.error_unknown)
                }
                Snackbar.make(coordinatorLayout, error, Snackbar.LENGTH_LONG)
                        .show()
            }
        }

        disposables.add(states.connect())
    }
}