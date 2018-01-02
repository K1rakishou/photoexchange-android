package com.kirakishou.photoexchange.helper.util

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Looper
import com.kirakishou.photoexchange.mwvm.model.other.Fickle
import okio.ByteString
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Created by kirakishou on 7/26/2017.
 */
object Utils {

    private val numericAlphabetic = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val random = SecureRandom()

    fun generateRandomString(len: Int, alphabet: String): String {
        val bytes = ByteArray(len)
        random.nextBytes(bytes)

        val sb = StringBuilder()
        val alphabetLen = alphabet.length

        for (i in 0 until len) {
            sb.append(alphabet[Math.abs(bytes[i] % alphabetLen)])
        }

        return sb.toString()
    }

    fun generateUserId(): String {
        return generateRandomString(64, numericAlphabetic) + "_photoexchange"
    }

    fun rotateBitmap(oldBitmap: Bitmap, rotation: Int): Fickle<String> {
        checkNotNull(oldBitmap)

        val tempFile = File.createTempFile("photo", ".tmp")

        try {
            val matrix = Matrix()

            when (rotation) {
                0 -> {
                    Timber.d("Applying additional photo rotation: 0f")
                    matrix.setRotate(0f)
                }
                90 -> {
                    Timber.d("Applying additional photo rotation: -90f")
                    matrix.setRotate(-90f)
                }
                180 -> {
                    Timber.d("Applying additional photo rotation: -180f")
                    matrix.setRotate(-180f)
                }
                270 -> {
                    Timber.d("Applying additional photo rotation: -270f")
                    matrix.setRotate(-270f)
                }
                else -> {
                    Timber.d("Unknown rotation. Applying no additional rotation")
                    matrix.setRotate(0f)
                }
            }

            try {
                val rotatedBitmap = Bitmap.createBitmap(oldBitmap, 0, 0, oldBitmap.width, oldBitmap.height, matrix, true)
                val out = FileOutputStream(tempFile)

                try {
                    rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                } finally {
                    rotatedBitmap.recycle()
                    out.close()
                }
            } finally {
                oldBitmap.recycle()
            }

            return Fickle.of(tempFile.absolutePath)
        } catch (error: Throwable) {
            Timber.e(error)

            if (tempFile.exists()) {
                tempFile.delete()
            }

            return Fickle.empty()
        }
    }
}