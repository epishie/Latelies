package com.epishie.news.model.network

import com.epishie.news.BuildConfig
import io.reactivex.Flowable
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.*

interface NewsApi {
    @GET("/v1/sources")
    fun getSources(): Flowable<SourceResult>

    @GET("v1/articles")
    fun getArticles(@Query("source") source: String,
                    @Query("apiKey") apiKey: String = BuildConfig.NEWS_API_KEY): Flowable<ArticleResult>

    data class SourceResult(
            val status: String,
            val sources: List<Source>?)
    data class Source(
            val id: String,
            val name: String,
            val description: String,
            val url: String,
            val category: String)
    data class ArticleResult(
            val status: String,
            val source: String,
            val articles: List<Article>?)
    data class Article(
            val url: String,
            var title: String,
            var description: String?,
            val author: String?,
            val urlToImage: String?,
            val publishedAt: Date?)
}