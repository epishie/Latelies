package com.epishie.news.model

import com.epishie.news.model.db.Db
import com.epishie.news.model.db.NewsDb
import com.epishie.news.model.network.NewsApi
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Scheduler
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class SourceModel
@Inject constructor(newsDb: NewsDb,
                    private val newsApi: NewsApi,
                    @Named("worker") private val worker: Scheduler) {
    private companion object {
        val SYNC_RESOURCE = "source"
        val SYNC_TIME = TimeUnit.DAYS.toMillis(1)
    }
    private val sourceDao = newsDb.sourceDao()
    private val syncDao = newsDb.syncDao()

    fun observe(actions: Flowable<SourceAction>): Flowable<SourceResult> {
        return actions.publish { shared ->
            Flowable.merge(
                    shared.ofType(SourceAction.Get::class.java).flatMap(this::handleGetActions),
                    shared.ofType(SourceAction.Sync::class.java).flatMap(this::handleRefreshActions),
                    shared.ofType(SourceAction.Select::class.java).flatMap(this::handleSelectActions)
            )
        }.subscribeOn(worker)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleGetActions(action: SourceAction.Get): Flowable<SourceResult> {
        return sourceDao.loadAllSources()
                .map { sources ->
                    SourceResult.Update(sources)
                }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleRefreshActions(action: SourceAction.Sync): Flowable<SourceResult> {
        return syncDao.loadSync(SYNC_RESOURCE)
                .toFlowable()
                .onErrorResumeNext(Flowable.just(Db.Sync(SYNC_RESOURCE, 0)))
                .filter { sync ->
                    val difference = Date().time - sync.timestamp
                    return@filter difference >= SYNC_TIME
                }
                .flatMap {
                    newsApi.getSources().publish { shared ->
                        shared.onErrorResumeNext { _: Throwable ->
                            Flowable.empty()
                        }.subscribe(this::saveToDb)

                        shared.map { (_, sources) ->
                            if (sources != null) {
                                SourceResult.Synced
                            } else {
                                SourceResult.Error(NewsApiError())
                            }
                        }.onErrorResumeNext { error: Throwable ->
                            Flowable.just(SourceResult.Error(error))
                        }.startWith(SourceResult.Syncing)                    }
                }
    }

    private fun handleSelectActions(action: SourceAction.Select): Flowable<SourceResult> {
        return Flowable.create<SourceResult>({ emitter ->
            sourceDao.updateSourceSelection(action.selection)
            emitter.onComplete()
        }, BackpressureStrategy.BUFFER).subscribeOn(worker)
    }

    private fun saveToDb(result: NewsApi.SourceResult) {
        if (result.sources != null) {
            sourceDao.saveSourceBases(result.sources.map(this::mapApiSourceToDb))
            sourceDao.saveSourceSelections(result.sources.map { (id) ->
                Db.SourceSelection(id, false)
            })
            syncDao.saveSync(Db.Sync(SYNC_RESOURCE, Date().time))
        }
    }

    private fun mapApiSourceToDb(source: NewsApi.Source): Db.SourceBase {
        return Db.SourceBase(source.id, source.name, source.url)
    }
}

sealed class SourceAction {
    object Get : SourceAction()
    object Sync : SourceAction()
    data class Select(val selection: Db.SourceSelection) : SourceAction()
}

sealed class SourceResult {
    object Syncing : SourceResult()
    object Synced : SourceResult()
    data class Update(val sources: List<Db.Source>) : SourceResult()
    data class Error(val throwable: Throwable) : SourceResult() {
        override fun equals(other: Any?): Boolean {
            return when (other) {
                !is Error -> false
                else -> throwable::class.java == other.throwable::class.java
            }
        }

        override fun hashCode(): Int = throwable.hashCode()
    }
}