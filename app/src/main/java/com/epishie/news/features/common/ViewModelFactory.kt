package com.epishie.news.features.common

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViewModelFactory
@Inject
constructor()
    : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>?): T {
        return when(modelClass) {
            else -> throw IllegalArgumentException("Unknown class: ${modelClass?.name}")

        }
    }
}