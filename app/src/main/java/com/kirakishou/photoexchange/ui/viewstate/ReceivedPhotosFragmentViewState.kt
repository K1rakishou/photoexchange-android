package com.kirakishou.photoexchange.ui.viewstate

import android.os.Bundle

class ReceivedPhotosFragmentViewState(
    var lastId: Long = Long.MAX_VALUE
) : BaseViewState {

    fun updateLastId(lastId: Long) {
        this.lastId = lastId
    }

    override fun saveToBundle(bundle: Bundle?) {
        if (bundle != null) {
            bundle.putLong(LAST_ID, lastId)
        }
    }

    override fun loadFromBundle(bundle: Bundle?) {
        if (bundle != null) {
            lastId = bundle.getLong(LAST_ID, Long.MAX_VALUE)
        }
    }

    companion object {
        const val LAST_ID = "last_id"
    }
}