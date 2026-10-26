package com.example.m2tmdbluque.api

import com.example.m2tmdbluque.model.PopularPeopleResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApi {
    @GET("person/popular")
    fun getPopularPeople(
        @Query("page") page: Int = 1,
    ): Call<PopularPeopleResponse>

    @GET("search/person")
    fun searchPeople(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
    ): Call<PopularPeopleResponse>
}
