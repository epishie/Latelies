package com.epishie.news.model.db

import android.arch.persistence.room.Embedded
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

    data class Story(
            val url: String,
            val title: String,
            val description: String?,
            val author: String?,
            val thumbnail: String?,
            val date: Long?,
            @Embedded(prefix = "source_") val source: Source,
            val read: Boolean,
            val content: String?)
    @Entity(tableName = "story_bases")
    data class StoryBase(
            @PrimaryKey val url: String,
            val title: String,
            val description: String?,
            val source: String,
            val author: String?,
            val thumbnail: String?,
            val date: Long?)
    @Entity(tableName = "story_extras")
    data class StoryExtra(
            @PrimaryKey val url: String,
            val read: Boolean = false,
            val content: String? = null)
}