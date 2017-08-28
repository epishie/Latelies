package com.epishie.news.model.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.reactivex.Single

@Dao
abstract class SyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveSync(sync: Db.Sync)
    @Query("SELECT * FROM syncs " +
            "WHERE resource = :resource")
    abstract fun loadSync(resource: String): Single<Db.Sync>
}