package com.epishie.news.model

import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import javax.inject.Inject

class SettingsModel
@Inject constructor(preferences: RxSharedPreferences) {
    private companion object {
        val KEY_DB_INITIALIZED = "db_initialized"
    }

    val dbInitialized: Preference<Boolean> = preferences
            .getBoolean(KEY_DB_INITIALIZED, false)
}