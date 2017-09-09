package com.epishie.news.features.splash

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.epishie.news.R
import com.epishie.news.component
import com.epishie.news.features.common.BaseActivity
import com.epishie.news.features.stories.StoriesActivity
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import io.reactivex.BackpressureStrategy
import io.reactivex.Scheduler
import kotlinx.android.synthetic.main.splash_activity.*
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

class SplashActivity : BaseActivity() {
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
                .subscribeOn(ui)
                .map { SplashViewModel.Event.RetryEvent as SplashViewModel.Event }
                .toFlowable(BackpressureStrategy.BUFFER)
        val states = vm.update(events)
                .observeOn(ui)
                .publish()
        states.filter { state -> state.success }
                .distinctUntilChanged()
                .subscribe {
                    startActivity(Intent(this, StoriesActivity::class.java))
                    finish()
                }
        states.map { (progress) -> progress }
                .subscribe(progress.visibility(View.GONE))
        states.filter { state -> state.error != null }
                .map { state ->
                    when (state.error) {
                        is IOException -> getString(R.string.error_no_internet)
                        else -> getString(R.string.error_unknown)
                    }
                }.subscribe(errorDescription.text())
        val error = states.map { state -> state.error != null }
                .publish()
        error.subscribe(errorTitle.visibility(View.GONE))
        error.subscribe(errorDescription.visibility(View.GONE))
        error.subscribe(retry.visibility(View.GONE))
        error.connect()

        disposables.add(states.connect())
    }
}