package com.example.m2tmdbluque.model

import com.google.gson.annotations.SerializedName

data class PopularPeopleResponse(
    val page: Int,
    val results: List<Person>,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_results") val totalResults: Int,
)
