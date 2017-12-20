package com.kirakishou.photoexchange.ui.activity

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import butterknife.BindView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.mwvm.model.other.LonLat
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign

class MapActivity : BaseActivity<Nothing>(), OnMapReadyCallback {

    @BindView(R.id.iv_close_activity)
    lateinit var ivCloseActivity: ImageView

    private val DEFAULT_MAP_ZOOM = 9f
    private var location: LatLng? = null

    override fun getContentView(): Int = R.layout.activity_map
    override fun initViewModel(): Nothing? = null
    override fun onInitRx() {}

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        getLocation(intent)

        val mapFrag = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFrag.getMapAsync(this)

        compositeDisposable += RxView.clicks(ivCloseActivity)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ finish() })
    }

    private fun getLocation(intent: Intent) {
        val lon = intent.getDoubleExtra("lon", 0.0)
        val lat = intent.getDoubleExtra("lat", 0.0)

        check(lon != 0.0)
        check(lat != 0.0)

        location = LatLng(lat, lon)
    }

    override fun onActivityDestroy() {
    }

    override fun onMapReady(map: GoogleMap) {
        map.addMarker(MarkerOptions().position(location!!))
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_MAP_ZOOM))
    }

    override fun resolveDaggerDependency() {
    }

}
