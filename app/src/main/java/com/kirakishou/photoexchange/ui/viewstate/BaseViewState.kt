package com.kirakishou.photoexchange.ui.viewstate

import android.os.Bundle

interface BaseViewState {
    fun saveToBundle(bundle: Bundle?)
    fun loadFromBundle(bundle: Bundle?)
}