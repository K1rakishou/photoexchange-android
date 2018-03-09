package com.kirakishou.photoexchange.mvp.view

import android.widget.Toast

/**
 * Created by kirakishou on 3/8/2018.
 */
interface BaseView {
    fun showToast(message: String, duration: Int = Toast.LENGTH_LONG)
}