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
                        AppStateMaps.Success
                    )
            } catch (e: Exception) {
                liveDataToObserve.postValue(AppStateMaps.Error(e))
            }
        }
    }
}