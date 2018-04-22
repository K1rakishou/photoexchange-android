package com.kirakishou.photoexchange.mvp.model

class FindPhotosData(val userId: String?,
                     val photoNames: String) {
    fun isEmpty(): Boolean {
        return userId == null || photoNames.isEmpty()
    }
}