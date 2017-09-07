package com.epishie.news.model.db

import android.arch.persistence.room.*
import io.reactivex.Flowable

@Dao
abstract class SourceDao {
    companion object {
        const val SOURCE_PROJECTION = "SELECT b.id as id, " +
                "   b.name as name, " +
                "   b.url as url, " +
                "   s.selected as selected " +
                "FROM source_bases AS b " +
                "INNER JOIN source_selections AS s " +
                "ON b.id = s.id"
        const val SOURCE_PROJECTION_SELECTED = "$SOURCE_PROJECTION " +
                "WHERE s.selected = 1"
    }

    @Query(SOURCE_PROJECTION)
    abstract fun loadAllSources(): Flowable<List<Db.Source>>
    @Query(SOURCE_PROJECTION_SELECTED)
    abstract fun loadSelectedSources(): Flowable<List<Db.Source>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveSourceBases(sources: List<Db.SourceBase>)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun saveSourceSelections(sources: List<Db.SourceSelection>)
    @Update
    abstract fun updateSourceSelection(source: Db.SourceSelection)
}