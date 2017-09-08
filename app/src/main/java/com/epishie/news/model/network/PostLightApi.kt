package com.epishie.news.model.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface PostLightApi {
    @GET("/parser")
    fun parseUrl(@Query("url") url: String, @Header("api-key") apiKey: String)
}