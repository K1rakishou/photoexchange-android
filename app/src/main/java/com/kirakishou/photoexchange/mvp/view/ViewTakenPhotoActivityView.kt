package com.kirakishou.photoexchange.mvp.view


/**
 * Created by kirakishou on 3/9/2018.
 */
interface ViewTakenPhotoActivityView : BaseView {
    fun hideControls()
    fun showControls()
    fun onPhotoUpdated()
}