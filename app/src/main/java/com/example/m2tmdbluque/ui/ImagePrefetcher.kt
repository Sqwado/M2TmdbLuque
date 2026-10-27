package com.example.m2tmdbluque.ui

import com.example.m2tmdbluque.api.TmdbClient
import com.example.m2tmdbluque.model.Person
import com.squareup.picasso.Picasso

/**
 * Précharge les portraits dans le cache Picasso (mémoire + disque)
 * pour éviter le flash au scroll.
 */
object ImagePrefetcher {

    /** Aligné sur w185 TMDB + ratio poster 2:3. */
    const val PROFILE_WIDTH = 185
    const val PROFILE_HEIGHT = 278

    fun prefetchProfiles(people: List<Person>) {
        people.forEach { person ->
            val url = TmdbClient.profileImageUrl(person.profilePath) ?: return@forEach
            Picasso.get()
                .load(url)
                .resize(PROFILE_WIDTH, PROFILE_HEIGHT)
                .centerCrop()
                .fetch()
        }
    }
}
