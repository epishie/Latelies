package com.epishie.news

import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import okhttp3.Interceptor

object Debuggers {
    fun setup(app: App) {
        Stetho.initializeWithDefaults(app)
    }

    fun createInterceptor(): Interceptor {
        return StethoInterceptor()
    }
}