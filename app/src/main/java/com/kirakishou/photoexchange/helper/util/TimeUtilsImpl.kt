package com.kirakishou.photoexchange.helper.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by kirakishou on 9/12/2017.
 */
open class TimeUtilsImpl : TimeUtils {

    override fun getTimeFast(): Long = System.currentTimeMillis()

    override fun formatDate(time: Long): String {
        try {
            val locale = Locale("ru", "RU")
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", locale)

            return dateFormat.format(Date(time))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ""
    }

    override fun formatDateAndTime(time: Long): String {
        try {
            val locale = Locale("ru", "RU")
            val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", locale)

            return dateFormat.format(Date(time))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ""
    }
}