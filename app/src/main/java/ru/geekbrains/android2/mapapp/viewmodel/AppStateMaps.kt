package ru.geekbrains.android2.mapapp.viewmodel

import com.google.android.gms.maps.model.LatLng

sealed class AppStateMaps {
    object SuccessMarker : AppStateMaps()
    data class SuccessDirection(val polyPoints: List<LatLng>) : AppStateMaps()
    data class Error(val error: Throwable) : AppStateMaps()
    object Loading : AppStateMaps()
}