@file:Suppress("IllegalIdentifier")

package com.epishie.news.features.sources

import com.epishie.news.features.common.toLogoUrl
import com.epishie.news.model.NetworkSyncError
import com.epishie.news.model.SourceModel
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
    fun `should emit an initial state`() {
        // GIVEN
        whenever(model.observe(any()))
                .thenReturn(Flowable.empty())
        val subscriber = TestSubscriber<SourcesViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .hasSize(1)
                .containsExactly(SourcesViewModel.State(false, "", emptyList()))
    }

    @Test
    fun `state should be retained on subsequent update calls`() {
        // GIVEN
        whenever(model.observe(any()))
                .thenReturn(Flowable.just(SourceModel.Result.Syncing),
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
                .containsExactly(SourcesViewModel.State(true, "", emptyList()))
    }

    @Test
    fun `DB update should emit a state with sources`() {
        // GIVEN
        val inputSource = Db.Source("source1", "Source 1", "http://source1.com",
                false)
        whenever(model.observe(any()))
                .thenReturn(Flowable.just(SourceModel.Result.Update(listOf(inputSource))))
        val subscriber = TestSubscriber<SourcesViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber)

        // THEN
        val source = SourcesViewModel.Source("source1", "Source 1",
                "http://source1.com".toLogoUrl(), false)
        assertThat(subscriber.values())
                .containsExactly(SourcesViewModel.State(false, "", emptyList()),
                        SourcesViewModel.State(false, "", listOf(source)))
    }

    @Test
    fun `Sync should emit states with progress = true and progress = false`() {
        // GIVEN
        whenever(model.observe(any()))
                .thenReturn(Flowable.just(SourceModel.Result.Syncing, SourceModel.Result.Synced))
        val subscriber = TestSubscriber<SourcesViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SourcesViewModel.State(false, "", emptyList()),
                        SourcesViewModel.State(true, "", emptyList()),
                        SourcesViewModel.State(false, "", emptyList()))
    }

    @Test
    fun `Error should emit states with non-empty error`() {
        // GIVEN
        whenever(model.observe(any()))
                .thenReturn(Flowable.just(SourceModel.Result.Error(NetworkSyncError())))
        val subscriber = TestSubscriber<SourcesViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SourcesViewModel.State(false, "", emptyList()),
                        SourcesViewModel.State(false, "Error", emptyList()))
    }

    @Test
    fun `Refresh event should trigger a Refresh action`() {
        // GIVEN
        val subscriber = TestSubscriber<SourceModel.Action>()
        whenever(model.observe(any())).then { invocation ->
            @Suppress("UNCHECKED_CAST")
            val events = (invocation.arguments[0] as Flowable<SourceModel.Action>)
            events.subscribe(subscriber)
            return@then Flowable.empty<SourceModel.Action>()
        }

        // WHEN
        vm.update(Flowable.just(SourcesViewModel.Event.Refresh))

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SourceModel.Action.Sync)
    }

    @Test
    fun `Select event should trigger a Select action`() {
        // GIVEN
        val subscriber = TestSubscriber<SourceModel.Action>()
        whenever(model.observe(any())).then { invocation ->
            @Suppress("UNCHECKED_CAST")
            val events = (invocation.arguments[0] as Flowable<SourceModel.Action>)
            events.subscribe(subscriber)
            return@then Flowable.empty<SourceModel.Action>()
        }

        // WHEN
        vm.update(Flowable.just(SourcesViewModel.Event.Select(
                SourcesViewModel.Source("source1",
                        "Source 1",
                        "http://source1.com",
                        true)
        )))

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SourceModel.Action.Select(Db.SourceSelection("source1", true)))
    }
}