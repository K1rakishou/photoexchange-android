package com.kirakishou.photoexchange.helper.location

import com.kirakishou.photoexchange.mwvm.model.other.LonLat
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * Created by kirakishou on 12/24/2017.
 */
object RxLocationManager {

    fun start(locationManager: MyLocationManager): Observable<LonLat> {
        val observable = Observable.create<LonLat> { subscriber ->
            locationManager.start(object : MyLocationManager.OnLocationChanged {
                override fun onNewLocation(location: LonLat) {
                    if (!subscriber.isDisposed) {
                        subscriber.onNext(location)
                    }
                }
            })
        }

        return observable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnDispose { locationManager.stop() }
    }
}