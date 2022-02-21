package ru.geekbrains.android2.mapapp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.beust.klaxon.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.async
import org.jetbrains.anko.uiThread
import ru.geekbrains.android2.mapapp.model.API.DirectionLoader
import ru.geekbrains.android2.mapapp.model.DB.MarkerDB
import ru.geekbrains.android2.mapapp.model.DB.MarkerDBImpl
import ru.geekbrains.android2.mapapp.model.DB.MarkerObj
import ru.geekbrains.android2.mapapp.utils.decodePoly
import java.net.URL

class MapsViewModel(
    private val liveDataToObserve: MutableLiveData<AppStateMaps> = MutableLiveData(),
    private val repository: MarkerDB = MarkerDBImpl()
) : ViewModel(), CoroutineScope by MainScope() {
    fun getLiveData() = liveDataToObserve
    fun saveMarker(marker: MarkerObj) {
        liveDataToObserve.value = AppStateMaps.Loading
        launch(Dispatchers.IO) {
            try {
                if (repository.saveMarker(marker) != null)
                    liveDataToObserve.postValue(
                        AppStateMaps.SuccessMarker
                    )
            } catch (e: Exception) {
                liveDataToObserve.postValue(AppStateMaps.Error(e))
            }
        }
    }

    fun getDirection(from: LatLng, to: LatLng, key: String) {
        liveDataToObserve.value = AppStateMaps.Loading
        val url = DirectionLoader.getURL(from, to, key)
        async {
            // Connect to URL, download content and convert into string asynchronously
            val result = URL(url).readText()
            uiThread {
                // When API call is done, create parser and convert into JsonObjec
                val parser = Parser()
                val stringBuilder: StringBuilder = StringBuilder(result)
                val json: JsonObject = parser.parse(stringBuilder) as JsonObject
                // get to the correct element in JsonObject
                val routes = json.array<JsonObject>("routes")
                try {
                    val points = routes!!["legs"]["steps"][0] as JsonArray<JsonObject>
                    // For every element in the JsonArray, decode the polyline string and pass all points to a List
                    val polypts =
                        points.flatMap { decodePoly(it.obj("polyline")?.string("points")!!) }
                    liveDataToObserve.setValue(AppStateMaps.SuccessDirection(polypts))
                } catch (e: Exception) {
                    liveDataToObserve.postValue(AppStateMaps.Error(e))
                }
            }
        }
    }
}