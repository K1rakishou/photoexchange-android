package com.kirakishou.photoexchange.helper.permission

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by kirakishou on 7/31/2017.
 */
open class PermissionManager {

  private val pendingPermissions = arrayListOf<PermissionRequest>()
  private val requestCodeSeed = AtomicInteger(0)

  private fun checkPermission(activity: Activity, permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
  }

  fun askForPermission(activity: Activity, permissions: Array<String>,
                       callback: (permissions: Array<out String>, grantResults: IntArray) -> Unit) {

    val allGranted = permissions.all { permission -> checkPermission(activity, permission) }
    if (allGranted) {
      callback.invoke(permissions, intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED))
      return
    }

    val requestCode = requestCodeSeed.getAndIncrement()
    val permissionRequest = PermissionRequest(requestCode, permissions, callback)
    pendingPermissions.add(permissionRequest)

    ActivityCompat.requestPermissions(activity, permissions, requestCode)
  }

  fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    val permission = pendingPermissions.first { permission -> permission.requestCode == requestCode }

    pendingPermissions.removeAll { it.requestCode == requestCode }
    permission.callback.invoke(permissions, grantResults)
  }
}