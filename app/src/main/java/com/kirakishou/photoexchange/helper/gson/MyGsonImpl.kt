package com.kirakishou.photoexchange.helper.gson

import com.google.gson.Gson

class MyGsonImpl(
  private val gson: Gson
) : MyGson {

  override fun <T> fromJson(json: String, clazz: Class<*>): T? {
    return gson.fromJson(json, clazz) as T
  }

  override fun toJson(src: Any): String {
    return gson.toJson(src)
  }
}