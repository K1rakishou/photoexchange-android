package com.kirakishou.photoexchange.mvp.model.photo

import android.os.Bundle
import com.kirakishou.photoexchange.helper.database.converter.PhotoStateConverter
import com.kirakishou.photoexchange.mvp.model.PhotoState
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

/**
 * Created by kirakishou on 3/9/2018.
 */
open class TakenPhoto(
  val id: Long,
  val isPublic: Boolean = false,
  var photoName: String? = null,
  var photoTempFile: File? = null,
  var photoState: PhotoState
) {

  fun getFile(): File {
    return photoTempFile!!
  }

  fun fileExists(): Boolean {
    return photoTempFile != null && photoTempFile!!.exists()
  }

  fun toBundle(): Bundle {
    val outBundle = Bundle()
    outBundle.putLong("photoId", id)
    outBundle.putInt("photo_state", PhotoStateConverter.fromPhotoState(photoState))
    outBundle.putString("photo_temp_file", photoTempFile?.absolutePath ?: "")

    return outBundle
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }

  override fun equals(other: Any?): Boolean {
    if (other == null) {
      return false
    }

    if (other === this) {
      return true
    }

    if (other::class != this::class) {
      return false
    }

    other as TakenPhoto

    return other.id == this.id
  }

  companion object {
    fun fromBundle(bundle: Bundle?): TakenPhoto {
      if (bundle == null) {
        throw IllegalArgumentException("Bundle is null")
      }

      val id = bundle.getLong("photoId", -1L)
      if (id == -1L) {
        throw IllegalStateException("No photoId in bundle")
      }

      val photoState = PhotoStateConverter.toPhotoState(bundle.getInt("photo_state"))
      val photoFileString = bundle.getString("photo_temp_file")
      val photoFile = if (photoFileString.isNullOrEmpty()) null else File(photoFileString)

      return TakenPhoto(id, false, null, photoFile, photoState)
    }
  }

}