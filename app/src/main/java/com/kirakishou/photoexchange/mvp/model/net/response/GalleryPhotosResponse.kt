package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class GalleryPhotosResponse
private constructor(

  @Expose
  @SerializedName("gallery_photos")
  val galleryPhotos: List<GalleryPhotoResponseData>,

  errorCode: ErrorCode
) : StatusResponse(errorCode.value, errorCode) {

  companion object {
    fun success(galleryPhotos: List<GalleryPhotoResponseData>): GalleryPhotosResponse {
      return GalleryPhotosResponse(galleryPhotos, ErrorCode.Ok)
    }

    fun fail(errorCode: ErrorCode): GalleryPhotosResponse {
      return GalleryPhotosResponse(emptyList(), errorCode)
    }
  }

  class GalleryPhotoResponseData(

    @Expose
    @SerializedName("id")
    val id: Long,

    @Expose
    @SerializedName("photo_name")
    val photoName: String,

    @Expose
    @SerializedName("lon")
    val lon: Double,

    @Expose
    @SerializedName("lat")
    val lat: Double,

    @Expose
    @SerializedName("uploaded_on")
    val uploadedOn: Long,

    @Expose
    @SerializedName("favourites_count")
    val favouritesCount: Long
  )
}