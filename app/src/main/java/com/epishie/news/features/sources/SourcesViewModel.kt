package com.epishie.news.features.sources

import android.arch.lifecycle.ViewModel
import com.epishie.news.model.SourceModel
import com.epishie.news.model.SourceModel.Action
import com.epishie.news.model.db.Db
import io.reactivex.Flowable
import javax.inject.Inject

class SourcesViewModel
@Inject constructor(private val sourceModel: SourceModel)
    : ViewModel() {

    private var lastState = State(false, "", emptyList())

    fun update(events: Flowable<Event>): Flowable<State> {
        val actions = events.flatMap { event ->
            when (event) {
                Event.Refresh -> Flowable.just<Action>(Action.Refresh)
            }
        }
        return sourceModel.observe(actions)
                .scan(lastState, this::reduce)
    }

    private fun reduce(state: State, result: SourceModel.Result): State {
        return when (result) {
            is SourceModel.Result.Update ->
                state.copy(sources = result.sources.map(this::mapDbToVm), error = "")
            is SourceModel.Result.Syncing ->
                state.copy(progress = true, error = "")
            is SourceModel.Result.Error -> state.copy(progress = false,
                    error = result.throwable.message ?: "Error")
            else -> state
        }
    }

    private fun mapDbToVm(source: Db.Source): Source {
        return Source(source.id, source.name, source.url, source.selected)
    }

    data class State(val progress: Boolean, val error: String, val sources: List<Source>)
    data class Source(val id: String, val name: String, val logo: String, val selected: Boolean)
    sealed class Event {
        object Refresh : Event()
    }
}