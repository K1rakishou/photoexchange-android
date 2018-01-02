package com.kirakishou.photoexchange.helper

import io.fotoapparat.parameter.Size
import io.fotoapparat.parameter.selector.SelectorFunction
import timber.log.Timber

/**
 * Created by kirakishou on 1/2/2018.
 */

class PhotoSizeSelector : SelectorFunction<Collection<Size>, Size> {

    private val idealWidth = 1920
    private val idealHeight = 1080
    private val tag = this::class.java.simpleName

    override fun select(sizes: Collection<Size>): Size {
        val idealSize = Size(idealWidth, idealHeight)
        val distancesMap = hashMapOf<Double, Size>()

        for (size in sizes) {
            val distance = Math.hypot((size.width - idealSize.width).toDouble(), (size.height - idealSize.height).toDouble())
            Timber.tag(tag).d("distance: $distance, size.width: ${size.width}, size.height: ${size.height}")

            distancesMap.put(distance, size)
        }

        val closestDistKey = distancesMap.keys.sorted().first()
        val resultSize = distancesMap[closestDistKey]!!
        Timber.tag(tag).d("result size: width = ${resultSize.width}, height = ${resultSize.height}")

        return resultSize
    }

}
