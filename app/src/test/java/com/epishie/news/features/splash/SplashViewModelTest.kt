@file:Suppress("IllegalIdentifier")

package com.epishie.news.features.splash

import android.annotation.SuppressLint
import com.epishie.news.model.SettingsModel
import com.epishie.news.model.SourceAction
import com.epishie.news.model.SourceModel
import com.epishie.news.model.SourceResult
import com.f2prateek.rx.preferences2.Preference
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.subscribers.TestSubscriber
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.IOException

@Suppress("MemberVisibilityCanPrivate")
@SuppressLint("CheckResult")
class SplashViewModelTest {
    lateinit var settingsModel: SettingsModel
    lateinit var sourceModel: SourceModel
    lateinit var dbInitialized: Preference<Boolean>
    lateinit var dbInitializedConsumer: Consumer<Boolean>
    lateinit var vm: SplashViewModel

    @Before
    fun setUp() {
        settingsModel = mock()
        dbInitializedConsumer = mock()
        dbInitialized = mock {
            on { asConsumer() } doReturn dbInitializedConsumer
        }
        whenever(settingsModel.dbInitialized).doReturn(dbInitialized)
        sourceModel = mock {
            on { observe(any()) } doReturn Flowable.empty()
        }
        vm = SplashViewModel(settingsModel, sourceModel)
    }

    @Test
    fun `update() should emit an initial state`() {
        whenever(dbInitialized.asObservable()).thenReturn(Observable.empty())
        val subscriber = TestSubscriber<SplashViewModel.State>()

        // WHEN
        vm.update(Flowable.empty()).subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SplashViewModel.State())
    }

    @Test
    fun `update() emit a state with progress = false and success = true when DB is initialized`() {
        // GIVEN
        whenever(dbInitialized.asObservable()).thenReturn(Observable.just(true))
        val subscriber = TestSubscriber<SplashViewModel.State>()

        // WHEN
        vm.update(Flowable.empty()).subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SplashViewModel.State(),
                        SplashViewModel.State(success = true))
    }

    @Test
    fun `update() should emit states with progress = true when Syncing`() {
        // GIVEN
        whenever(sourceModel.observe(any()))
                .thenReturn(Flowable.just(SourceResult.Syncing))
        whenever(dbInitialized.asObservable()).thenReturn(Observable.just(false))
        val subscriber = TestSubscriber<SplashViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SplashViewModel.State(),
                        SplashViewModel.State(progress = true))
    }

    @Test
    fun `update() should emit states with success = true when Synced`() {
        // GIVEN
        whenever(sourceModel.observe(any()))
                .thenReturn(Flowable.just(SourceResult.Synced))
        whenever(dbInitialized.asObservable()).thenReturn(Observable.just(false, true))
        val subscriber = TestSubscriber<SplashViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SplashViewModel.State(),
                        SplashViewModel.State(success = true))
        verify(dbInitializedConsumer).accept(true)
    }

    @Test
    fun `update() should emit states with error on Error`() {
        // GIVEN
        val error = IOException()
        whenever(sourceModel.observe(any()))
                .thenReturn(Flowable.just(SourceResult.Error(error)))
        whenever(dbInitialized.asObservable()).thenReturn(Observable.just(false))
        val subscriber = TestSubscriber<SplashViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SplashViewModel.State(),
                        SplashViewModel.State(error = error))
    }

    @Test
    fun `update() should trigger a Sync action when DB is unitialized`() {
        // GIVEN
        val subscriber = TestSubscriber<SourceAction>()
        whenever(dbInitialized.asObservable()).thenReturn(Observable.just(false))
        whenever(sourceModel.observe(any())).then { invocation ->
            @Suppress("UNCHECKED_CAST")
            val events = (invocation.arguments[0] as Flowable<SourceAction>)
            events.subscribe(subscriber)
            return@then Flowable.empty<SourceAction>()
        }

        // WHEN
        vm.update(Flowable.empty())

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SourceAction.Sync)
    }

    @Test
    fun `update(Retry) should trigger a Sync action`() {
        // GIVEN
        val subscriber = TestSubscriber<SourceAction>()
        whenever(dbInitialized.asObservable()).thenReturn(Observable.empty())
        whenever(sourceModel.observe(any())).then { invocation ->
            @Suppress("UNCHECKED_CAST")
            val events = (invocation.arguments[0] as Flowable<SourceAction>)
            events.subscribe(subscriber)
            return@then Flowable.empty<SourceAction>()
        }

        // WHEN
        vm.update(Flowable.just(SplashViewModel.Event.RetryEvent))

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SourceAction.Sync)
    }
}