package com.epishie.news.features.story

import android.arch.lifecycle.ViewModel
import com.epishie.news.features.common.toLogoUrl
import com.epishie.news.model.StoryAction
import com.epishie.news.model.StoryModel
import com.epishie.news.model.StoryResult
import com.epishie.news.model.db.Db
import io.reactivex.Flowable
import javax.inject.Inject


class StoryViewModel
@Inject constructor(private val storyModel: StoryModel) : ViewModel() {
    private var lastState = State()

    fun update(url: String, events: Flowable<Event>): Flowable<State> {
        val actions = events.map { event ->
            when (event) {
                is Event.Refresh -> StoryAction.Sync(url)
            } as StoryAction
        }.startWith(StoryAction.Get(url))
        return storyModel.observe(actions)
                .flatMap { result ->
                    if (result is StoryResult.Update) {
                        result.stories
                                .filter { stories ->
                                    stories.size == 1 && stories.first().url == url
                                }
                                .map { stories -> stories.first() }
                                .map { story ->
                                    Result.Data(story) as Result
                                }
                    } else {
                        Flowable.just(Result.Status(result))
                    }
                }
                .scan(lastState, this::reduce)
                .doOnNext { state -> lastState = state }
    }

    private fun reduce(state: State, result: Result): State {
        val lastState = state.copy(error = null)
        return when (result) {
            is Result.Data -> {
                val story = result.story
                lastState.copy(story = Story(story.url, story.title, story.source.name,
                        story.source.url.toLogoUrl(), story.author,
                        story.date, story.thumbnail, story.content, story.wordCount?.div(200))
                )
            }
            is Result.Status -> when (result.actual) {
                is StoryResult.Syncing -> lastState.copy(progress = true)
                is StoryResult.Synced -> lastState.copy(progress = false)
                is StoryResult.Error -> lastState.copy(progress = false,
                        error = result.actual.throwable)
                else -> lastState
            }
        }
    }

    private sealed class Result {
        data class Data(val story: Db.Story) : Result()
        data class Status(val actual: StoryResult) : Result()
    }

    data class State(val progress: Boolean = false, val error: Throwable? = null,
                     val story: Story? = null)
    data class Story(val url: String, val title: String, val source: String,
                     val sourceLogo: String?, val author: String?, val date: Long?,
                     val thumbnail: String?, val content: String?, val timeToRead: Int?)

    sealed class Event {
        object Refresh : Event()
    }
}