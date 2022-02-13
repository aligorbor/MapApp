package ru.geekbrains.android2.mapapp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import ru.geekbrains.android2.mapapp.model.MarkerDB
import ru.geekbrains.android2.mapapp.model.MarkerDBImpl
import ru.geekbrains.android2.mapapp.model.MarkerObj

class MarkersViewModel(
    private val liveDataToObserve: MutableLiveData<AppStateMarkers> = MutableLiveData(),
    private val repository: MarkerDB = MarkerDBImpl()
) : ViewModel(), CoroutineScope by MainScope() {

    fun getLiveData() = liveDataToObserve

    fun getListOfMarkers() {
        liveDataToObserve.value = AppStateMarkers.Loading
        launch(Dispatchers.IO) {
            try {
                liveDataToObserve.postValue(
                    AppStateMarkers.Success(repository.listMarker())
                )
            } catch (e: Exception) {
                liveDataToObserve.postValue(AppStateMarkers.Error(e))
            }
        }
    }

    fun saveMarker(marker: MarkerObj) {
        liveDataToObserve.value = AppStateMarkers.Loading
        launch(Dispatchers.IO) {
            try {
                if (repository.saveMarker(marker) != null)
                    liveDataToObserve.postValue(
                        AppStateMarkers.Success(repository.listMarker())
                    )
            } catch (e: Exception) {
                liveDataToObserve.postValue(AppStateMarkers.Error(e))
            }
        }
    }

    fun deleteMarker(marker: MarkerObj) {
        liveDataToObserve.value = AppStateMarkers.Loading
        launch(Dispatchers.IO) {
            try {
                if (repository.delMarker(marker.keyMarker))
                    liveDataToObserve.postValue(
                        AppStateMarkers.Success(repository.listMarker())
                    )
            } catch (e: Exception) {
                liveDataToObserve.postValue(AppStateMarkers.Error(e))
            }
        }
    }
}