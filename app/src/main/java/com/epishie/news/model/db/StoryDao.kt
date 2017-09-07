package com.epishie.news.model.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.reactivex.Flowable

@Dao
abstract class StoryDao {
    companion object {
        const val STORY_PROJECTION = "SELECT b.url AS url, " +
                "   b.title AS title, " +
                "   b.description AS description, " +
                "   b.author AS author, " +
                "   b.thumbnail AS thumbnail, " +
                "   b.date AS date, " +
                "   s.id AS source_id, " +
                "   s.name AS source_name, " +
                "   s.url AS source_url, " +
                "   s.selected AS source_selected, " +
                "   r.read AS read " +
                "FROM story_bases AS b " +
                "INNER JOIN story_extras AS r ON b.url = r.url " +
                "INNER JOIN (${SourceDao.SOURCE_PROJECTION_SELECTED}) AS s ON b.source = s.id " +
                "ORDER BY date DESC"
    }

    @Query(STORY_PROJECTION)
    abstract fun loadAllStories(): Flowable<List<Db.Story>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveStoryBases(stories: List<Db.StoryBase>)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun saveStoryExtras(stories: List<Db.StoryExtra>)
}