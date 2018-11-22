package com.kirakishou.photoexchange.helper.permission


/**
 * Created by kirakishou on 7/31/2017.
 */
class PermissionRequest(val requestCode: Int,
                        val callback: (permissions: Array<out String>, grantResults: IntArray) -> Unit)