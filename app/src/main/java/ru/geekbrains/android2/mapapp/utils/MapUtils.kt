package ru.geekbrains.android2.mapapp.utils

import android.content.Context
import android.util.DisplayMetrics
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import org.w3c.dom.Document
import org.w3c.dom.NodeList

/**
 * Method to decode polyline points
 * Courtesy : https://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
 */
fun decodePoly(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].toInt() - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].toInt() - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        val p = LatLng(
            lat.toDouble() / 1E5,
            lng.toDouble() / 1E5
        )
        poly.add(p)
    }

    return poly
}

fun getDirection(doc: Document): ArrayList<LatLng> {
    val nl1: NodeList
    var nl2: NodeList
    var nl3: NodeList
    val listGeopoints = ArrayList<LatLng>()
    nl1 = doc.getElementsByTagName("step")
    if (nl1.length > 0) {
        for (i in 0 until nl1.length) {
            val node1 = nl1.item(i)
            nl2 = node1.childNodes
            var locationNode = nl2.item(getNodeIndex(nl2, "start_location"))
            nl3 = locationNode.childNodes
            var latNode = nl3.item(getNodeIndex(nl3, "lat"))
            var lat = latNode.textContent.toDouble()
            var lngNode = nl3.item(getNodeIndex(nl3, "lng"))
            var lng = lngNode.textContent.toDouble()
            listGeopoints.add(LatLng(lat, lng))
            locationNode = nl2.item(getNodeIndex(nl2, "polyline"))
            nl3 = locationNode.childNodes
            latNode = nl3.item(getNodeIndex(nl3, "points"))
            val arr = decodePoly1(latNode.textContent)
            for (j in arr.indices) {
                listGeopoints.add(
                    LatLng(
                        arr[j].latitude, arr[j].longitude
                    )
                )
            }
            locationNode = nl2.item(getNodeIndex(nl2, "end_location"))
            nl3 = locationNode.childNodes
            latNode = nl3.item(getNodeIndex(nl3, "lat"))
            lat = latNode.textContent.toDouble()
            lngNode = nl3.item(getNodeIndex(nl3, "lng"))
            lng = lngNode.textContent.toDouble()
            listGeopoints.add(LatLng(lat, lng))
        }
    }
    return listGeopoints
}

fun getSection(doc: Document): ArrayList<LatLng> {
    val nl1: NodeList
    var nl2: NodeList
    var nl3: NodeList
    val listGeopoints = ArrayList<LatLng>()
    nl1 = doc.getElementsByTagName("step")
    if (nl1.length > 0) {
        for (i in 0 until nl1.length) {
            val node1 = nl1.item(i)
            nl2 = node1.childNodes
            val locationNode = nl2.item(getNodeIndex(nl2, "end_location"))
            nl3 = locationNode.childNodes
            val latNode = nl3.item(getNodeIndex(nl3, "lat"))
            val lat = latNode.textContent.toDouble()
            val lngNode = nl3.item(getNodeIndex(nl3, "lng"))
            val lng = lngNode.textContent.toDouble()
            listGeopoints.add(LatLng(lat, lng))
        }
    }
    return listGeopoints
}

fun getPolyline(doc: Document, width: Int, color: Int, context: Context): PolylineOptions {
    val arr_pos = getDirection(doc)
    val rectLine = PolylineOptions().width(dpToPx(width, context).toFloat()).color(color)
    for (i in arr_pos.indices) rectLine.add(arr_pos[i])
    return rectLine
}

private fun getNodeIndex(nl: NodeList, nodename: String): Int {
    for (i in 0 until nl.length) {
        if (nl.item(i).nodeName == nodename) return i
    }
    return -1
}

private fun decodePoly1(encoded: String): ArrayList<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0
    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].toInt() - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat
        shift = 0
        result = 0
        do {
            b = encoded[index++].toInt() - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng
        val position = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
        poly.add(position)
    }
    return poly
}


fun dpToPx(dp: Int, mContext: Context): Int {
    val displayMetrics = mContext!!.resources.displayMetrics
    return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
}