package com.kirakishou.photoexchange.mvp.view

import com.kirakishou.photoexchange.mvp.model.MyPhoto

/**
 * Created by kirakishou on 3/9/2018.
 */
interface ViewTakenPhotoActivityView : BaseView {
    fun hideControls()
    fun showControls()
    fun onPhotoUpdated(takenPhoto: MyPhoto)
}