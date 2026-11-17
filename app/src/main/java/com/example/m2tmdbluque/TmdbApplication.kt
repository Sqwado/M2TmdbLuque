package com.example.m2tmdbluque

import android.app.Application
import com.example.m2tmdbluque.data.db.SocialBarRepository
import com.example.m2tmdbluque.data.db.TmdbDatabase

class TmdbApplication : Application() {
    val database by lazy { TmdbDatabase.getInstance(this) }
    val socialBarRepository by lazy { SocialBarRepository(database.socialBarDao()) }
}
