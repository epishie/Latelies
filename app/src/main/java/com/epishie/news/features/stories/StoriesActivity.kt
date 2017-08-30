package com.epishie.news.features.stories

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.epishie.news.R
import com.epishie.news.features.sources.SourcesFragment
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.stories_activity.*

class StoriesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stories_activity)

        setupView()
    }

    private fun setupView() {
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(R.string.title_headlines)

        RxView.clicks(filterButton)
                .subscribe {
                    SourcesFragment().show(supportFragmentManager, null)
                }
    }
}