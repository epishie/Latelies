@file:Suppress("IllegalIdentifier")

package com.epishie.news.model

import com.epishie.news.model.db.Db
import com.epishie.news.model.db.NewsDb
import com.epishie.news.model.db.SourceDao
import com.epishie.news.model.db.SyncDao
import com.epishie.news.model.network.NewsApi
import com.nhaarman.mockito_kotlin.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import io.reactivex.subscribers.TestSubscriber
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class SourceModelTest {
    lateinit var sourceDao: SourceDao
    lateinit var syncDao: SyncDao
    lateinit var newsApi: NewsApi
    lateinit var worker: TestScheduler
    lateinit var sources: PublishSubject<List<Db.Source>>
    lateinit var model: SourceModel

    @Before
    fun setUp() {
        sources = PublishSubject.create()
        sourceDao = mock {
            on { loadAllSources() } doReturn sources.toFlowable(BackpressureStrategy.BUFFER)
        }
        syncDao = mock {
            on { loadSync("source") } doReturn
                Single.just(Db.Sync("source",
                        Date().time - TimeUnit.DAYS.toMillis(2)))
        }
        newsApi = mock()
        worker = TestScheduler()
        val newsDb: NewsDb = mock {
            on { sourceDao() } doReturn sourceDao
            on { syncDao() } doReturn syncDao
        }
        model = SourceModel(newsDb, newsApi, worker)
    }

    @Test
    fun `observe(Get) should emit Update result from DB`() {
        // GIVEN
        val subscriber = TestSubscriber<SourceResult>()
        val source = Db.Source("source1", "Source 1", "http://source1.com", false)
        val results = model.observe(Flowable.just(SourceAction.Get))

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        sources.onNext(listOf(source))

        // THEN
        verify(sourceDao).loadAllSources()
        assertThat(subscriber.values())
                .containsExactly(SourceResult.Update(listOf(source)))
    }

    @Test
    fun `observe(Sync) should fetch from news api, write to DB and should emit Syncing, Synced results`() {
        // GIVEN
        val source = NewsApi.Source("source1", "Source 1", "Description 1",
                "http://source1.com", "general")
        val result = NewsApi.SourceResult("ok", listOf(source))
        whenever(newsApi.getSources()).thenReturn(Flowable.just(result))
        val subscriber = TestSubscriber<SourceResult>()
        val results = model.observe(Flowable.just(SourceAction.Sync))

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        verify(newsApi).getSources()
        verify(sourceDao).saveSourceBases(check { bases ->
            assertThat(bases)
                    .containsExactly(Db.SourceBase("source1", "Source 1",
                            "http://source1.com"))
        })
        verify(sourceDao).saveSourceSelections(check { selections ->
            assertThat(selections)
                    .containsExactly(Db.SourceSelection("source1", false))
        })
        verify(syncDao).saveSync(argThat {
            resource == "source"
        })
        assertThat(subscriber.values())
                .containsExactly(SourceResult.Syncing, SourceResult.Synced)
    }

    @Test
    fun `observe(Sync) action should emit Syncing, Error results on network error`() {
        // GIVEN
        val error = IOException()
        whenever(newsApi.getSources()).thenReturn(Flowable.error(error))
        val subscriber = TestSubscriber<SourceResult>()
        val results = model.observe(Flowable.just(SourceAction.Sync))

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        verify(newsApi).getSources()
        verify(sourceDao, never()).saveSourceBases(any())
        verify(sourceDao, never()).saveSourceSelections(any())
        assertThat(subscriber.values())
                .containsExactly(SourceResult.Syncing, SourceResult.Error(error))
    }

    @Test
    fun `observe(Sync) should emit Syncing, Error results on API error`() {
        // GIVEN
        val result = NewsApi.SourceResult("error", null)
        whenever(newsApi.getSources()).thenReturn(Flowable.just(result))
        val subscriber = TestSubscriber<SourceResult>()
        val results = model.observe(Flowable.just(SourceAction.Sync))

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        verify(newsApi).getSources()
        verify(sourceDao, never()).saveSourceBases(any())
        verify(sourceDao, never()).saveSourceSelections(any())
        assertThat(subscriber.values())
                .containsExactly(SourceResult.Syncing, SourceResult.Error(NewsApiError()))
    }

    @Test
    fun `observe(Sync) should handle other events after an error`() {
        // GIVEN
        val error = IOException()
        val source = NewsApi.Source("source1", "Source 1", "Description 1",
                "http://source1.com", "general")
        val result = NewsApi.SourceResult("ok", listOf(source))
        whenever(newsApi.getSources()).thenReturn(Flowable.error(error), Flowable.just(result))
        val subscriber = TestSubscriber<SourceResult>()
        val actions = PublishSubject.create<SourceAction>()
        val results = model.observe(actions.toFlowable(BackpressureStrategy.BUFFER))

        // WHEN
        results.subscribe(subscriber)
        actions.onNext(SourceAction.Sync)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        actions.onNext(SourceAction.Sync)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        verify(newsApi, times(2)).getSources()
        assertThat(subscriber.values())
                .containsExactly(SourceResult.Syncing, SourceResult.Error(error),
                        SourceResult.Syncing, SourceResult.Synced)
    }

    @Test
    fun `observe(Sync) action should should not resync if last sync time is less than 5 mins`() {
        // GIVEN
        whenever(syncDao.loadSync("source"))
                .thenReturn(Single.just(Db.Sync("source",
                        Date().time - TimeUnit.MINUTES.toMillis(4))))
        val subscriber = TestSubscriber<SourceResult>()
        val results = model.observe(Flowable.just(SourceAction.Sync))

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        verify(newsApi, never()).getSources()
    }

    @Test
    fun `observe(Select) should update DB and not emit result`() {
        // GIVEN
        val subscriber = TestSubscriber<SourceResult>()
        val selection = Db.SourceSelection("1", true)
        val results = model.observe(Flowable.just(SourceAction.Select(selection)))

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        verify(sourceDao).updateSourceSelection(selection)
        assertThat(subscriber.values())
                .isEmpty()
    }
}