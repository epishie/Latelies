package com.epishie.news.di

import com.epishie.news.App
import com.epishie.news.features.sources.SourcesFragment
import com.epishie.news.features.common.ViewModelFactory
import com.epishie.news.features.splash.SplashActivity
import com.epishie.news.features.stories.StoriesActivity
import com.epishie.news.features.story.StoryActivity
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

    fun inject(app: App)
    fun inject(activity: SplashActivity)
    fun inject(activity: StoriesActivity)
    fun inject(activity: StoryActivity)
    fun inject(fragment: SourcesFragment)
}