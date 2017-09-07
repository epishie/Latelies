package com.epishie.news.features.sources

import android.arch.lifecycle.ViewModel
import com.epishie.news.features.common.toLogoUrl
import com.epishie.news.model.SourceAction
import com.epishie.news.model.SourceModel
import com.epishie.news.model.SourceResult
import com.epishie.news.model.db.Db
import io.reactivex.Flowable
import javax.inject.Inject

class SourcesViewModel
@Inject constructor(private val sourceModel: SourceModel)
    : ViewModel() {

    private var lastState = State()

    fun update(events: Flowable<Event>): Flowable<State> {
        val actions = events.publish { shared ->
            Flowable.merge(
                    shared.ofType(Event.Refresh::class.java).map {
                        SourceAction.Sync
                    },
                    shared.ofType(Event.Select::class.java).map { (source) ->
                        SourceAction.Select(Db.SourceSelection(source.id, source.selected))
                    }
            )
        }
        return sourceModel.observe(actions)
                .scan(lastState, this::reduce)
                .doOnNext { state -> lastState = state }
    }

    private fun reduce(state: State, result: SourceResult): State {
        val lastState = state.copy(error = null)
        return when (result) {
            is SourceResult.Update -> lastState.copy(sources = result.sources.map(this::mapDbToVm))
            is SourceResult.Syncing -> lastState.copy(progress = true)
            is SourceResult.Synced -> lastState.copy(progress = false)
            is SourceResult.Error -> lastState.copy(progress = false, error = result.throwable)
        }
    }

    private fun mapDbToVm(source: Db.Source): Source {
        return Source(source.id, source.name, source.url.toLogoUrl(), source.selected)
    }

    data class State(val progress: Boolean = false, val error: Throwable? = null,
                     val sources: List<Source>? = null)
    data class Source(val id: String, val name: String, val logo: String, val selected: Boolean)
    sealed class Event {
        object Refresh : Event()
        data class Select(val source: Source) : Event()
    }
}