package com.kirakishou.photoexchange.helper.location

import android.location.Location
import io.reactivex.Observable

/**
 * Created by kirakishou on 12/24/2017.
 */
object RxLocationManager {

    fun start(locationManager: MyLocationManager): Observable<Location> {
        val observable = Observable.create<Location> { emitter ->
            locationManager.start(object : MyLocationManager.OnLocationChanged {
                override fun onNewLocation(location: Location) {
                    emitter.onNext(location)
                }
            })
        }

        return observable
                .doOnDispose { locationManager.stop() }
    }
}