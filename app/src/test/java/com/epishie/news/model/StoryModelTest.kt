@file:Suppress("IllegalIdentifier")

package com.epishie.news.model

import com.epishie.news.model.db.Db
import com.epishie.news.model.db.NewsDb
import com.epishie.news.model.db.SourceDao
import com.epishie.news.model.db.StoryDao
import com.epishie.news.model.network.NewsApi
import com.epishie.news.model.network.PostLightApi
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subscribers.TestSubscriber
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("MemberVisibilityCanPrivate")
class StoryModelTest {
    lateinit var sourceDao: SourceDao
    lateinit var storyDao: StoryDao
    lateinit var newsDb: NewsDb
    lateinit var newsApi: NewsApi
    lateinit var postLightApi: PostLightApi
    lateinit var model: StoryModel
    lateinit var worker: TestScheduler
    private val testSource = Db.Source("source1", "Source 1", "http://source1.com",
            true)
    private val testStory = Db.Story("http://story1.com", "Story 1", "Story One",
            "Author 1", "http://thumbnail1.com", Date().time, testSource,
            false, null, 200)

    @Before
    fun setUp() {
        sourceDao = mock {
            on { loadSelectedSources() } doReturn Flowable.just(listOf(testSource))
        }
        storyDao = mock {
            on { loadAllStories() } doReturn Flowable.just(listOf(testStory))
            on { loadStory("http://story1.com") } doReturn Flowable.just(listOf(testStory))
            on { loadStoryExtra("http://story1.com") } doReturn Single
                    .just(Db.StoryExtra(testStory.url, testStory.read, testStory.content))
        }
        newsDb = mock {
            on { sourceDao() } doReturn sourceDao
            on { storyDao() } doReturn storyDao
        }
        newsApi = mock()
        postLightApi = mock()

        worker = TestScheduler()
        model = StoryModel(newsDb, newsApi, postLightApi, worker)
    }

    @Test
    fun `observer(Get(null)) should emit stories from db`() {
        // GIVEN
        val subscriber = TestSubscriber<StoryResult>()
        val results = model.observe(Flowable.just(StoryAction.Get()))

        // WHEN - THEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        assertThat(subscriber.values().toList())
                .hasSize(1)
                .hasOnlyElementsOfType(StoryResult.Update::class.java)
        val updates = TestSubscriber<List<Db.Story>>()
        (subscriber.values()[0] as StoryResult.Update).stories.subscribe(updates)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        assertThat(updates.values().toList())
                .containsExactly(listOf(testStory))
    }

    @Test
    fun `observer(Get(url)) should emit stories from db`() {
        // GIVEN
        val subscriber = TestSubscriber<StoryResult>()
        val results = model.observe(Flowable.just(StoryAction.Get("http://story1.com")))

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        assertThat(subscriber.values().toList())
                .hasSize(1)
                .hasOnlyElementsOfType(StoryResult.Update::class.java)
        val updates = TestSubscriber<List<Db.Story>>()
        (subscriber.values()[0] as StoryResult.Update).stories.subscribe(updates)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        assertThat(updates.values().toList())
                .containsExactly(listOf(testStory))
    }

    @Test
    fun `observer(Sync(null)) should emit Syncing, Synced results, fetch all stories from NewsApi and save to DB`() {
        // GIVEN
        val article = NewsApi.Article("http://story1.com", "Story 1", "Story One",
                "Author 1", "http://image1.com", Date())
        val articleResult = NewsApi.ArticleResult("ok", "source1", listOf(article))
        whenever(newsApi.getArticles(eq("source1"), any())).thenReturn(Flowable.just(articleResult))
        val subscriber = TestSubscriber<StoryResult>()
        val results = model.observe(Flowable.just(StoryAction.Sync()))

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        assertThat(subscriber.values().toList())
                .containsExactly(StoryResult.Syncing(), StoryResult.Synced())
        verify(newsApi).getArticles(eq("source1"), any())
        verify(storyDao).saveStoryBases(check { stories ->
            assertThat(stories).containsExactly(Db.StoryBase(article.url, article.title,
                    article.description, "source1", article.author, article.urlToImage,
                    article.publishedAt?.time))
        })
        verify(storyDao).saveStoryExtras(check { extra ->
            assertThat(extra).containsExactly(Db.StoryExtra(article.url, false))
        })
    }

    @Test
    fun `observer(Sync(url)) should emit Syncing, Synced results, fetch all stories from PostLightApi and save to DB`() {
        // GIVEN
        val articleResult = PostLightApi.Result("http://story1.com",
                "<div>Content 1</div>", 200)
        whenever(postLightApi.parseArticle(eq("http://story1.com"), any()))
                .thenReturn(Flowable.just(articleResult))
        val subscriber = TestSubscriber<StoryResult>()
        val results = model.observe(Flowable.just(StoryAction.Sync("http://story1.com")))

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        assertThat(subscriber.values().toList())
                .containsExactly(StoryResult.Syncing("http://story1.com"),
                        StoryResult.Synced("http://story1.com"))
        verify(postLightApi).parseArticle(eq("http://story1.com"), any())
        verify(storyDao).updateStoryExtra(check { extra ->
            assertThat(extra).isEqualTo(Db.StoryExtra(articleResult.url, false,
                    articleResult.content, 200))
        })
    }

    @Test
    fun `observer(Sync(null)) should emit Syncing, Synced results when there is no selected sources`() {
        // GIVEN
        whenever(sourceDao.loadSelectedSources()).thenReturn(Flowable.just(emptyList()))
        val subscriber = TestSubscriber<StoryResult>()
        val results = model.observe(Flowable.just(StoryAction.Sync()))

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        assertThat(subscriber.values().toList())
                .containsExactly(StoryResult.Syncing(), StoryResult.Synced())
        verify(newsApi, never()).getArticles(any(), any())
    }

    @Test
    fun `observer(Sync(null)) should emit Syncing, Error results when there is a network error`() {
        // GIVEN
        val error = IOException()
        whenever(newsApi.getArticles(eq("source1"), any())).thenReturn(Flowable.error(error))
        val subscriber = TestSubscriber<StoryResult>()
        val results = model.observe(Flowable.just(StoryAction.Sync()))

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        assertThat(subscriber.values().toList())
                .containsExactly(StoryResult.Syncing(), StoryResult.Error(error))
    }

    @Test
    fun `observer(Sync(null)) should emit Syncing, Error results when there is a an API error`() {
        // GIVEN
        val articleResult = NewsApi.ArticleResult("error", "source1", null)
        whenever(newsApi.getArticles(eq("source1"), any())).thenReturn(Flowable.just(articleResult))
        val subscriber = TestSubscriber<StoryResult>()
        val results = model.observe(Flowable.just(StoryAction.Sync()))

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        assertThat(subscriber.values().toList())
                .containsExactly(StoryResult.Syncing(), StoryResult.Error(NewsApiError()))
    }
}