@file:Suppress("IllegalIdentifier", "MemberVisibilityCanPrivate")

package com.epishie.news.features.stories

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

class StoriesViewModelTest {
    lateinit var model: StoryModel
    lateinit var vm: StoriesViewModel

    @Before
    fun setUp() {
        model = mock()
        vm = StoriesViewModel(model)
    }

    @Test
    fun `update() should emit an initial state`() {
        // GIVEN
        whenever(model.observe(any()))
                .thenReturn(Flowable.empty())
        val subscriber = TestSubscriber<StoriesViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .containsExactly(StoriesViewModel.State())
    }

    @Test
    fun `subsequent update() should emit last state`() {
        // GIVEN
        whenever(model.observe(any()))
                .thenReturn(Flowable.just(StoryResult.Syncing()),
                        Flowable.empty())
        val subscriber1 = TestSubscriber<StoriesViewModel.State>()
        val subscriber2 = TestSubscriber<StoriesViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber1)
        subscriber1.dispose()
        vm.update(Flowable.empty())
                .subscribe(subscriber2)

        // THEN
        assertThat(subscriber2.values())
                .containsExactly(StoriesViewModel.State(progress = true))
    }

    @Test
    fun `update() should emit a state with sources`() {
        // GIVEN
        val source = Db.Source("source1", "Source 1", "http://source1.com", false)
        val inputStory = Db.Story("http://story1.com", "Story 1", "Story One",
                "Author 1", "http://image1.com", Date().time, source, false,
                null)
        whenever(model.observe(any()))
                .thenReturn(Flowable.just(StoryResult.Update(Flowable.just(listOf(inputStory)))))
        val subscriber = TestSubscriber<StoriesViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber)

        // THEN
        val story = StoriesViewModel.Story(inputStory.url, inputStory.title, source.name,
                source.url.toLogoUrl(), inputStory.author, inputStory.description, inputStory.thumbnail)
        assertThat(subscriber.values().toList())
                .contains(StoriesViewModel.State(stories = listOf(story)))
    }

    @Test
    fun `update() should emit states with progress = true and progress = false on sync result`() {
        // GIVEN
        whenever(model.observe(any()))
                .thenReturn(Flowable.just(StoryResult.Syncing(), StoryResult.Synced()))
        val subscriber = TestSubscriber<StoriesViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .containsSubsequence(StoriesViewModel.State(progress = true),
                        StoriesViewModel.State())
    }

    @Test
    fun `update() should emit states with error on error result`() {
        // GIVEN
        val error = NewsApiError()
        whenever(model.observe(any()))
                .thenReturn(Flowable.just(StoryResult.Error(error)))
        val subscriber = TestSubscriber<StoriesViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .contains(StoriesViewModel.State(error = error))
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
        vm.update(Flowable.empty())

        // THEN
        assertThat(subscriber.values())
                .contains(StoryAction.Get())
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
        vm.update(Flowable.just(StoriesViewModel.Event.Refresh))

        // THEN
        assertThat(subscriber.values())
                .contains(StoryAction.Sync())
    }
}