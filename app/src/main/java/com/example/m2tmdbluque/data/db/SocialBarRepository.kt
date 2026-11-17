package com.example.m2tmdbluque.data.db

class SocialBarRepository(private val dao: SocialBarDao) {
    suspend fun getAllFavorites(): Map<Int, Boolean> = dao.getAllFavorites()

    suspend fun getAllLikes(): Map<Int, Int> = dao.getAllLikes()

    suspend fun upsert(personId: Int, isFavorite: Boolean, nbLikes: Int) {
        dao.upsert(
            SocialBarEntity(
                personId = personId,
                isFavorite = isFavorite,
                nbLikes = nbLikes,
            ),
        )
    }
}
