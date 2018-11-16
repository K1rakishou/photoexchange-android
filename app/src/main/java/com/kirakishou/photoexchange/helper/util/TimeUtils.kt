package com.kirakishou.photoexchange.helper.util

interface TimeUtils {
  fun getTimeFast(): Long
  fun formatDate(time: Long): String
  fun formatDateAndTime(time: Long): String
}