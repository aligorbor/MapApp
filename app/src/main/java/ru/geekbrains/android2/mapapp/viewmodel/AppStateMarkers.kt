package ru.geekbrains.android2.mapapp.viewmodel

import ru.geekbrains.android2.mapapp.model.MarkerObj

sealed class AppStateMarkers {
    data class Success(val markers: List<MarkerObj>) : AppStateMarkers()
    data class Error(val error: Throwable) : AppStateMarkers()
    object Loading : AppStateMarkers()
}
