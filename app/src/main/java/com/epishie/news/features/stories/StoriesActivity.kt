package com.epishie.news.features.stories

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import com.epishie.news.R
import com.epishie.news.component
import com.epishie.news.features.sources.SourcesFragment
import com.jakewharton.rxbinding2.support.v4.widget.refreshes
import com.jakewharton.rxbinding2.support.v4.widget.refreshing
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.BackpressureStrategy
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.stories_activity.*
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

class StoriesActivity : AppCompatActivity() {
    lateinit var vm: StoriesViewModel
    @field:[Inject Named("ui")]
    lateinit var ui: Scheduler
    lateinit var disposable: Disposable
    lateinit var storiesAdapter: StoriesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        vm = ViewModelProviders.of(this, component.vmFactory())[StoriesViewModel::class.java]

        setContentView(R.layout.stories_activity)
        setupView()
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

    private fun setupView() {
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(R.string.title_headlines)

        storiesAdapter = StoriesAdapter()
        storyList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL,
                false)
        storyList.adapter = storiesAdapter

        RxView.clicks(filterButton)
                .subscribe {
                    SourcesFragment().show(supportFragmentManager, null)
                }
        val events = refresher.refreshes()
                .toFlowable(BackpressureStrategy.BUFFER)
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

        disposable = states.connect()
    }
}