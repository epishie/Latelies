@file:Suppress("IllegalIdentifier")

package com.epishie.news.model

import com.epishie.news.model.db.Db
import com.epishie.news.model.db.NewsDb
import com.epishie.news.model.db.SourceDao
import com.epishie.news.model.db.StoryDao
import com.epishie.news.model.network.NewsApi
import com.nhaarman.mockito_kotlin.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import io.reactivex.subscribers.TestSubscriber
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class StoryModelTest {
    lateinit var sourceDao: SourceDao
    lateinit var storyDao: StoryDao
    lateinit var newsDb: NewsDb
    lateinit var newsApi: NewsApi
    lateinit var model: StoryModel
    lateinit var worker: TestScheduler
    lateinit var sources: PublishSubject<List<Db.Source>>
    lateinit var stories: PublishSubject<List<Db.Story>>
    private val testSource = Db.Source("source1",
                    "Source 1", "http://source1.com", true)
    private val testStory = Db.Story("http://story1.com", "Story 1", "Story One",
            "Author 1", "http://thumbnail1.com", Date().time, testSource,
            false, null)

    @Before
    fun setUp() {
        sources = PublishSubject.create()
        sourceDao = mock {
            on { loadSelectedSources() } doReturn sources.toFlowable(BackpressureStrategy.BUFFER)
        }
        stories = PublishSubject.create()
        storyDao = mock {
            on { loadAllStories() } doReturn stories.toFlowable(BackpressureStrategy.BUFFER)
        }
        newsDb = mock {
            on { sourceDao() } doReturn sourceDao
            on { storyDao() } doReturn storyDao
        }
        newsApi = mock {
        }

        worker = TestScheduler()
        model = StoryModel(newsDb, newsApi, worker)
    }

    @Test
    fun `observe() should emit stories from db`() {
        // GIVEN
        val subscriber = TestSubscriber<StoriesResult>()
        val results = model.observe(Flowable.empty())

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        stories.onNext(listOf(testStory))

        // THEN
        assertThat(subscriber.values())
                .containsExactly(StoriesResult.Update(listOf(testStory)))
    }

    @Test
    fun `observe(Sync) should fetch from news api, write to DB and should emit Syncing, Synced result`() {
        // GIVEN
        val article = NewsApi.Article("http://story1.com", "Story 1", "Story One",
                "Author 1", "http://image1.com", Date())
        val articleResult = NewsApi.ArticleResult("ok", "source1", listOf(article))
        whenever(newsApi.getArticles(eq("source1"), any())).thenReturn(Flowable.just(articleResult))
        val subscriber = TestSubscriber<StoriesResult>()
        val results = model.observe(Flowable.just(StoriesAction.Sync))

        // WHEN
        results.subscribe(subscriber)
        sources.onNext(listOf(testSource))
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        verify(newsApi).getArticles(eq("source1"), any())
        verify(storyDao).saveStoryBases(check { stories ->
            assertThat(stories).containsExactly(Db.StoryBase(article.url, article.title,
                    article.description, "source1", article.author, article.urlToImage,
                    article.publishedAt?.time))
        })
        verify(storyDao).saveStoryExtras(check { extra ->
            assertThat(extra).containsExactly(Db.StoryExtra(article.url, false))
        })
        assertThat(subscriber.values())
                .containsExactly(StoriesResult.Syncing, StoriesResult.Synced)
    }

    @Test
    fun `observe(Sync) should should emit Syncing, Synced result when there is no selected sources`() {
        // GIVEN
        whenever(sourceDao.loadSelectedSources()).thenReturn(Flowable.just(emptyList()))
        val subscriber = TestSubscriber<StoriesResult>()
        val results = model.observe(Flowable.just(StoriesAction.Sync))

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        verify(newsApi, never()).getArticles(any(), any())
        assertThat(subscriber.values())
                .containsExactly(StoriesResult.Synced)
    }

    @Test
    fun `observe(Sync) should emit Syncing, Error result on network error`() {
        // GIVEN
        whenever(newsApi.getArticles(eq("source1"), any())).thenReturn(Flowable.error(IOException()))
        val subscriber = TestSubscriber<StoriesResult>()
        val results = model.observe(Flowable.just(StoriesAction.Sync))

        // WHEN
        results.subscribe(subscriber)
        sources.onNext(listOf(testSource))
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        verify(newsApi).getArticles(eq("source1"), any())
        verify(storyDao, never()).saveStoryBases(any())
        verify(storyDao, never()).saveStoryExtras(any())
        assertThat(subscriber.values())
                .containsExactly(StoriesResult.Syncing, StoriesResult.Error(IOException()))
    }

    @Test
    fun `observe(Sync) should emit Syncing, Error result on API error`() {
        // GIVEN
        val result = NewsApi.ArticleResult("error", "source1", null)
        whenever(newsApi.getArticles(eq("source1"), any())).thenReturn(Flowable.just(result))
        val subscriber = TestSubscriber<StoriesResult>()
        val results = model.observe(Flowable.just(StoriesAction.Sync))

        // WHEN
        results.subscribe(subscriber)
        sources.onNext(listOf(testSource))
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        verify(newsApi).getArticles(eq("source1"), any())
        verify(storyDao, never()).saveStoryBases(any())
        verify(storyDao, never()).saveStoryExtras(any())
        assertThat(subscriber.values())
                .containsExactly(StoriesResult.Syncing, StoriesResult.Error(NewsApiError()))
    }
}