package com.epishie.news

import android.app.Activity
import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatDelegate
import com.epishie.news.di.AppComponent
import com.epishie.news.di.DaggerAppComponent
import com.epishie.news.model.db.NewsDb
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.jakewharton.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.OkHttpClient
import javax.inject.Inject

class App : Application() {
    lateinit var component: AppComponent
    @field:[Inject]
    lateinit var httpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        Debuggers.setup(this)

        component = DaggerAppComponent.builder()
                .application(this)
                .build()
        component.inject(this)

        val picasso = Picasso.Builder(this)
                .downloader(OkHttp3Downloader(httpClient))
                .build()
        Picasso.setSingletonInstance(picasso)
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