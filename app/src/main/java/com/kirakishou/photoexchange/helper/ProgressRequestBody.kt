package com.kirakishou.photoexchange.helper

import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
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
  private val DEFAULT_BUFFER_SIZE = 4096

  override fun contentType(): MediaType? {
    return MediaType.parse("image/*")
  }

  override fun contentLength(): Long {
    return photoFile.length()
  }

  override fun writeTo(sink: BufferedSink) {
    val fileLength = photoFile.length()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
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

          val percent = 100L * uploaded / fileLength
          if (percent - lastPercent >= 3) {
            lastPercent = percent
            channel.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress(photo, percent.toInt()))
          }

          uploaded += read.toLong()
          sink.write(buffer, 0, read)
        }

        channel.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress(photo, 100))
      }
    } finally {
      fis.close()
    }
  }
}