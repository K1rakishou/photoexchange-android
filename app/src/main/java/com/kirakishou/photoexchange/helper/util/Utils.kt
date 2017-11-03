package com.kirakishou.photoexchange.helper.util

import android.os.Looper
import okio.ByteString
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Created by kirakishou on 7/26/2017.
 */
object Utils {

    fun checkIsOnMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }

    fun throwIfOnMainThread() {
        if (checkIsOnMainThread()) {
            Timber.e("Current operation cannot be executed on the main thread")
            throw RuntimeException("Current operation cannot be executed on the main thread")
        }
    }

    fun getFileMd5(file: File): String {
        val fis = FileInputStream(file)

        val buffer = ByteArray(1024)
        val complete = MessageDigest.getInstance("MD5")
        var numRead: Int

        fis.use {
            do {
                numRead = it.read(buffer)
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead)
                }
            } while (numRead != -1)
        }

        val md5 = complete.digest()
        return ByteString.of(md5, 0, md5.size).hex()
    }

    fun distanceToString(distance: Double): String {
        if (distance < 1.0) {
            return "<1.0"
        }

        return distance.toString()
    }
}