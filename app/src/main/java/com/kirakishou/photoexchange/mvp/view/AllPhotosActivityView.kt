package com.kirakishou.photoexchange.mvp.view

import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapter
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Created by kirakishou on 3/11/2018.
 */
interface AllPhotosActivityView : BaseView {
    fun getCurrentLocation(): Single<LonLat>
}