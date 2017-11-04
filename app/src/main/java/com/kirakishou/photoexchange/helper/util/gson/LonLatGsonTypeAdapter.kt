package com.kirakishou.photoexchange.helper.util.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.kirakishou.photoexchange.mvvm.model.LonLat

/**
 * Created by kirakishou on 11/4/2017.
 */
class LonLatGsonTypeAdapter : TypeAdapter<LonLat>() {

    override fun read(input: JsonReader): LonLat {
        val lon = input.nextDouble()
        val lat = input.nextDouble()
        return LonLat(lon, lat)
    }

    override fun write(output: JsonWriter, lonLat: LonLat) {
        output.jsonValue("lon")!!.value(lonLat.lon)
        output.jsonValue("lat")!!.value(lonLat.lat)
    }
}