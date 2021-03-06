package com.kirakishou.photoexchange.mvrx.model

import android.os.Bundle

data class NewReceivedPhoto(
  val uploadedPhotoName: String,
  val receivedPhotoName: String,
  val lon: Double,
  val lat: Double,
  val uploadedOn: Long
) {

  fun toBundle(bundle: Bundle) {
    bundle.putString(uploadedPhotoNameField, uploadedPhotoName)
    bundle.putString(receivedPhotoNameField, receivedPhotoName)
    bundle.putDouble(receiverLonField, lon)
    bundle.putDouble(receiverLatField, lat)
    bundle.putLong(uploadedOnField, uploadedOn)
  }

  companion object {
    const val uploadedPhotoNameField = "uploaded_photo_name"
    const val receivedPhotoNameField = "received_photo_name"
    const val receiverLonField = "lon"
    const val receiverLatField = "lat"
    const val uploadedOnField = "uploaded_on"

    fun fromBundle(bundle: Bundle?): NewReceivedPhoto? {
      if (bundle == null) {
        return null
      }

      return NewReceivedPhoto(
        requireNotNull(bundle.getString(uploadedPhotoNameField)),
        requireNotNull(bundle.getString(receivedPhotoNameField)),
        bundle.getDouble(receiverLonField),
        bundle.getDouble(receiverLatField),
        bundle.getLong(uploadedOnField)
      )
    }
  }
}