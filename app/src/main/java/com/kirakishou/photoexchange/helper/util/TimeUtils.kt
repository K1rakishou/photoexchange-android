package com.kirakishou.photoexchange.helper.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by kirakishou on 9/12/2017.
 */
object TimeUtils {

    @Synchronized
    fun getTimeFast(): Long = System.currentTimeMillis()

    fun format(time: Long): String {
        try {
            val locale = Locale("ru", "RU")
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", locale)

            return dateFormat.format(Date(time))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ""
    }
}