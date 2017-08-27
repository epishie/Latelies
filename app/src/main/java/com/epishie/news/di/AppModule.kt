package com.epishie.news.di

import android.arch.persistence.room.Room
import android.content.Context
import com.epishie.news.App
import com.epishie.news.model.db.NewsDb
import com.epishie.news.model.network.NewsApi
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
class AppModule {
    @Singleton
    @Provides
    fun provideContext(app: App): Context = app

    @Singleton
    @Named("worker")
    @Provides
    fun providesBgScheduler(): Scheduler = Schedulers.io()

    @Singleton
    @Named("ui")
    @Provides
    fun providesMainScheduler(): Scheduler = AndroidSchedulers.mainThread()

    @Singleton
    @Provides
    fun provideNewsDb(context: Context): NewsDb {
        return Room.inMemoryDatabaseBuilder(context, NewsDb::class.java)
                .build()
    }

    @Singleton
    @Provides
    fun provideNewsApi(): NewsApi {
        return Retrofit.Builder()
                .baseUrl("http://newsapi.org")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(NewsApi::class.java)
    }
}