package com.epishie.news

import android.app.Activity
import android.app.Application
import android.arch.persistence.room.Room
import android.support.v4.app.Fragment
import com.epishie.news.di.AppComponent
import com.epishie.news.di.DaggerAppComponent
import com.epishie.news.model.db.NewsDb

class App : Application() {
    lateinit var component: AppComponent

    override fun onCreate() {
        super.onCreate()
        component = DaggerAppComponent.builder()
                .application(this)
                .build()

        Debuggers.setup(this)
    }

    fun createDb(): NewsDb {
        return Room.databaseBuilder(this, NewsDb::class.java, "newsdb")
                .build()
    }
}

val Application.component: AppComponent
    get() = (this as App).component
val Activity.component: AppComponent
    get() = application.component
val Fragment.component: AppComponent
    get() = activity.component