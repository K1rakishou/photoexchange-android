package com.kirakishou.photoexchange.helper.location

import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers

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
                        subscriber.onComplete()
                    }
                }
            })
        }

        return observable
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnDispose { locationManager.stop() }
    }
}