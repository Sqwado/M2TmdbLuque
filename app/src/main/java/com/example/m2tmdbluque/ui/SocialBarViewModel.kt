package com.example.m2tmdbluque.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.m2tmdbluque.TmdbApplication
import com.example.m2tmdbluque.data.db.SocialBarRepository
import kotlinx.coroutines.launch

/**
 * ViewModel partagé (activityViewModels) — maps par personId (= mapkey du fragment).
 * Persiste likes / favoris via Room.
 */
class SocialBarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SocialBarRepository =
        (application as TmdbApplication).socialBarRepository

    var nbLikes = mutableMapOf<Int, Int>()
    var isFavorite = mutableMapOf<Int, Boolean>()

    /** Notifie les fragments recyclés qu'une map a changé. */
    private val _revision = MutableLiveData(0)
    val revision: LiveData<Int> = _revision

    init {
        viewModelScope.launch {
            nbLikes.putAll(repository.getAllLikes())
            isFavorite.putAll(repository.getAllFavorites())
            bump()
        }
    }

    fun likesOf(mapkey: Int): Int = nbLikes.getOrElse(mapkey) { 0 }

    fun isFavoriteOf(mapkey: Int): Boolean = isFavorite.getOrElse(mapkey) { false }

    fun toggleFavorite(mapkey: Int) {
        isFavorite[mapkey] = !isFavoriteOf(mapkey)
        bump()
        persist(mapkey)
    }

    fun incrementLikes(mapkey: Int) {
        nbLikes[mapkey] = likesOf(mapkey) + 1
        bump()
        persist(mapkey)
    }

    private fun persist(mapkey: Int) {
        viewModelScope.launch {
            repository.upsert(
                personId = mapkey,
                isFavorite = isFavoriteOf(mapkey),
                nbLikes = likesOf(mapkey),
            )
        }
    }

    private fun bump() {
        _revision.value = (_revision.value ?: 0) + 1
    }
}
