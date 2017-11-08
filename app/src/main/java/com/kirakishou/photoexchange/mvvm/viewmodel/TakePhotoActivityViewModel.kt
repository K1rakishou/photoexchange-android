package com.kirakishou.photoexchange.mvvm.viewmodel

import android.os.Debug
import com.kirakishou.fixmypc.photoexchange.BuildConfig
import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.mvvm.model.Constants
import com.kirakishou.photoexchange.mvvm.model.LonLat
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.error.MainActivityViewModelErrors
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.input.MainActivityViewModelInputs
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.output.MainActivityViewModelOutputs
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by kirakishou on 11/3/2017.
 */
class TakePhotoActivityViewModel
@Inject constructor(
        val takenPhotosRepo: TakenPhotosRepository
) : BaseViewModel(),
        MainActivityViewModelInputs,
        MainActivityViewModelOutputs,
        MainActivityViewModelErrors {

    val inputs: MainActivityViewModelInputs = this
    val outputs: MainActivityViewModelOutputs = this
    val errors: MainActivityViewModelErrors = this

    //TODO: redo this asynchronously
    fun saveTakenPhotoToDb(location: LonLat, userId: String, photoFilePath: String): Long {
        val id = takenPhotosRepo.saveOne(location.lon, location.lat, userId, photoFilePath)

        if (Constants.isDebugBuild) {
            val allPhotos = takenPhotosRepo.findAll()

            allPhotos.forEach { Timber.d(it.toString()) }
        }

        return id
    }

    override fun onCleared() {
        Timber.e("TakePhotoActivityViewModel.onCleared()")

        super.onCleared()
    }
}