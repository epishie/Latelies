package com.epishie.news.model.network

import io.reactivex.Flowable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface PostLightApi {
    @GET("/parser")
    fun parseArticle(@Query("url") url: String, @Header("api-key") apiKey: String): Flowable<Result>

    data class Result(val url: String, val content: String?)
}