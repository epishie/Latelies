package com.epishie.news.model

sealed class ModelStatus {
    data class Success<out T>(val result: T) : ModelStatus()
    data class Error(val throwable: Throwable) : ModelStatus()
}