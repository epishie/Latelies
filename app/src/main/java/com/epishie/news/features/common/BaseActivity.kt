package com.epishie.news.features.common

import android.support.v7.app.AppCompatActivity
import io.reactivex.disposables.CompositeDisposable

abstract class BaseActivity : AppCompatActivity() {
    protected val disposables =  CompositeDisposable()

    override fun onDestroy() {
        disposables.dispose()
        super.onDestroy()
    }
}