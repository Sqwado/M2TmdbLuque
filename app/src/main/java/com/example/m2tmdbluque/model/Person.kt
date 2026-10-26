package com.example.m2tmdbluque.model

import android.content.Context
import com.example.m2tmdbluque.R
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Person(
    val adult: Boolean = false,
    val gender: Int = 0,
    val id: Int,
    @SerializedName("known_for_department") val knownForDepartment: String? = null,
    val name: String,
    @SerializedName("original_name") val originalName: String? = null,
    val popularity: Double = 0.0,
    @SerializedName("profile_path") val profilePath: String? = null,
    @SerializedName("known_for") val knownFor: List<KnownFor> = emptyList(),
) : Serializable {
    val knownForSummary: String
        get() = knownFor.take(3).joinToString { it.displayTitle() }.ifBlank { "—" }

    fun genderLabel(context: Context): String = context.getString(
        when (gender) {
            1 -> R.string.gender_female
            2 -> R.string.gender_male
            3 -> R.string.gender_non_binary
            else -> R.string.gender_unspecified
        },
    )
}
