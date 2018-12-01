package com.kirakishou.photoexchange.helper

import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okio.BufferedSink
import okhttp3.RequestBody
import java.io.File
import java.io.FileInputStream


/**
 * Created by kirakishou on 3/17/2018.
 */
class ProgressRequestBody(
  private val photoFile: File,
  private val photo: TakenPhoto,
  private val channel: SendChannel<UploadedPhotosFragmentEvent.PhotoUploadEvent>
) : RequestBody() {
  private val TAG = "ProgressRequestBody"
  private val defaultBufferSize = 4096
  private val maxPercent = 100
  private val percentStep = 10

  override fun contentType(): MediaType? {
    return MediaType.parse("image/*")
  }

  override fun contentLength(): Long {
    return photoFile.length()
  }

  override fun writeTo(sink: BufferedSink) {
    val fileLength = photoFile.length()
    val buffer = ByteArray(defaultBufferSize)
    val fis = FileInputStream(photoFile)
    var uploaded: Long = 0
    var lastPercent = 0L

    try {
      runBlocking {
        while (true) {
          val read = fis.read(buffer)
          if (read == -1) {
            break
          }

          val percent = maxPercent * uploaded / fileLength
          if (percent - lastPercent >= percentStep) {
            lastPercent = percent
            channel.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, percent.toInt()))
          }

          uploaded += read.toLong()
          sink.write(buffer, 0, read)
        }

        channel.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, maxPercent))
      }
    } finally {
      fis.close()
    }
  }
}