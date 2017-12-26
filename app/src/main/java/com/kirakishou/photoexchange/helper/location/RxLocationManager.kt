package com.kirakishou.photoexchange.helper.location

import com.kirakishou.photoexchange.mwvm.model.other.LonLat
import io.reactivex.Observable

/**
 * Created by kirakishou on 12/24/2017.
 */
object RxLocationManager {

    fun start(locationManager: MyLocationManager): Observable<LonLat> {
        val observable = Observable.create<LonLat> { emitter ->
            locationManager.start(object : MyLocationManager.OnLocationChanged {
                override fun onNewLocation(location: LonLat) {
                    emitter.onNext(location)
                }
            })
        }

        return observable
                .doOnDispose { locationManager.stop() }
    }
}