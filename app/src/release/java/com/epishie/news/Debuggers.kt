package com.epishie.news

import okhttp3.Interceptor
import okhttp3.Response

object Debuggers {
    fun setup(app: App) {
        // no-op
    }

    fun createInterceptor(): Interceptor {
        return Interceptor { chain -> chain.proceed(chain.request()) }
    }
}