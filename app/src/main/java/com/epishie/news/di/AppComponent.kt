package com.epishie.news.di

import com.epishie.news.App
import com.epishie.news.features.sources.SourcesFragment
import com.epishie.news.features.common.ViewModelFactory
import com.epishie.news.features.splash.SplashActivity
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(AppModule::class))
interface AppComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(app: App): Builder
        fun build(): AppComponent
    }

    fun vmFactory(): ViewModelFactory

    fun inject(activity: SplashActivity)
    fun inject(fragment: SourcesFragment)
}