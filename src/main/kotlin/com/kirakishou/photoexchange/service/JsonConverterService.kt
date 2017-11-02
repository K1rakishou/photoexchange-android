package com.kirakishou.photoexchange.service

import org.springframework.core.io.buffer.DataBuffer

interface JsonConverterService {
    fun <T> fromJson(dataBufferList: List<DataBuffer>, clazz: Class<*>): T
}