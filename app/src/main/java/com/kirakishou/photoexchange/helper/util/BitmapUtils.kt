package com.kirakishou.photoexchange.helper.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.support.media.ExifInterface
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream


/**
 * Created by kirakishou on 1/5/2018.
 */
object BitmapUtils {

    fun rotatePhoto(photoFile: File, tempFile: File): Boolean {
        try {
            val photoFilePath = photoFile.absolutePath
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            val photoBitmap = BitmapFactory.decodeFile(photoFilePath, options)
                ?: return false

            try {
                val ei = ExifInterface(photoFilePath)
                val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                val rotatedBitmap = doRotation(photoBitmap, orientation)
                val out = FileOutputStream(tempFile)

                try {
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                } finally {
                    rotatedBitmap.recycle()
                    out.close()
                }
            } finally {
                photoBitmap.recycle()
            }

            return true
        } catch (error: Throwable) {
            Timber.e(error)
            return false
        }
    }

    private fun doRotation(bitmap: Bitmap, orientation: Int): Bitmap {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                return rotate(bitmap, 90f)
            }

            ExifInterface.ORIENTATION_ROTATE_180 -> {
                return rotate(bitmap, 180f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> {
                return rotate(bitmap, 270f)
            }

            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                return flip(bitmap, true, false)
            }

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                return flip(bitmap, false, true)
            }

            else -> return bitmap
        }
    }

    private fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun flip(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix()
        val sx = (if (horizontal) -1 else 1).toFloat()
        val sy = (if (vertical) -1 else 1).toFloat()

        matrix.preScale(sx, sy)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}