package com.epishie.news.features.stories

import android.arch.lifecycle.ViewModel
import com.epishie.news.features.common.toLogoUrl
import com.epishie.news.model.StoriesAction
import com.epishie.news.model.StoryModel
import com.epishie.news.model.StoriesResult
import io.reactivex.Flowable
import javax.inject.Inject

class StoriesViewModel
@Inject constructor(private val storyModel: StoryModel)
    : ViewModel() {

    private var lastState = State()

    fun update(events: Flowable<Event>): Flowable<State> {
        val actions = events.map {
            StoriesAction.Sync as StoriesAction
        }
        return storyModel.observe(actions)
                .scan(lastState, this::reduce)
                .doOnNext { state -> lastState = state }
    }

    private fun reduce(state: State, result: StoriesResult): State {
        val lastState = state.copy(error = null)
        return when (result) {
            is StoriesResult.Syncing -> lastState.copy(progress = true)
            is StoriesResult.Synced -> lastState.copy(progress = false)
            is StoriesResult.Update -> lastState.copy(stories = result.sources.map {
                (url, title, description, author, thumbnail, _, source) ->
                Story(url, title, source.name, source.url.toLogoUrl(), author, description,
                        thumbnail)
            })
            is StoriesResult.Error -> lastState.copy(progress = false, error = result.throwable)
        }
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

