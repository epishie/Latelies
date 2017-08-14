package com.epishie.news

import android.app.Activity
import android.app.Application
import android.support.v4.app.Fragment
import com.epishie.news.di.AppComponent
import com.epishie.news.di.DaggerAppComponent

class App : Application() {
    lateinit var component: AppComponent

    override fun onCreate() {
        super.onCreate()
        component = DaggerAppComponent.builder()
                .application(this)
                .build()
    }
}

val Application.component: AppComponent
    get() = (this as App).component
val Activity.component: AppComponent
    get() = application.component
val Fragment.component: AppComponent
    get() = activity.component