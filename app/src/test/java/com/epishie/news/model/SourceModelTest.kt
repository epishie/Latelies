@file:Suppress("IllegalIdentifier")

package com.epishie.news.model

import com.epishie.news.model.SourceModel.Action
import com.epishie.news.model.SourceModel.Result
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
    fun `sources should emit result from DB on init`() {
        // GIVEN
        val subscriber = TestSubscriber<Result>()
        val source = Db.Source("source1", "Source 1", "http://source1.com", false)
        val results = model.observe(Flowable.empty())

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        sources.onNext(listOf(source))

        // THEN
        verify(sourceDao).loadAllSources()
        assertThat(subscriber.values())
                .hasSize(1)
        assertThat(subscriber.values()[0])
                .isInstanceOf(Result.Update::class.java)
    }

    @Test
    fun `Refresh action should fetch from news api, write to DB and should emit Syncing, Synced result`() {
        // GIVEN
        val source = NewsApi.Source("source1", "Source 1", "Description 1",
                "http://source1.com", "general")
        val result = NewsApi.SourceResult("ok", listOf(source))
        whenever(newsApi.getSources()).thenReturn(Flowable.just(result))
        val subscriber = TestSubscriber<Result>()
        val results = model.observe(Flowable.just(Action.Refresh))

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
                .hasSize(2)
                .containsExactly(Result.Syncing, Result.Synced)
    }

    @Test
    fun `Refresh action should emit Syncing, Error result on network error`() {
        // GIVEN
        val error = IOException()
        whenever(newsApi.getSources()).thenReturn(Flowable.error(error))
        val subscriber = TestSubscriber<Result>()
        val results = model.observe(Flowable.just(Action.Refresh))

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        verify(newsApi).getSources()
        verify(sourceDao, never()).saveSourceBases(any())
        verify(sourceDao, never()).saveSourceSelections(any())
        assertThat(subscriber.values())
                .hasSize(2)
                .containsExactly(Result.Syncing, Result.Error(error))
    }

    @Test
    fun `Refresh action should emit Syncing, Error result on API error`() {
        // GIVEN
        val result = NewsApi.SourceResult("error", null)
        whenever(newsApi.getSources()).thenReturn(Flowable.just(result))
        val subscriber = TestSubscriber<Result>()
        val results = model.observe(Flowable.just(Action.Refresh))

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        verify(newsApi).getSources()
        verify(sourceDao, never()).saveSourceBases(any())
        verify(sourceDao, never()).saveSourceSelections(any())
        assertThat(subscriber.values())
                .hasSize(2)
        assertThat(subscriber.values())
                .element(0).isEqualTo(Result.Syncing)
        assertThat(subscriber.values())
                .element(1)
                .extracting("throwable")
                .hasOnlyElementsOfType(NetworkSyncError::class.java)
    }

    @Test
    fun `Refresh action should handle other events after an error`() {
        // GIVEN
        val error = IOException()
        val source = NewsApi.Source("source1", "Source 1", "Description 1",
                "http://source1.com", "general")
        val result = NewsApi.SourceResult("ok", listOf(source))
        whenever(newsApi.getSources()).thenReturn(Flowable.error(error), Flowable.just(result))
        val subscriber = TestSubscriber<Result>()
        val actions = PublishSubject.create<Action>()
        val results = model.observe(actions.toFlowable(BackpressureStrategy.BUFFER))

        // WHEN
        results.subscribe(subscriber)
        actions.onNext(Action.Refresh)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        actions.onNext(Action.Refresh)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        verify(newsApi, times(2)).getSources()
        assertThat(subscriber.values())
                .hasSize(4)
                .containsExactly(Result.Syncing, Result.Error(error), Result.Syncing, Result.Synced)
    }

    @Test
    fun `Refresh action should should not resync if last sync time is less than 5 mins`() {
        // GIVEN
        whenever(syncDao.loadSync("source"))
                .thenReturn(Single.just(Db.Sync("source",
                        Date().time - TimeUnit.MINUTES.toMillis(4))))
        val subscriber = TestSubscriber<Result>()
        val results = model.observe(Flowable.just(Action.Refresh))

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        verify(newsApi, never()).getSources()
    }

    @Test
    fun `Select action should update DB and not emit result`() {
        // GIVEN
        val subscriber = TestSubscriber<Result>()
        val selection = Db.SourceSelection("1", true)
        val results = model.observe(Flowable.just(Action.Select(selection)))

        // WHEN
        results.subscribe(subscriber)
        worker.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        // THEN
        verify(sourceDao).updateSourceSelection(selection)
        assertThat(subscriber.values())
                .isEmpty()
    }
}