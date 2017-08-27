package com.epishie.news.model

sealed class ModelStatus {
    object Success : ModelStatus()
    data class Error(val throwable: Throwable) : ModelStatus()
}