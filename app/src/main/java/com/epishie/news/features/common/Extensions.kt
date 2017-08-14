package com.epishie.news.features.common

import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single

fun ViewGroup.inflate(@LayoutRes layout: Int, attachToRoot: Boolean): View {
    val inflater = LayoutInflater.from(context)
    return inflater.inflate(layout, this, attachToRoot)
}

fun <T> Observable<T>.schedule(ui: Scheduler, worker: Scheduler): Observable<T> =
        subscribeOn(worker).observeOn(ui)

fun <T> Flowable<T>.schedule(ui: Scheduler, worker: Scheduler): Flowable<T> =
        subscribeOn(worker).observeOn(ui)

fun <T> Single<T>.schedule(ui: Scheduler, worker: Scheduler): Single<T> =
        subscribeOn(worker).observeOn(ui)