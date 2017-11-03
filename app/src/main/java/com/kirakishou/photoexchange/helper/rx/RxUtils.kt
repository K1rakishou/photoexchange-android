package com.kirakishou.photoexchange.helper.rx

import io.reactivex.Observable
import io.reactivex.functions.Predicate

/**
 * Created by kirakishou on 9/16/2017.
 */
object RxUtils {

    fun <T> splitRxStream(inStream: Observable<T>, vararg predicateArray: List<Predicate<T>>): List<Observable<T>> {
        val outStreams = ArrayList<Observable<T>>(predicateArray.size)
        val publishedObservable = inStream.publish().autoConnect(predicateArray.size)

        for (predicateList in predicateArray) {
            var stream: Observable<T> = publishedObservable

            for (predicate in predicateList) {
                stream = stream.filter(predicate)
            }

            outStreams += stream
        }

        return outStreams
    }
}