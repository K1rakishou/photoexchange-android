package com.kirakishou.photoexchange.helper.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.support.v4.app.ActivityCompat
import timber.log.Timber

/**
 * Created by kirakishou on 12/24/2017.
 */
class MyLocationManager(
        val context: Context
) {
    private val GPS_PROVIDER = "gps"
    private var listener: OnLocationChanged? = null

    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private val locationListener = object : LocationListener {
        override fun onStatusChanged(p0: String, p1: Int, p2: Bundle) {
        }

        override fun onProviderEnabled(provider: String) {
        }

        override fun onProviderDisabled(provider: String) {
        }

        override fun onLocationChanged(location: Location) {
            listener!!.onNewLocation(location)
        }
    }

    fun isGpsEnabled(): Boolean = locationManager.isProviderEnabled(GPS_PROVIDER)

    @SuppressLint("MissingPermission")
    fun start(listener: OnLocationChanged) {
        Timber.d("MyLocationManager: start")

        checkNotNull(listener)
        this.listener = listener

        checkPermissions()
        locationManager.requestSingleUpdate(GPS_PROVIDER, locationListener, Looper.getMainLooper())
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        Timber.d("MyLocationManager: stop")

        checkPermissions()
        locationManager.removeUpdates(locationListener)
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            throw IllegalStateException("Permission check failed")
        }
    }

    interface OnLocationChanged {
        fun onNewLocation(location: Location)
    }
}


















