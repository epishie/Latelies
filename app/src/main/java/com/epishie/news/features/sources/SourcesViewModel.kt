package com.epishie.news.features.sources

import android.arch.lifecycle.ViewModel
import com.epishie.news.features.common.toLogoUrl
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
        val actions = events.publish { shared ->
            Flowable.merge(
                    shared.ofType(Event.Refresh::class.java).map {
                        Action.Refresh
                    },
                    shared.ofType(Event.Select::class.java).map { (source) ->
                        Action.Select(Db.SourceSelection(source.id, source.selected))
                    }
            )
        }
        return sourceModel.observe(actions)
                .scan(lastState, this::reduce)
    }

    private fun reduce(state: State, result: SourceModel.Result): State {
        return when (result) {
            is SourceModel.Result.Update ->
                state.copy(sources = result.sources.map(this::mapDbToVm), error = "")
            is SourceModel.Result.Syncing -> state.copy(progress = true, error = "")
            is SourceModel.Result.Synced -> state.copy(progress = false, error = "")
            is SourceModel.Result.Error ->
                state.copy(progress = false, error = result.throwable.message ?: "Error")
        }
    }

    private fun mapDbToVm(source: Db.Source): Source {
        return Source(source.id, source.name, source.url.toLogoUrl(), source.selected)
    }

    data class State(val progress: Boolean, val error: String, val sources: List<Source>)
    data class Source(val id: String, val name: String, val logo: String, val selected: Boolean)
    sealed class Event {
        object Refresh : Event()
        data class Select(val source: Source) : Event()
    }
}