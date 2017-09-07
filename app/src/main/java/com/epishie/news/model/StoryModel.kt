package com.epishie.news.model

import com.epishie.news.BuildConfig
import com.epishie.news.model.db.Db
import com.epishie.news.model.db.NewsDb
import com.epishie.news.model.network.NewsApi
import io.reactivex.Flowable
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named

class StoryModel
@Inject constructor(newsDb: NewsDb,
                    private val newsApi: NewsApi,
                    @Named("worker") private val worker: Scheduler) {
    private val sourceDao = newsDb.sourceDao()
    private val storyDao = newsDb.storyDao()

    fun observe(actions: Flowable<StoriesAction>): Flowable<StoriesResult> {
        val dbUpdates = storyDao.loadAllStories()
                .map { stories -> StoriesResult.Update(stories) as StoriesResult }
                .subscribeOn(worker)
        return Flowable.merge(dbUpdates, createResultsFromActions(actions))
    }

    private fun createResultsFromActions(actions: Flowable<StoriesAction>): Flowable<StoriesResult> {
        return actions.flatMap {
                    sourceDao.loadSelectedSources()
                }
                .flatMap { sources ->
                    if (sources.isEmpty()) {
                        Flowable.just(StoriesResult.Synced)
                    } else {
                        Flowable.fromIterable(sources)
                                .concatMap { (id) ->
                                    val sync = newsApi.getArticles(id, BuildConfig.NEWS_API_KEY)
                                            .subscribeOn(worker)
                                            .publish()
                                            .autoConnect(2)
                                    sync.onErrorResumeNext { _: Throwable ->
                                        Flowable.empty()
                                    }.subscribe { result ->
                                        saveToDb(id, result)
                                    }

                                    return@concatMap sync.map { result ->
                                        if (result.articles != null) {
                                            StoriesResult.Synced
                                        } else {
                                            StoriesResult.Error(NewsApiError())
                                        }
                                    }
                                }
                                .onErrorResumeNext { error: Throwable ->
                                    Flowable.just(StoriesResult.Error(error))
                                }
                                .startWith(StoriesResult.Syncing)
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

sealed class StoriesAction {
    object Sync : StoriesAction()
}

sealed class StoriesResult {
    object Syncing : StoriesResult()
    object Synced : StoriesResult()
    data class Update(val sources: List<Db.Story>) : StoriesResult()
    data class Error(val throwable: Throwable) : StoriesResult() {
        override fun equals(other: Any?): Boolean {
            return when (other) {
                !is Error -> false
                else -> throwable::class.java == other.throwable::class.java
            }
        }
        override fun hashCode(): Int = throwable.hashCode()
    }
}

sealed class StoryAction {
    data class LoadStory(val url: String)
}

sealed class StoryResult {
    object Loading : StoryResult()
    data class Loaded(val story: Db.Story) : StoryResult()
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
