package ru.geekbrains.android2.mapapp.model.DB

interface MarkerDB {
    suspend fun insMarker(marker: MarkerObj): String?
    suspend fun updMarker(key: String, marker: MarkerObj): Boolean
    suspend fun delMarker(key: String): Boolean
    suspend fun getMarker(key: String): MarkerObj?
    suspend fun findMarker(title: String): MarkerObj?
    suspend fun saveMarker(marker: MarkerObj): String?
    suspend fun listMarker(): List<MarkerObj>
}