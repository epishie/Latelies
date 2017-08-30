package com.epishie.news.features.splash

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.epishie.news.R
import com.epishie.news.component
import com.epishie.news.features.stories.StoriesActivity
import io.reactivex.Flowable
import io.reactivex.Scheduler
import kotlinx.android.synthetic.main.splash_activity.*
import javax.inject.Inject
import javax.inject.Named

class SplashActivity : AppCompatActivity() {
    lateinit var vm: SplashViewModel
    @field:[Inject Named("ui")]
    lateinit var ui: Scheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        vm = ViewModelProviders.of(this, component.vmFactory())[SplashViewModel::class.java]

        setContentView(R.layout.splash_activity)
        setupView()
    }

    fun setupView() {
        vm.update(Flowable.empty())
                .observeOn(ui)
                .subscribe { state ->
                    if (state.success) {
                        startActivity(Intent(this, StoriesActivity::class.java))
                        finish()
                        return@subscribe
                    }

                    progress.visibility = if (state.progress) View.VISIBLE else View.GONE
                }
    }
}