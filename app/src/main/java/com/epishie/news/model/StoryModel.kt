package com.epishie.news.model

import com.epishie.news.BuildConfig
import com.epishie.news.model.db.Db
import com.epishie.news.model.db.NewsDb
import com.epishie.news.model.network.NewsApi
import com.epishie.news.model.network.PostLightApi
import io.reactivex.Flowable
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named

class StoryModel
@Inject constructor(newsDb: NewsDb,
                    private val newsApi: NewsApi,
                    private val postLightApi: PostLightApi,
                    @Named("worker") private val worker: Scheduler) {
    private val sourceDao = newsDb.sourceDao()
    private val storyDao = newsDb.storyDao()

    fun observe(actions: Flowable<StoryAction>): Flowable<StoryResult> {
        return actions.publish { shared ->
            Flowable.merge(
                    shared.ofType(StoryAction.Get::class.java).flatMap(this::handleGetAction),
                    shared.ofType(StoryAction.Sync::class.java).flatMap(this::handleSyncAction)
            )
        }
    }

    private fun handleGetAction(get: StoryAction.Get): Flowable<StoryResult> {
        val dbUpdates = if (get.url != null) {
            storyDao.loadStory(get.url)
        } else {
            storyDao.loadAllStories()
        }
        return Flowable.just(StoryResult.Update(dbUpdates))
    }

    private fun handleSyncAction(sync: StoryAction.Sync): Flowable<StoryResult> {
        return if (sync.url != null) {
            Flowable.empty()
        } else {
            sourceDao.loadSelectedSources().flatMap { sources ->
                if (sources.isEmpty()) {
                    Flowable.just(StoryResult.Synced())
                } else {
                    fetchStories(sources)
                }
            }.startWith(StoryResult.Syncing())
        }
    }

    private fun fetchStories(sources: List<Db.Source>): Flowable<StoryResult> {
        return Flowable.fromIterable(sources)
                .concatMap { (id) ->
                    newsApi.getArticles(id, BuildConfig.NEWS_API_KEY)
                            .subscribeOn(worker)
                            .publish { shared ->
                                shared.onErrorResumeNext { _: Throwable ->
                                    Flowable.empty()
                                }.subscribe { result ->
                                    saveToDb(id, result)
                                }

                                shared.map { result ->
                                    if (result.articles != null) {
                                        StoryResult.Synced()
                                    } else {
                                        StoryResult.Error(NewsApiError())
                                    }
                                }
                            }.onErrorResumeNext { error: Throwable ->
                        Flowable.just(StoryResult.Error(error))
                    }
                }
    }

    private fun saveToDb(source: String, articleResult: NewsApi.ArticleResult) {
        if (articleResult.articles == null) {
            return
        }
        storyDao.saveStoryBases(articleResult.articles.map { (url, title, description, author,
                                                                     urlToImage, publishedAt) ->
            Db.StoryBase(url, title, description, source, author,
                    urlToImage, publishedAt?.time)
        })
        storyDao.saveStoryExtras(articleResult.articles.map { (url) ->
            Db.StoryExtra(url)
        })
    }
}

sealed class StoryAction {
    data class Get(val url: String? = null) : StoryAction()
    data class Sync(val url: String? = null) : StoryAction()
}

sealed class StoryResult {
    data class Syncing(val url: String? = null) : StoryResult()
    data class Synced(val url: String? = null) : StoryResult()
    data class Update(val sources: Flowable<List<Db.Story>>) : StoryResult()
    data class Error(val throwable: Throwable) : StoryResult() {
        override fun equals(other: Any?): Boolean {
            return when (other) {
                !is Error -> false
                else -> throwable::class.java == other.throwable::class.java
            }
        }
        override fun hashCode(): Int = throwable.hashCode()
    }
}