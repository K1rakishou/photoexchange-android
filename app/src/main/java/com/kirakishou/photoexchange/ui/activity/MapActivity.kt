package com.kirakishou.photoexchange.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.MapActivityModule
import com.kirakishou.photoexchange.helper.permission.PermissionManager
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import javax.inject.Inject


class MapActivity : BaseActivity() {

    @BindView(R.id.map)
    lateinit var mapView: MapView

    @Inject
    lateinit var permissionManager: PermissionManager

    override fun getContentView(): Int = R.layout.activity_map

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {

    }

    override fun onActivityStart() {
        checkPermissions()
    }

    override fun onActivityStop() {
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    private fun checkPermissions() {
        val requestedPermissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        permissionManager.askForPermission(this, requestedPermissions) { _, grantResults ->
            if (grantResults.any { it == PackageManager.PERMISSION_DENIED }) {
                finish()
                return@askForPermission
            }

            initMap()
        }
    }

    private fun initMap() {
        mapView.isClickable = true
        mapView.setBuiltInZoomControls(true)

        mapView.controller.setZoom(15.0)
        mapView.setUseDataConnection(false)
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun resolveDaggerDependency() {
        (application as PhotoExchangeApplication).applicationComponent
            .plus(MapActivityModule())
            .inject(this)
    }
}
