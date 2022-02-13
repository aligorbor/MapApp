package ru.geekbrains.android2.mapapp.model

import com.parse.ParseObject
import com.parse.ParseQuery
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MarkerDBImpl : MarkerDB {
    private fun putParseMarker(parseObject: ParseObject, marker: MarkerObj) {
        parseObject.put("title", marker.title)
        parseObject.put("description", marker.description)
        parseObject.put("lat", marker.lat)
        parseObject.put("long", marker.long)
    }

    private fun getParseMarker(obj: ParseObject) = MarkerObj(
        title = obj.getString("title") ?: "",
        description = obj.getString("description") ?: "",
        lat = obj.getNumber("lat")?.toDouble() ?: 0.0,
        long = obj.getNumber("long")?.toDouble() ?: 0.0,
        keyMarker = obj.objectId
    )

    override suspend fun insMarker(marker: MarkerObj): String? {
        var objectId: String? = null
        val parseObject = ParseObject(CLASS_MARKER)
        parseObject.save()
        putParseMarker(parseObject, marker)
        return suspendCoroutine { cont ->
            parseObject.saveInBackground {
                if (it == null) objectId = parseObject.objectId
                cont.resume(objectId)
            }
        }
    }

    override suspend fun updMarker(key: String, marker: MarkerObj): Boolean {
        val parseObject = ParseObject(CLASS_MARKER)
        parseObject.objectId = key
        putParseMarker(parseObject, marker)
        return suspendCoroutine { cont ->
            parseObject.saveInBackground {
                cont.resume(it == null)
            }
        }
    }

    override suspend fun delMarker(key: String): Boolean {
        val parseObject = ParseObject(CLASS_MARKER)
        parseObject.objectId = key
        return suspendCoroutine { cont ->
            parseObject.deleteInBackground {
                cont.resume(it == null)
            }
        }
    }

    override suspend fun getMarker(key: String): MarkerObj? {
        var marker: MarkerObj? = null
        val query = ParseQuery<ParseObject>(CLASS_MARKER)
        return suspendCoroutine { cont ->
            query.getInBackground(
                key
            ) { obj, e ->
                if (e == null)
                    marker = getParseMarker(obj)
                cont.resume(marker)
            }
        }
    }

    override suspend fun findMarker(title: String): MarkerObj? {
        var marker: MarkerObj? = null
        val query = ParseQuery<ParseObject>(CLASS_MARKER)
        query.whereContains("title", title)
        return suspendCoroutine { cont ->
            query.findInBackground { objects, e ->
                if (e == null)
                    objects?.let {
                        if (objects.isNotEmpty()) marker = getParseMarker(objects[0])
                    }
                cont.resume(marker)
            }
        }
    }

    override suspend fun saveMarker(marker: MarkerObj): String? {
        var key: String? = null
        if (marker.keyMarker == "") {
            if (marker.title != "") {
                val m = findMarker(marker.title)
                if (m == null) {
                    key = insMarker(marker)
                } else {
                    if (updMarker(m.keyMarker, marker)) key = m.keyMarker
                }
            }
        } else {
            if (updMarker(marker.keyMarker, marker)) key = marker.keyMarker
        }
        return key
    }

    override suspend fun listMarker(): List<MarkerObj> {
        var gamers: List<MarkerObj> = listOf()
        val query = ParseQuery.getQuery<ParseObject>(CLASS_MARKER)
        query.orderByDescending("createdAt")
        return suspendCoroutine { cont ->
            query.findInBackground { objects, e ->
                if (e == null) gamers = getListParseMarker(objects)
                cont.resume(gamers)
            }
        }
    }

    private fun getListParseMarker(objects: List<ParseObject>) = objects.map {
        getParseMarker(it)
    }

    companion object {
        private const val CLASS_MARKER = "Marker"
    }
}