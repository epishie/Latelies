package com.epishie.news.model.db

import android.arch.persistence.room.*
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
abstract class StoryDao {
    companion object {
        const val STORY_QUERY = "SELECT b.url AS url, " +
                "   b.title AS title, " +
                "   b.description AS description, " +
                "   b.author AS author, " +
                "   b.thumbnail AS thumbnail, " +
                "   b.date AS date, " +
                "   s.id AS source_id, " +
                "   s.name AS source_name, " +
                "   s.url AS source_url, " +
                "   s.selected AS source_selected, " +
                "   e.read AS read, " +
                "   e.content AS content " +
                "FROM story_bases AS b " +
                "INNER JOIN story_extras AS e ON b.url = e.url " +
                "INNER JOIN (${SourceDao.SOURCE_SELECTED_QUERY}) AS s ON b.source = s.id"
        const val STORY_BY_URL_QUERY = "$STORY_QUERY " +
                "WHERE b.url = :url"
    }

    @Query(STORY_QUERY + " ORDER BY date DESC")
    abstract fun loadAllStories(): Flowable<List<Db.Story>>
    @Query(STORY_BY_URL_QUERY)
    abstract fun loadStory(url: String): Flowable<List<Db.Story>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveStoryBases(stories: List<Db.StoryBase>)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun saveStoryExtras(stories: List<Db.StoryExtra>)
    @Query("SELECT * " +
            "FROM story_extras e " +
            "WHERE e.url = :url")
    abstract fun loadStoryExtra(url: String): Single<Db.StoryExtra>
    @Update
    abstract fun updateStoryExtra(extra: Db.StoryExtra)
}