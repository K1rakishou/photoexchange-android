package com.kirakishou.photoexchange.mvp.view

import com.kirakishou.photoexchange.mvp.model.MyPhoto
import io.reactivex.Single
import java.io.File

/**
 * Created by kirakishou on 3/3/2018.
 */
interface TakePhotoActivityView : BaseView{
    fun showTakePhotoButton()
    fun hideTakePhotoButton()
    fun takePhoto(file: File): Single<Boolean>
    fun onPhotoTaken(myPhoto: MyPhoto)
}