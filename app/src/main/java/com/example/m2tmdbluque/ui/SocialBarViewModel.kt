package com.example.m2tmdbluque.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class SocialBarUiState(
    val isFavorite: Boolean = false,
    val likeCount: Int = 0,
)

/**
 * ViewModel partagé au niveau de l'Activity (activityViewModels).
 * Conserve favoris / likes par personId malgré le recyclage des fragments
 * dans la RecyclerView.
 */
class SocialBarViewModel : ViewModel() {

    private val states = mutableMapOf<Int, MutableLiveData<SocialBarUiState>>()

    fun uiState(personId: Int): LiveData<SocialBarUiState> =
        states.getOrPut(personId) { MutableLiveData(SocialBarUiState()) }

    fun toggleFavorite(personId: Int) {
        val live = states.getOrPut(personId) { MutableLiveData(SocialBarUiState()) }
        val current = live.value ?: SocialBarUiState()
        live.value = current.copy(isFavorite = !current.isFavorite)
    }

    fun incrementLikes(personId: Int) {
        val live = states.getOrPut(personId) { MutableLiveData(SocialBarUiState()) }
        val current = live.value ?: SocialBarUiState()
        live.value = current.copy(likeCount = current.likeCount + 1)
    }
}
