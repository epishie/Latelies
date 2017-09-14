@file:Suppress("IllegalIdentifier")

package com.epishie.news.features.story

import com.epishie.news.features.common.toLogoUrl
import com.epishie.news.model.NewsApiError
import com.epishie.news.model.StoryAction
import com.epishie.news.model.StoryModel
import com.epishie.news.model.StoryResult
import com.epishie.news.model.db.Db
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Flowable
import io.reactivex.subscribers.TestSubscriber
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.*

class StoryViewModelTest {
    lateinit var model: StoryModel
    lateinit var vm: StoryViewModel

    @Before
    fun setUp() {
        model = mock()
        vm = StoryViewModel(model)
    }

    @Test
    fun `update() should emit an initial state`() {
        // GIVEN
        whenever(model.observe(any()))
                .thenReturn(Flowable.empty())
        val subscriber = TestSubscriber<StoryViewModel.State>()

        // WHEN
        vm.update("http://story1.com", Flowable.empty())
                .subscribe(subscriber)

        // THEN
        assertThat(subscriber.values().toList())
                .containsExactly(StoryViewModel.State())
    }

    @Test
    fun `subsequent update() should emit last state`() {
        // GIVEN
        whenever(model.observe(any()))
                .thenReturn(Flowable.just(StoryResult.Syncing()),
                        Flowable.empty())
        val subscriber1 = TestSubscriber<StoryViewModel.State>()
        val subscriber2 = TestSubscriber<StoryViewModel.State>()

        // WHEN
        vm.update("http://story1.com", Flowable.empty())
                .subscribe(subscriber1)
        subscriber1.dispose()
        vm.update("http://story1.com", Flowable.empty())
                .subscribe(subscriber2)

        // THEN
        assertThat(subscriber2.values())
                .containsExactly(StoryViewModel.State(progress = true))
    }

    @Test
    fun `update() should emit state with story`() {
        // GIVEN
        val time = Date().time
        val source = Db.Source("source1", "Source 1", "http://source1.com",
                true)
        val inputStory = Db.Story("http://story1.com", "Story 1", "Story One",
                "Author1", "http://image1.com", time, source, false,
                "This is story one", 200)
        whenever(model.observe(any()))
                .thenReturn(Flowable.just(StoryResult.Update(Flowable.just(listOf(inputStory)))))
        val subscriber = TestSubscriber<StoryViewModel.State>()

        // WHEN
        vm.update("http://story1.com", Flowable.empty())
                .subscribe(subscriber)

        // THEN
        val story = StoryViewModel.Story(inputStory.url, inputStory.title, inputStory.source.name,
                inputStory.source.url.toLogoUrl(), inputStory.author, time, inputStory.thumbnail,
                inputStory.content, 1)
        assertThat(subscriber.values().toList())
                .contains(StoryViewModel.State(story = story))
    }

    @Test
    fun `update() should emit states with progress = true and progress = false on sync result`() {
        // GIVEN
        whenever(model.observe(any()))
                .thenReturn(Flowable.just(StoryResult.Syncing("http://story1.com"),
                        StoryResult.Synced("http://story1.com")))
        val subscriber = TestSubscriber<StoryViewModel.State>()

        // WHEN
        vm.update(("http://story1.com"), Flowable.empty())
                .subscribe(subscriber)

        // THEN
        assertThat(subscriber.values().toList())
                .containsSubsequence(StoryViewModel.State(progress = true),
                        StoryViewModel.State())
    }

    @Test
    fun `update() should emit states with error on error result`() {
        // GIVEN
        val error = NewsApiError()
        whenever(model.observe(any()))
                .thenReturn(Flowable.just(StoryResult.Error(error)))
        val subscriber = TestSubscriber<StoryViewModel.State>()

        // WHEN
        vm.update("http://story1.com", Flowable.empty())
                .subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .contains(StoryViewModel.State(error = error))
    }

    @Test
    fun `update() should trigger a Get action`() {
        // GIVEN
        val subscriber = TestSubscriber<StoryAction>()
        whenever(model.observe(any())).then { invocation ->
            @Suppress("UNCHECKED_CAST")
            val events = (invocation.arguments[0] as Flowable<StoryAction>)
            events.subscribe(subscriber)
            return@then Flowable.empty<StoryAction>()
        }

        // WHEN
        vm.update("http://story1.com", Flowable.empty())

        // THEN
        assertThat(subscriber.values())
                .contains(StoryAction.Get("http://story1.com"))
    }

    @Test
    fun `update(Refresh) should trigger a Refresh action`() {
        // GIVEN
        val subscriber = TestSubscriber<StoryAction>()
        whenever(model.observe(any())).then { invocation ->
            @Suppress("UNCHECKED_CAST")
            val events = (invocation.arguments[0] as Flowable<StoryAction>)
            events.subscribe(subscriber)
            return@then Flowable.empty<StoryAction>()
        }

        // WHEN
        vm.update("http://story1.com", Flowable.just(StoryViewModel.Event.Refresh))

        // THEN
        assertThat(subscriber.values().toList())
                .contains(StoryAction.Sync("http://story1.com"))
    }
}