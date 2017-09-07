package com.epishie.news.features.splash

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.epishie.news.R
import com.epishie.news.component
import com.epishie.news.features.stories.StoriesActivity
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.BackpressureStrategy
import io.reactivex.Scheduler
import kotlinx.android.synthetic.main.splash_activity.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class SplashActivity : AppCompatActivity() {
    @field:[Inject Named("ui")]
    lateinit var ui: Scheduler
    private lateinit var vm: SplashViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        vm = ViewModelProviders.of(this, component.vmFactory())[SplashViewModel::class.java]

        setContentView(R.layout.splash_activity)
        setupView()
    }

    private fun setupView() {
        val events = retry.clicks()
                .map { SplashViewModel.Event.RetryEvent as SplashViewModel.Event }
                .toFlowable(BackpressureStrategy.BUFFER)
        vm.update(events)
                .observeOn(ui)
                .subscribe(this::renderState)
    }

    private fun renderState(state: SplashViewModel.State) {
        if (state.success) {
            startActivity(Intent(this, StoriesActivity::class.java))
            finish()
            return
        }

        progress.visibility = if (state.progress) View.VISIBLE else View.INVISIBLE
        if (state.error != null) {
            errorTitle.visibility = View.VISIBLE
            errorDescription.visibility = View.VISIBLE
            retry.visibility = View.VISIBLE
            errorDescription.text = when (state.error) {
                is IOException -> getString(R.string.error_no_internet)
                else -> getString(R.string.error_unknown)
            }
        } else {
            errorTitle.visibility = View.INVISIBLE
            errorDescription.visibility = View.INVISIBLE
            retry.visibility = View.INVISIBLE
        }
    }
}