package com.epishie.news.model

import com.epishie.news.BuildConfig
import com.epishie.news.model.db.Db
import com.epishie.news.model.db.NewsDb
import com.epishie.news.model.network.NewsApi
import com.epishie.news.model.network.PostLightApi
import io.reactivex.Flowable
import io.reactivex.Scheduler
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
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
        }.subscribeOn(worker)
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
            parseStory(sync.url)
                    .startWith(StoryResult.Syncing(sync.url))
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
                            .map { result ->
                                if (result.articles != null) {
                                    FetchStoriesResult.Success(result.articles.map {
                                        (url, title, description, author, urlToImage, publishedAt) ->
                                        Db.StoryBase(url, title, description,
                                                id, author, urlToImage,
                                                publishedAt?.time)
                                    })
                                } else {
                                    throw NewsApiError()
                                } as FetchStoriesResult
                            }
                            .onErrorResumeNext { throwable: Throwable ->
                                Flowable.just(FetchStoriesResult.Error(throwable))
                            }
                }
                .collect({ FetchStoriesSummary() }, { summary, result ->
                    when (result) {
                        is FetchStoriesResult.Error -> {
                            if (summary.error !is IOException) {
                                summary.error = result.error
                            }
                        }
                        is FetchStoriesResult.Success -> {
                            summary.stories.addAll(result.stories)
                        }
                    }
                })
                .toFlowable()
                .publish { shared ->
                    shared.map{ summary -> summary.stories}
                            .subscribe(this::saveToDb)
                    shared.map { (error, _) ->
                        if (error != null) {
                            StoryResult.Error(error)
                        } else {
                            StoryResult.Synced()
                        }
                    }
                }
    }

    private fun parseStory(url: String): Flowable<StoryResult> {
        return storyDao.loadStoryExtra(url)
                .toFlowable()
                .flatMap { extra ->
                    postLightApi.parseArticle(extra.url, BuildConfig.POST_LIGHT_API_KEY)
                            .publish { shared ->
                                shared.onErrorResumeNext { _: Throwable ->
                                    Flowable.empty()
                                }.subscribe { (_, content, word_count) ->
                                    storyDao.updateStoryExtra(extra.copy(content = content,
                                            wordCount = word_count))
                                }

                                shared.map {
                                    StoryResult.Synced(url) as StoryResult
                                }.onErrorResumeNext { error: Throwable ->
                                    Flowable.just(StoryResult.Error(error))
                                }
                            }
                }
    }

    private fun saveToDb(stories: List<Db.StoryBase>) {
        storyDao.saveStoryBases(stories)
        storyDao.saveStoryExtras(stories.map { (url) ->
            Db.StoryExtra(url)
        })
    }

    sealed class FetchStoriesResult {
        data class Success(val stories: List<Db.StoryBase>) : FetchStoriesResult()
        data class Error(val error: Throwable) : FetchStoriesResult()
    }
    data class FetchStoriesSummary(var error: Throwable? = null,
                                   val stories: MutableList<Db.StoryBase> = mutableListOf())
}

sealed class StoryAction {
    data class Get(val url: String? = null) : StoryAction()
    data class Sync(val url: String? = null) : StoryAction()
}

sealed class StoryResult {
    data class Syncing(val url: String? = null) : StoryResult()
    data class Synced(val url: String? = null) : StoryResult()
    data class Update(val stories: Flowable<List<Db.Story>>) : StoryResult()
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