package com.example.m2tmdbluque.data.db

import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface SocialBarDao {
    @Query("SELECT * FROM social_bar_table")
    suspend fun getAll(): List<SocialBarEntity>

    @Query("SELECT person_id, is_favorite FROM social_bar_table")
    suspend fun getAllFavorites(): Map<
        @MapColumn(columnName = "person_id") Int,
        @MapColumn(columnName = "is_favorite") Boolean,
    >

    @Query("SELECT person_id, nb_likes FROM social_bar_table")
    suspend fun getAllLikes(): Map<
        @MapColumn(columnName = "person_id") Int,
        @MapColumn(columnName = "nb_likes") Int,
    >

    @Upsert
    suspend fun upsert(entity: SocialBarEntity)
}
