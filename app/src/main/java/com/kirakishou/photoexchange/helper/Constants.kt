package com.kirakishou.photoexchange.helper

import com.kirakishou.fixmypc.photoexchange.BuildConfig
import com.kirakishou.photoexchange.helper.extension.hours
import com.kirakishou.photoexchange.helper.extension.mb
import java.util.concurrent.TimeUnit

/**
 * Created by kirakishou on 11/8/2017.
 */
object Constants {
  val isDebugBuild = BuildConfig.DEBUG
  const val appid = "com.kirakishou.photoexchange"
  const val BASE_URL = "http://kez1911.asuscomm.com:8080/"
  const val BASE_PHOTOS_URL = "${BASE_URL}v1/api/get_photo"
  const val BASE_STATIC_MAP_URL = "${BASE_URL}v1/api/get_static_map"
  const val DATABASE_NAME = "photoexchange_db"
  const val DELIMITER = ","

  const val DEFAULT_ADAPTER_ITEM_WIDTH = 288

  //TODO: change in production
  const val DEFAULT_PHOTOS_PER_PAGE_COUNT = 5

  //TODO: change in production
  val INSERTED_EARLIER_THAN_TIME_DELTA = TimeUnit.MINUTES.toMillis(1)

  //TODO: change in production
  val BLACKLISTED_EARLIER_THAN_TIME_DELTA = TimeUnit.MINUTES.toMillis(1)

  val MAX_CACHE_SIZE = 50.mb()
  val OLD_PHOTO_TIME_THRESHOLD = 1.hours()
  val FILES_TO_DELETE_AT_A_TIME = 7

  val CHANNEL_ID = "1"
  val CHANNEL_NAME = "my_channel"
}