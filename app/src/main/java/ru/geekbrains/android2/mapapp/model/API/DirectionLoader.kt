package ru.geekbrains.android2.mapapp.model.API

import com.google.android.gms.maps.model.LatLng

object DirectionLoader {
    fun getURL(from: LatLng, to: LatLng, key: String): String {
        val origin = "origin=" + from.latitude + "," + from.longitude
        val dest = "destination=" + to.latitude + "," + to.longitude
        val sensor = "sensor=false"
        val k = "key=$key"
        val params = "$origin&$dest&$sensor&$k"
        return "https://maps.googleapis.com/maps/api/directions/json?$params"
    }

}