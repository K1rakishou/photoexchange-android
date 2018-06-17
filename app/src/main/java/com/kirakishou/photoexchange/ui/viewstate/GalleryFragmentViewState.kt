package com.kirakishou.photoexchange.ui.viewstate

import android.os.Bundle

class GalleryFragmentViewState(
    var lastId: Long = Long.MAX_VALUE
) : BaseViewState {

    fun updateLastId(lastId: Long) {
        this.lastId = lastId
    }

    override fun saveToBundle(bundle: Bundle?) {
        if (bundle != null) {
        }
    }

    override fun loadFromBundle(bundle: Bundle?) {
        if (bundle != null) {
        }
    }

    companion object {
    }
}