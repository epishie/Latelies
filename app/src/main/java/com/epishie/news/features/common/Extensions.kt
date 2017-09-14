package com.epishie.news.features.common

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import com.squareup.picasso.Target
import okhttp3.HttpUrl

fun ViewGroup.inflate(@LayoutRes layout: Int, attachToRoot: Boolean): View {
    val inflater = LayoutInflater.from(context)
    return inflater.inflate(layout, this, attachToRoot)
}

fun ViewGroup?.inflate(inflater: LayoutInflater, @LayoutRes layout: Int, attachToRoot: Boolean): View {
    return inflater.inflate(layout, null, attachToRoot)
}

fun String.toLogoUrl(): String {
    val url = HttpUrl.parse(this)
    return if (url == null) {
        ""
    } else {
        "https://logo.clearbit.com/${url.host()}"
    }
}

class __PicassoCallback : Callback {
    private var _onSuccess: (() -> Unit)? = null
    private var _onFailed: (() -> Unit)? = null

    override fun onSuccess() {
        _onSuccess?.invoke()
    }

    override fun onError() {
        _onFailed?.invoke()
    }

    fun onSuccess(func: () -> Unit) {
        _onSuccess = func
    }

    fun onFailed(func: () -> Unit) {
        _onFailed = func
    }
}

fun RequestCreator.into(imageView: ImageView, func: __PicassoCallback.() -> Unit) {
    val callback = __PicassoCallback()
    callback.func()
    into(imageView, callback)
}

class __PicassoTarget : Target {
    private var _onPrepareLoad: ((Drawable?) -> Unit)? = null
    private var _onBitmapFailed: ((Drawable?) -> Unit)? = null
    private var _onBitmapLoaded: ((Bitmap?, Picasso.LoadedFrom?) -> Unit)? = null

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        _onPrepareLoad?.invoke(placeHolderDrawable)
    }

    override fun onBitmapFailed(errorDrawable: Drawable?) {
        _onBitmapFailed?.invoke(errorDrawable)
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        _onBitmapLoaded?.invoke(bitmap, from)
    }

    fun onPrepareLoaded(func: (Drawable?) -> Unit) {
        _onPrepareLoad = func
    }

    fun onBitmapFailed(func: (Drawable?) -> Unit) {
        _onBitmapFailed = func
    }

    fun onBitmapLoaded(func: (Bitmap?, Picasso.LoadedFrom?) -> Unit) {
        _onBitmapLoaded = func
    }
}

fun RequestCreator.into(func: __PicassoTarget.() -> Unit) {
    val target = __PicassoTarget()
    target.func()
    into(target)
}
