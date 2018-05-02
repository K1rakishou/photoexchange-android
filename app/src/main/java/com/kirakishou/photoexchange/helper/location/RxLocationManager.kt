package com.kirakishou.photoexchange.helper.location

import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers

/**
 * Created by kirakishou on 12/24/2017.
 */
object RxLocationManager {

    fun start(locationManager: MyLocationManager): Observable<LonLat> {
        val observable = Observable.create<LonLat> { emitter ->
            locationManager.start(object : MyLocationManager.OnLocationChanged {
                override fun onNewLocation(location: LonLat) {
                    if (!emitter.isDisposed) {
                        emitter.onNext(location)
                        emitter.onComplete()
                    }
                }
            })
        }

        return observable
                .subscribeOn(AndroidSchedulers.mainThread())
                .doFinally { locationManager.stop() }
    }
}