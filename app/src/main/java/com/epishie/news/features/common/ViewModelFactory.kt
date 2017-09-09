package com.epishie.news.features.common

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.epishie.news.features.sources.SourcesViewModel
import com.epishie.news.features.splash.SplashViewModel
import com.epishie.news.features.stories.StoriesViewModel
import com.epishie.news.features.story.StoryViewModel
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class ViewModelFactory
@Inject
constructor(private val splashVmProvider: Provider<SplashViewModel>,
            private val sourceVmProvider: Provider<SourcesViewModel>,
            private val storiesVmProvider: Provider<StoriesViewModel>,
            private val storyVmProvider: Provider<StoryViewModel>)
    : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when(modelClass) {
            SplashViewModel::class.java -> splashVmProvider.get()
            SourcesViewModel::class.java -> sourceVmProvider.get()
            StoriesViewModel::class.java -> storiesVmProvider.get()
            StoryViewModel::class.java -> storyVmProvider.get()
            else -> throw IllegalArgumentException("Unknown class: ${modelClass.name}")
        } as T
    }
}