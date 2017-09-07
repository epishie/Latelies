package com.epishie.news.model.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(
        entities = arrayOf(Db.SourceBase::class, Db.SourceSelection::class, Db.StoryBase::class,
                Db.StoryExtra::class, Db.Sync::class),
        version = 1,
        exportSchema = false
)
abstract class NewsDb : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun storyDao(): StoryDao
    abstract fun syncDao(): SyncDao
}