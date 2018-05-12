package com.kirakishou.photoexchange.mvp.model

class UploadedPhoto(
    val photoId: Long,
    val photoName: String,
    var receiverInfo: ReceiverInfo = ReceiverInfo.empty()
) {

    fun hasReceivedInfo(): Boolean {
        return receiverInfo.hasReceivedInfo()
    }

    class ReceiverInfo(
        val lon: Double,
        val lat: Double
    ) {

        fun hasReceivedInfo(): Boolean {
            return lon != 0.0 && lat != 0.0
        }

        companion object {
            fun empty(): ReceiverInfo {
                return ReceiverInfo(0.0, 0.0)
            }
        }
    }
}