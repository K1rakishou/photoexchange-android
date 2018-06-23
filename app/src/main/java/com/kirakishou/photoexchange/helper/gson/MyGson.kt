package com.kirakishou.photoexchange.helper.gson

interface MyGson {
    fun <T> fromJson(json: String, clazz: Class<*>): T?
    fun toJson(src: Any): String
}