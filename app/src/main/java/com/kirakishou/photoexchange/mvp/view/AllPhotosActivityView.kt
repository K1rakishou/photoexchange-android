package com.kirakishou.photoexchange.mvp.view

import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Observable

/**
 * Created by kirakishou on 3/11/2018.
 */
interface AllPhotosActivityView : BaseView {
    fun startUploadingService()
    fun getCurrentLocation(): Observable<LonLat>
    fun onUploadedPhotosRetrieved(uploadedPhotos: List<MyPhoto>)
}