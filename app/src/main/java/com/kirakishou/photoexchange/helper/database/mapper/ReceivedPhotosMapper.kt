package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import net.response.ReceivedPhotosResponse

object ReceivedPhotosMapper {

  object FromEntity {
    fun toReceivedPhoto(receivedPhotoEntity: ReceivedPhotoEntity): ReceivedPhoto {
      return ReceivedPhoto(
        receivedPhotoEntity.id!!,
        receivedPhotoEntity.uploadedPhotoName!!,
        receivedPhotoEntity.receivedPhotoName!!,
        receivedPhotoEntity.lon!!,
        receivedPhotoEntity.lat!!,
        receivedPhotoEntity.uploadedOn!!
      )
    }

    fun toReceivedPhotos(receivedPhotoEntityList: List<ReceivedPhotoEntity>): MutableList<ReceivedPhoto> {
      return receivedPhotoEntityList.map { toReceivedPhoto(it) } as MutableList<ReceivedPhoto>
    }
  }

  object FromResponse {
    object ReceivedPhotos {
      fun toReceivedPhoto(receivedPhotosResponse: ReceivedPhotosResponse.ReceivedPhotoResponseData): ReceivedPhoto {
        return ReceivedPhoto(
          receivedPhotosResponse.photoId,
          receivedPhotosResponse.uploadedPhotoName,
          receivedPhotosResponse.receivedPhotoName,
          receivedPhotosResponse.lon,
          receivedPhotosResponse.lat,
          receivedPhotosResponse.uploadedOn
        )
      }

      fun toReceivedPhotos(receivedPhotosResponseList: List<ReceivedPhotosResponse.ReceivedPhotoResponseData>): List<ReceivedPhoto> {
        return receivedPhotosResponseList.map { toReceivedPhoto(it) }
      }

      fun toReceivedPhotoEntity(time: Long, receivedPhotosResponse: ReceivedPhotosResponse.ReceivedPhotoResponseData): ReceivedPhotoEntity {
        return ReceivedPhotoEntity.create(
          receivedPhotosResponse.photoId,
          receivedPhotosResponse.uploadedPhotoName,
          receivedPhotosResponse.receivedPhotoName,
          receivedPhotosResponse.lon,
          receivedPhotosResponse.lat,
          receivedPhotosResponse.uploadedOn,
          time
        )
      }

      fun toReceivedPhotoEntities(time: Long, receivedPhotosResponseList: List<ReceivedPhotosResponse.ReceivedPhotoResponseData>): List<ReceivedPhotoEntity> {
        return receivedPhotosResponseList.map { toReceivedPhotoEntity(time, it) }
      }
    }

    object GetReceivedPhotos {
      fun toReceivedPhoto(receivedPhotosResponse: ReceivedPhotosResponse.ReceivedPhotoResponseData): ReceivedPhoto {
        return ReceivedPhoto(
          receivedPhotosResponse.photoId,
          receivedPhotosResponse.uploadedPhotoName,
          receivedPhotosResponse.receivedPhotoName,
          receivedPhotosResponse.lon,
          receivedPhotosResponse.lat,
          receivedPhotosResponse.uploadedOn
        )
      }

      fun toReceivedPhotos(receivedPhotosResponseList: List<ReceivedPhotosResponse.ReceivedPhotoResponseData>): MutableList<ReceivedPhoto> {
        return receivedPhotosResponseList.map { toReceivedPhoto(it) } as MutableList<ReceivedPhoto>
      }

      fun toReceivedPhotoEntity(time: Long, receivedPhotosResponse: ReceivedPhotosResponse.ReceivedPhotoResponseData): ReceivedPhotoEntity {
        return ReceivedPhotoEntity.create(
          receivedPhotosResponse.photoId,
          receivedPhotosResponse.uploadedPhotoName,
          receivedPhotosResponse.receivedPhotoName,
          receivedPhotosResponse.lon,
          receivedPhotosResponse.lat,
          receivedPhotosResponse.uploadedOn,
          time
        )
      }

      fun toReceivedPhotoEntities(time: Long, receivedPhotosResponseList: List<ReceivedPhotosResponse.ReceivedPhotoResponseData>): List<ReceivedPhotoEntity> {
        return receivedPhotosResponseList.map { toReceivedPhotoEntity(time, it) }
      }
    }
  }

  object FromObject {
    fun toReceivedPhotoEntity(time: Long, receivedPhoto: ReceivedPhotosResponse.ReceivedPhotoResponseData): ReceivedPhotoEntity {
      return ReceivedPhotoEntity.create(
        receivedPhoto.uploadedPhotoName,
        receivedPhoto.receivedPhotoName,
        receivedPhoto.lon,
        receivedPhoto.lat,
        receivedPhoto.uploadedOn,
        time
      )
    }
  }
}