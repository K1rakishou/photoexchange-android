package com.kirakishou.photoexchange.helper

import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.service.UploadPhotoServiceCallbacks
import okhttp3.MediaType
import okio.BufferedSink
import okhttp3.RequestBody
import java.io.File
import java.io.FileInputStream
import java.lang.ref.WeakReference


/**
 * Created by kirakishou on 3/17/2018.
 */
class ProgressRequestBody(
    private val photoFile: File,
    private val callback: UploadPhotosUseCase.PhotoUploadProgressCallback
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
            while (true) {
                val read = fis.read(buffer)
                if (read == -1) {
                    break
                }

                val percent = 100L * uploaded / fileLength
                if (percent - lastPercent >= 3) {
                    lastPercent = percent
                    callback.onProgress(percent.toInt())
                }

                uploaded += read.toLong()
                sink.write(buffer, 0, read)
            }

            callback.onProgress(100)
        } finally {
            fis.close()
        }
    }
}