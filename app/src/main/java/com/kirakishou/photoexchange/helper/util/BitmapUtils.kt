package com.kirakishou.photoexchange.helper.util

import java.io.File

interface BitmapUtils {
  fun rotatePhoto(photoFile: File, tempFile: File): Boolean
}