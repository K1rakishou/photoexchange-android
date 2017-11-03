package com.kirakishou.photoexchange.helper.permission

import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

/**
 * Created by kirakishou on 7/31/2017.
 */
class PermissionManager {

    private val pendingPermissions = arrayListOf<PermissionRequest>()

    private fun checkPermission(activity: Activity, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun askForPermission(activity: Activity, permission: String, requestCode: Int, callback: (granted: Boolean) -> Unit) {
        if (checkPermission(activity, permission)) {
            callback.invoke(true)
            return
        }

        val permissions = arrayOf(permission)
        val permissionRequest = PermissionRequest(requestCode, permissions, callback)
        pendingPermissions.add(permissionRequest)

        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        var indexToRemove = -1

        for ((index, permission) in pendingPermissions.withIndex()) {
            if (permission.requestCode == requestCode) {
                indexToRemove = index

                val granted = grantResults.none {
                    it != PackageManager.PERMISSION_GRANTED
                }

                permission.callback.invoke(granted)
                break
            }
        }

        pendingPermissions.removeAt(indexToRemove)
    }
}