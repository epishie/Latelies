package com.epishie.news.model.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.reactivex.Flowable

@Dao
abstract class SourceDao {
    @Query("SELECT b.id as id, " +
            "   b.name as name, " +
            "   b.url as url, " +
            "   s.selected as selected " +
            "FROM source_bases AS b " +
            "LEFT JOIN source_selections AS s " +
            "ON b.id = s.id")
    abstract fun loadAllSources(): Flowable<List<Db.Source>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveSourceBases(sources: List<Db.SourceBase>)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun saveSourceSelections(sources: List<Db.SourceSelection>)
}