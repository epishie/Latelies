package com.epishie.news

import android.app.Activity
import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import android.support.v4.app.Fragment
import com.epishie.news.di.AppComponent
import com.epishie.news.di.DaggerAppComponent
import com.epishie.news.model.db.NewsDb
import com.f2prateek.rx.preferences2.RxSharedPreferences

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

    fun createSharedPrefs(): RxSharedPreferences {
        return RxSharedPreferences.create(getSharedPreferences("settings",
                Context.MODE_PRIVATE))
    }
}

val Application.component: AppComponent
    get() = (this as App).component
val Activity.component: AppComponent
    get() = application.component
val Fragment.component: AppComponent
    get() = activity.component