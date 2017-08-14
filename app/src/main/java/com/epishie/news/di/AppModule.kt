package com.epishie.news.di

import android.content.Context
import com.epishie.news.App
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
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
}