package com.epishie.news

import com.facebook.stetho.Stetho

object Debuggers {
    fun setup(app: App) {
        Stetho.initializeWithDefaults(app)
    }
}