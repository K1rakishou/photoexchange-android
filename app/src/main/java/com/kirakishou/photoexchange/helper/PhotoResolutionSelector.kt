package com.kirakishou.photoexchange.helper

import io.fotoapparat.parameter.Resolution
import timber.log.Timber

/**
 * Created by kirakishou on 1/2/2018.
 */

class PhotoResolutionSelector(
    val resolutions: Iterable<Resolution>
) {

    private val idealWidth = 1920
    private val idealHeight = 1080
    private val tag = this::class.java.simpleName

    fun select(): Resolution {
        val idealResolution = Resolution(idealWidth, idealHeight)
        val distancesMap = hashMapOf<Double, Resolution>()

        for (size in resolutions) {
            val distance = Math.hypot((size.width - idealResolution.width).toDouble(), (size.height - idealResolution.height).toDouble())
            Timber.tag(tag).d("distance: $distance, size.width: ${size.width}, size.height: ${size.height}")

            distancesMap[distance] = size
        }

        val closestDistKey = distancesMap.keys.sorted().first()
        val resultResolution = distancesMap[closestDistKey]!!
        Timber.tag(tag).d("result resolution: width = ${resultResolution.width}, height = ${resultResolution.height}")

        return resultResolution
    }

}
