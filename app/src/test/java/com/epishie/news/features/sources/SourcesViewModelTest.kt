@file:Suppress("IllegalIdentifier")

package com.epishie.news.features.sources

import com.epishie.news.features.common.toLogoUrl
import com.epishie.news.model.NewsApiError
import com.epishie.news.model.SourceAction
import com.epishie.news.model.SourceModel
import com.epishie.news.model.SourceResult
import com.epishie.news.model.db.Db
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Flowable
import io.reactivex.subscribers.TestSubscriber
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class SourcesViewModelTest {
    lateinit var model: SourceModel
    lateinit var vm: SourcesViewModel

    @Before
    fun setUp() {
        model = mock()
        vm = SourcesViewModel(model)
    }

    @Test
    fun `update() should emit an initial state`() {
        // GIVEN
        whenever(model.observe(any()))
                .thenReturn(Flowable.empty())
        val subscriber = TestSubscriber<SourcesViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SourcesViewModel.State())
    }

    @Test
    fun `subsequent update() should emit last state`() {
        // GIVEN
        whenever(model.observe(any()))
                .thenReturn(Flowable.just(SourceResult.Syncing),
                        Flowable.empty())
        val subscriber1 = TestSubscriber<SourcesViewModel.State>()
        val subscriber2 = TestSubscriber<SourcesViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber1)
        subscriber1.dispose()
        vm.update(Flowable.empty())
                .subscribe(subscriber2)

        // THEN
        assertThat(subscriber2.values())
                .containsExactly(SourcesViewModel.State(true))
    }

    @Test
    fun `update() should emit a state with sources`() {
        // GIVEN
        val inputSource = Db.Source("source1", "Source 1", "http://source1.com",
                false)
        whenever(model.observe(any()))
                .thenReturn(Flowable.just(SourceResult.Update(listOf(inputSource))))
        val subscriber = TestSubscriber<SourcesViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber)

        // THEN
        val source = SourcesViewModel.Source("source1", "Source 1",
                "http://source1.com".toLogoUrl(), false)
        assertThat(subscriber.values())
                .containsExactly(SourcesViewModel.State(),
                        SourcesViewModel.State(sources = listOf(source)))
    }

    @Test
    fun `update() should emit states with progress = true and progress = false on sync result`() {
        // GIVEN
        whenever(model.observe(any()))
                .thenReturn(Flowable.just(SourceResult.Syncing, SourceResult.Synced))
        val subscriber = TestSubscriber<SourcesViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SourcesViewModel.State(),
                        SourcesViewModel.State(progress = true),
                        SourcesViewModel.State())
    }

    @Test
    fun `update() should emit states with non-empty error on error result`() {
        // GIVEN
        val error = NewsApiError()
        whenever(model.observe(any()))
                .thenReturn(Flowable.just(SourceResult.Error(error)))
        val subscriber = TestSubscriber<SourcesViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SourcesViewModel.State(),
                        SourcesViewModel.State(error = error))
    }

    @Test
    fun `update(Refresh) should trigger a Refresh action`() {
        // GIVEN
        val subscriber = TestSubscriber<SourceAction>()
        whenever(model.observe(any())).then { invocation ->
            @Suppress("UNCHECKED_CAST")
            val events = (invocation.arguments[0] as Flowable<SourceAction>)
            events.subscribe(subscriber)
            return@then Flowable.empty<SourceAction>()
        }

        // WHEN
        vm.update(Flowable.just(SourcesViewModel.Event.Refresh))

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SourceAction.Sync)
    }

    @Test
    fun `update(Select) should trigger a Select action`() {
        // GIVEN
        val subscriber = TestSubscriber<SourceAction>()
        whenever(model.observe(any())).then { invocation ->
            @Suppress("UNCHECKED_CAST")
            val events = (invocation.arguments[0] as Flowable<SourceAction>)
            events.subscribe(subscriber)
            return@then Flowable.empty<SourceAction>()
        }

        // WHEN
        vm.update(Flowable.just(SourcesViewModel.Event.Select(
                SourcesViewModel.Source("source1", "Source 1", "http://source1.com",
                        true)
        )))

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SourceAction.Select(Db.SourceSelection("source1", true)))
    }
}