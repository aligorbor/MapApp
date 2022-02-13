package ru.geekbrains.android2.mapapp.viewmodel

sealed class AppStateMaps {
    object Success : AppStateMaps()
    data class Error(val error: Throwable) : AppStateMaps()
    object Loading : AppStateMaps()
}