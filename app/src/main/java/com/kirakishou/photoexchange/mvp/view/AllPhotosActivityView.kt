package com.kirakishou.photoexchange.mvp.view

import com.kirakishou.photoexchange.ui.adapter.UploadedPhotosAdapter
import io.reactivex.Observable

/**
 * Created by kirakishou on 3/11/2018.
 */
interface AllPhotosActivityView : BaseView {
    fun handleUploadedPhotosFragmentAdapterButtonClicks(adapterButtonsClickEvent: UploadedPhotosAdapter.UploadedPhotosAdapterButtonClickEvent): Observable<Boolean>
}