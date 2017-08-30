@file:Suppress("IllegalIdentifier")

package com.epishie.news.features.splash

import android.annotation.SuppressLint
import com.epishie.news.model.SettingsModel
import com.epishie.news.model.SourceModel
import com.f2prateek.rx.preferences2.Preference
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.subscribers.TestSubscriber
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.IOException

@SuppressLint("CheckResult")
class SplashViewModelTest {
    lateinit var settingsModel: SettingsModel
    lateinit var sourceModel: SourceModel
    lateinit var vm: SplashViewModel

    @Before
    fun setUp() {
        settingsModel = mock()
        sourceModel = mock {
            on { observe(any()) } doReturn Flowable.empty()
        }
        vm = SplashViewModel(settingsModel, sourceModel)
    }

    @Test
    fun `initialized DB should emit a state with progress = false and success = true`() {
        // GIVEN
        val preference: Preference<Boolean> = mock {
            on { asObservable() } doReturn Observable.just(true)
        }
        whenever(settingsModel.dbInitialized).thenReturn(preference)
        val subscriber = TestSubscriber<SplashViewModel.State>()

        // WHEN
        vm.update(Flowable.empty()).subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SplashViewModel.State(),
                        SplashViewModel.State(success = true))
    }

    @Test
    fun `Syncing should emit states with progress = true and success = false`() {
        // GIVEN
        whenever(sourceModel.observe(any()))
                .thenReturn(Flowable.just(SourceModel.Result.Syncing))
        val preference: Preference<Boolean> = mock {
            on { asObservable() } doReturn Observable.just(false)
        }
        whenever(settingsModel.dbInitialized).thenReturn(preference)
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
    fun `Synced should emit states with success = true`() {
        // GIVEN
        whenever(sourceModel.observe(any()))
                .thenReturn(Flowable.just(SourceModel.Result.Synced))
        val preference: Preference<Boolean> = mock {
            on { asObservable() } doReturn Observable.just(false)
        }
        whenever(settingsModel.dbInitialized).thenReturn(preference)
        val subscriber = TestSubscriber<SplashViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SplashViewModel.State(),
                        SplashViewModel.State(success = true))
    }

    @Test
    fun `Error should emit states with progress = true and success = false`() {
        // GIVEN
        whenever(sourceModel.observe(any()))
                .thenReturn(Flowable.just(SourceModel.Result.Error(IOException())))
        val preference: Preference<Boolean> = mock {
            on { asObservable() } doReturn Observable.just(false)
        }
        whenever(settingsModel.dbInitialized).thenReturn(preference)
        val subscriber = TestSubscriber<SplashViewModel.State>()

        // WHEN
        vm.update(Flowable.empty())
                .subscribe(subscriber)

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SplashViewModel.State(),
                        SplashViewModel.State(error = "Error"))
    }

    @Test
    fun `uninitialized DB should trigger a Sync action`() {
        // GIVEN
        val subscriber = TestSubscriber<SourceModel.Action>()
        val preference: Preference<Boolean> = mock {
            on { asObservable() } doReturn Observable.just(false)
        }
        whenever(settingsModel.dbInitialized).thenReturn(preference)
        whenever(sourceModel.observe(any())).then { invocation ->
            @Suppress("UNCHECKED_CAST")
            val events = (invocation.arguments[0] as Flowable<SourceModel.Action>)
            events.subscribe(subscriber)
            return@then Flowable.empty<SourceModel.Action>()
        }

        // WHEN
        vm.update(Flowable.empty())

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SourceModel.Action.Sync)
    }

    @Test
    fun `retry should trigger a Sync action`() {
        // GIVEN
        val subscriber = TestSubscriber<SourceModel.Action>()
        val preference: Preference<Boolean> = mock {
            on { asObservable() } doReturn Observable.empty<Boolean>()
        }
        whenever(settingsModel.dbInitialized).thenReturn(preference)
        whenever(sourceModel.observe(any())).then { invocation ->
            @Suppress("UNCHECKED_CAST")
            val events = (invocation.arguments[0] as Flowable<SourceModel.Action>)
            events.subscribe(subscriber)
            return@then Flowable.empty<SourceModel.Action>()
        }

        // WHEN
        vm.update(Flowable.just(SplashViewModel.Event.RetryEvent))

        // THEN
        assertThat(subscriber.values())
                .containsExactly(SourceModel.Action.Sync)
    }
}