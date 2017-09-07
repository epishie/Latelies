package com.epishie.news.features.splash

import android.arch.lifecycle.ViewModel
import com.epishie.news.model.SettingsModel
import com.epishie.news.model.SourceAction
import com.epishie.news.model.SourceModel
import com.epishie.news.model.SourceResult
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import javax.inject.Inject

class SplashViewModel
@Inject constructor(private val settingsModel: SettingsModel,
                    private val sourceModel: SourceModel)
    : ViewModel() {
    private val lastState = State(false, false, null)
    private val dbInitialized: Flowable<Boolean>
        get() = settingsModel.dbInitialized.asObservable().toFlowable(BackpressureStrategy.BUFFER)

    fun update(events: Flowable<Event>): Flowable<State> {
        val actions = Flowable.merge(
                dbInitialized.flatMap { result ->
                    when (result)  {
                        true -> Flowable.empty<SourceAction>()
                        else -> Flowable.just(SourceAction.Sync)
                    }
                },
                events.flatMap { event ->
                    when (event) {
                        is SplashViewModel.Event.RetryEvent -> Flowable.just(SourceAction.Sync)
                    }
                }
        )
        val dbUpdates = dbInitialized.filter { result -> result }
        val sourceResults = sourceModel.observe(actions)
                .publish()
                .autoConnect(2)
        sourceResults.filter{ result -> result == SourceResult.Synced }
                .map { true }
                .subscribe(settingsModel.dbInitialized.asConsumer())

        return Flowable.merge(sourceResults, dbUpdates).scan(lastState, this::reduce)
                .distinctUntilChanged()
    }

    private fun reduce(state: State, result: Any): State {
        return when (result) {
            is Boolean -> State(success = true)
            is SourceResult.Syncing -> State(progress = true)
            is SourceResult.Error -> State(error = result.throwable)
            else -> state
        }
    }

    data class State(val progress: Boolean = false, val success: Boolean = false,
                     val error: Throwable? = null)

    sealed class Event {
        object RetryEvent : Event()
    }
}