package com.epishie.news.features.stories

import android.arch.lifecycle.ViewModel
import com.epishie.news.features.common.toLogoUrl
import com.epishie.news.model.StoryAction
import com.epishie.news.model.StoryModel
import com.epishie.news.model.StoryResult
import com.epishie.news.model.db.Db
import io.reactivex.Flowable
import javax.inject.Inject

class StoriesViewModel
@Inject constructor(private val storyModel: StoryModel)
    : ViewModel() {

    private var lastState = State()

    fun update(events: Flowable<Event>): Flowable<State> {
        val actions = events.map {
            StoryAction.Sync() as StoryAction
        }.startWith(StoryAction.Get())
        return storyModel.observe(actions)
                .flatMap { result ->
                    if (result is StoryResult.Update) {
                        result.stories.map { stories ->
                            Result.Stories(stories)
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
            is Result.Stories -> lastState.copy(stories = result.actual.map {
                (url, title, description, author, thumbnail, _, source) ->
                Story(url, title, source.name, source.url.toLogoUrl(), author, description,
                        thumbnail)
            })
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
        data class Stories(val actual: List<Db.Story>) : Result()
        data class Status(val actual: StoryResult) : Result()
    }

    data class State(val progress: Boolean = false, val error: Throwable? = null,
                     val stories: List<Story> = emptyList())
    data class Story(val url: String, val title: String, val source: String,
                     val sourceLogo: String?, val author: String?, val description: String?,
                     val thumbnail: String?)

    sealed class Event {
        object Refresh : Event()
    }
}

