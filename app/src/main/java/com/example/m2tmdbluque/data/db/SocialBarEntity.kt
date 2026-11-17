package com.example.m2tmdbluque.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "social_bar_table")
data class SocialBarEntity(
    @PrimaryKey
    @ColumnInfo(name = "person_id")
    val personId: Int,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "nb_likes")
    val nbLikes: Int = 0,
)
