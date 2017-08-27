package com.epishie.news.features.common

import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import okhttp3.HttpUrl

fun ViewGroup.inflate(@LayoutRes layout: Int, attachToRoot: Boolean): View {
    val inflater = LayoutInflater.from(context)
    return inflater.inflate(layout, this, attachToRoot)
}

fun ViewGroup?.inflate(inflater: LayoutInflater, @LayoutRes layout: Int, attachToRoot: Boolean): View {
    return inflater.inflate(layout, null, attachToRoot)
}

fun <T> Observable<T>.schedule(ui: Scheduler, worker: Scheduler): Observable<T> =
        subscribeOn(worker).observeOn(ui)

fun <T> Flowable<T>.schedule(ui: Scheduler, worker: Scheduler): Flowable<T> =
        subscribeOn(worker).observeOn(ui)

fun <T> Single<T>.schedule(ui: Scheduler, worker: Scheduler): Single<T> =
        subscribeOn(worker).observeOn(ui)

fun String.toLogoUrl(): String {
    val url = HttpUrl.parse(this)
    return if (url == null) {
        ""
    } else {
        "https://logo.clearbit.com/${url.host()}"
    }
}
