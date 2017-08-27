package com.epishie.news.model.network

import io.reactivex.Flowable
import retrofit2.http.GET

interface NewsApi {
    @GET("/v1/sources")
    fun getSources(): Flowable<SourceResult>

    data class SourceResult(val status: String, val sources: List<Source>?)
    data class Source(val id: String, val name: String, val description: String, val url: String,
                      val category: String)
}