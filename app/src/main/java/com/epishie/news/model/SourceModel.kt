package com.epishie.news.model

import com.epishie.news.model.db.Db
import com.epishie.news.model.db.NewsDb
import com.epishie.news.model.network.NewsApi
import io.reactivex.Flowable
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named

class SourceModel
@Inject constructor(newsDb: NewsDb,
                    private val newsApi: NewsApi,
                    @Named("worker") private val worker: Scheduler) {
    private val sourceDao = newsDb.sourceDao()
    private val dbUpdates: Flowable<Result>
        get() {
            return sourceDao.loadAllSources()
                    .map { sources -> Result.Update(sources) as Result }
                    .subscribeOn(worker)
        }

    fun observe(actions: Flowable<Action>): Flowable<Result> {
        return Flowable.merge(dbUpdates, createResultsFromActions(actions))
    }

    private fun createResultsFromActions(actions: Flowable<Action>): Flowable<Result> {
        return actions.flatMap {
            val sync = newsApi.getSources()
                    .subscribeOn(worker)
                    .publish()
                    .autoConnect(2)

            // DB sync
            sync.onErrorResumeNext { _: Throwable ->
                Flowable.empty()
            }.subscribe(this::saveToDb)

            return@flatMap sync.map { (_, sources) ->
                if (sources != null) {
                    Result.Synced
                } else {
                    Result.Error(NetworkSyncError())
                }
            }.onErrorResumeNext { error: Throwable ->
                Flowable.just(Result.Error(error))
            }.startWith(Result.Syncing)
        }
    }

    private fun saveToDb(result: NewsApi.SourceResult) {
        if (result.sources != null) {
            sourceDao.saveSourceBases(result.sources.map(this::mapApiSourceToDb))
            sourceDao.saveSourceSelections(result.sources.map { (id) ->
                Db.SourceSelection(id, false)
            })
        }
    }

    private fun mapApiSourceToDb(source: NewsApi.Source): Db.SourceBase {
        return Db.SourceBase(source.id, source.name, source.url)
    }

    sealed class Result {
        object Syncing : Result()
        object Synced : Result()
        data class Update(val sources: List<Db.Source>) : Result()
        data class Error(val throwable: Throwable) : Result()
    }
    sealed class Action {
        object Refresh : Action()
    }
}