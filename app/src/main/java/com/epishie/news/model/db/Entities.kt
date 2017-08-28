package com.epishie.news.model.db

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

object Db {
    @Entity(tableName = "syncs")
    data class Sync(
            @PrimaryKey val resource: String,
            val timestamp: Long)

    data class Source(val id: String, val name: String, val url: String, val selected: Boolean)
    @Entity(tableName = "source_bases")
    data class SourceBase(
            @PrimaryKey val id: String,
            val name: String,
            val url: String)
    @Entity(tableName = "source_selections")
    data class SourceSelection(
            @PrimaryKey val id: String,
            val selected: Boolean)
}