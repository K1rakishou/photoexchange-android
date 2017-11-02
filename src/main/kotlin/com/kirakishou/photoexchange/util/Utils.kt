package com.kirakishou.photoexchange.util

import org.springframework.core.io.buffer.DataBuffer

object Utils {

    fun dataBufferToString(dataBufferList: List<DataBuffer>): String {
        val fullLength = dataBufferList.sumBy { it.readableByteCount() }
        val array = ByteArray(fullLength)
        var offset = 0

        for (dataBuffer in dataBufferList) {
            val dataBufferArray = dataBuffer.asByteBuffer().array()
            val arrayLength = dataBuffer.readableByteCount()

            System.arraycopy(dataBufferArray, 0, array, offset, arrayLength)
            offset += arrayLength
        }

        return String(array)
    }
}