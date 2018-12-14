package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto
import net.response.data.ReceivedPhotoResponseData

object ReceivedPhotosMapper {

  object FromEntity {
    fun toReceivedPhoto(receivedPhotoEntity: ReceivedPhotoEntity): ReceivedPhoto {
      return ReceivedPhoto(
        receivedPhotoEntity.uploadedPhotoName,
        receivedPhotoEntity.receivedPhotoName!!,
        LonLat(
          receivedPhotoEntity.lon!!,
          receivedPhotoEntity.lat!!
        ),
        receivedPhotoEntity.uploadedOn!!
      )
    }

    fun toReceivedPhotos(receivedPhotoEntityList: List<ReceivedPhotoEntity>): MutableList<ReceivedPhoto> {
      return receivedPhotoEntityList.map { toReceivedPhoto(it) } as MutableList<ReceivedPhoto>
    }
  }

  object FromResponse {
    object ReceivedPhotos {
      fun toReceivedPhoto(receivedPhotosResponse: ReceivedPhotoResponseData): ReceivedPhoto {
        return ReceivedPhoto(
          receivedPhotosResponse.uploadedPhotoName,
          receivedPhotosResponse.receivedPhotoName,
          LonLat(
            receivedPhotosResponse.lon,
            receivedPhotosResponse.lat
          ),
          receivedPhotosResponse.uploadedOn
        )
      }

      fun toReceivedPhotos(receivedPhotosResponseList: List<ReceivedPhotoResponseData>): List<ReceivedPhoto> {
        return receivedPhotosResponseList.map { toReceivedPhoto(it) }
      }

      fun toReceivedPhotoEntity(time: Long, receivedPhotosResponse: ReceivedPhotoResponseData): ReceivedPhotoEntity {
        return ReceivedPhotoEntity.create(
          receivedPhotosResponse.uploadedPhotoName,
          receivedPhotosResponse.receivedPhotoName,
          receivedPhotosResponse.lon,
          receivedPhotosResponse.lat,
          receivedPhotosResponse.uploadedOn,
          time
        )
      }

      fun toReceivedPhotoEntities(time: Long, receivedPhotosResponseList: List<ReceivedPhotoResponseData>): List<ReceivedPhotoEntity> {
        return receivedPhotosResponseList.map { toReceivedPhotoEntity(time, it) }
      }
    }

    object GetReceivedPhotos {
      fun toReceivedPhoto(receivedPhotosResponse: ReceivedPhotoResponseData): ReceivedPhoto {
        return ReceivedPhoto(
          receivedPhotosResponse.uploadedPhotoName,
          receivedPhotosResponse.receivedPhotoName,
          LonLat(
            receivedPhotosResponse.lon,
            receivedPhotosResponse.lat
          ),
          receivedPhotosResponse.uploadedOn
        )
      }

      fun toReceivedPhotos(receivedPhotosResponseList: List<ReceivedPhotoResponseData>): MutableList<ReceivedPhoto> {
        return receivedPhotosResponseList.map { toReceivedPhoto(it) } as MutableList<ReceivedPhoto>
      }

      fun toReceivedPhotoEntity(time: Long, receivedPhotosResponse: ReceivedPhotoResponseData): ReceivedPhotoEntity {
        return ReceivedPhotoEntity.create(
          receivedPhotosResponse.uploadedPhotoName,
          receivedPhotosResponse.receivedPhotoName,
          receivedPhotosResponse.lon,
          receivedPhotosResponse.lat,
          receivedPhotosResponse.uploadedOn,
          time
        )
      }

      fun toReceivedPhotoEntities(time: Long, receivedPhotosResponseList: List<ReceivedPhotoResponseData>): List<ReceivedPhotoEntity> {
        return receivedPhotosResponseList.map { toReceivedPhotoEntity(time, it) }
      }
    }
  }

  object FromObject {
    fun toReceivedPhotoEntity(time: Long, receivedPhoto: ReceivedPhotoResponseData): ReceivedPhotoEntity {
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