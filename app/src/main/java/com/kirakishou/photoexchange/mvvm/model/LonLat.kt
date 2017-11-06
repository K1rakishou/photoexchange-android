package com.kirakishou.photoexchange.mvvm.model

/**
 * Created by kirakishou on 11/3/2017.
 */
class LonLat(val lon: Double,
             val lat: Double) {

    override fun toString(): String {
        return "[lon: $lon, lat: $lat]"
    }

    companion object {
        fun empty(): LonLat {
            return LonLat(0.0, 0.0)
        }
    }
}