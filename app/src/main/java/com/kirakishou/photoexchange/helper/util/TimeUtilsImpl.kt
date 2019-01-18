package com.kirakishou.photoexchange.helper.util

import java.util.concurrent.TimeUnit

/**
 * Created by kirakishou on 9/12/2017.
 */
open class TimeUtilsImpl : TimeUtils {
  //there are 26 time zones
  private val oneDay = TimeUnit.HOURS.toMillis(26)

  override fun getTimeFast(): Long = System.currentTimeMillis()

  /**
   * FIXME: A better way to do this is to get the difference between the server and the client time
   *
   * Client and server may be in different time zones.
   * When we receive photos from the server they contain time that is local to the server.
   * When we search for photos in the local database by the time they have been uploaded at we use client's local time.
   * We need to compensate that.
   * By adding 26 hours to the current time we ensure that the condition "currentTime >= uploadedOn"
   * is always true for any photo in the database
   * */
  override fun getTimePlus26Hours(): Long = getTimeFast() + oneDay
}