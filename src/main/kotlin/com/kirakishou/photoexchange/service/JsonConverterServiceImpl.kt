package com.kirakishou.photoexchange.service

import com.google.gson.Gson
import com.kirakishou.photoexchange.util.Utils
import org.springframework.core.io.buffer.DataBuffer

class JsonConverterServiceImpl(
        private val gson: Gson
) : JsonConverterService {

    @Suppress("UNCHECKED_CAST")
    override fun <T> fromJson(dataBufferList: List<DataBuffer>, clazz: Class<*>): T {
        return gson.fromJson(Utils.dataBufferToString(dataBufferList), clazz) as T
    }

}