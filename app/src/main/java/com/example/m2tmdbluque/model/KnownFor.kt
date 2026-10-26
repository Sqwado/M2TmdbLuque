package com.example.m2tmdbluque.model

import android.content.Context
import com.example.m2tmdbluque.R
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class KnownFor(
    val id: Int,
    @SerializedName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    @SerializedName("original_title") val originalTitle: String? = null,
    @SerializedName("original_name") val originalName: String? = null,
    val overview: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("vote_average") val voteAverage: Double? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
) : Serializable {
    fun rawTitle(): String? = title ?: name ?: originalTitle ?: originalName

    fun displayTitle(context: Context): String =
        rawTitle() ?: context.getString(R.string.untitled)

    fun displayDate(context: Context): String =
        releaseDate ?: firstAirDate ?: context.getString(R.string.value_unavailable)
}
