package com.epishie.news.features.common

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.epishie.news.features.sources.SourcesViewModel
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class ViewModelFactory
@Inject
constructor(private val sourceVmProvider: Provider<SourcesViewModel>)
    : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when(modelClass) {
            SourcesViewModel::class.java -> sourceVmProvider.get() as T
            else -> throw IllegalArgumentException("Unknown class: ${modelClass.name}")
        }
    }
}