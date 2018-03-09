package com.kirakishou.photoexchange.helper.permission

import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 7/31/2017.
 */
class PermissionRequest(val requestCode: Int,
                        val permissions: Array<out String>,
                        val callback: WeakReference<(permissions: Array<out String>, grantResults: IntArray) -> Unit>)