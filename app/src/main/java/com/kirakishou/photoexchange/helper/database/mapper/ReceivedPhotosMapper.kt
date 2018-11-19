package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import net.response.GetReceivedPhotosResponse
import net.response.ReceivePhotosResponse

object ReceivedPhotosMapper {

  object FromEntity {
    fun toReceivedPhoto(receivedPhotoEntity: ReceivedPhotoEntity): ReceivedPhoto {
      return ReceivedPhoto(
        receivedPhotoEntity.id!!,
        receivedPhotoEntity.uploadedPhotoName!!,
        receivedPhotoEntity.receivedPhotoName!!,
        receivedPhotoEntity.lon!!,
        receivedPhotoEntity.lat!!
      )
    }

    fun toReceivedPhotos(receivedPhotoEntityList: List<ReceivedPhotoEntity>): MutableList<ReceivedPhoto> {
      return receivedPhotoEntityList.map { toReceivedPhoto(it) } as MutableList<ReceivedPhoto>
    }
  }

  object FromResponse {
    object ReceivedPhotos {
      fun toReceivedPhoto(receivedPhotosResponse: ReceivePhotosResponse.ReceivedPhotoResponseData): ReceivedPhoto {
        return ReceivedPhoto(
          receivedPhotosResponse.photoId,
          receivedPhotosResponse.uploadedPhotoName,
          receivedPhotosResponse.receivedPhotoName,
          receivedPhotosResponse.lon,
          receivedPhotosResponse.lat
        )
      }

      fun toReceivedPhotos(receivedPhotosResponseList: List<ReceivePhotosResponse.ReceivedPhotoResponseData>): List<ReceivedPhoto> {
        return receivedPhotosResponseList.map { toReceivedPhoto(it) }
      }

      fun toReceivedPhotoEntity(time: Long, receivedPhotosResponse: ReceivePhotosResponse.ReceivedPhotoResponseData): ReceivedPhotoEntity {
        return ReceivedPhotoEntity.create(
          receivedPhotosResponse.photoId,
          receivedPhotosResponse.uploadedPhotoName,
          receivedPhotosResponse.receivedPhotoName,
          receivedPhotosResponse.lon,
          receivedPhotosResponse.lat,
          time
        )
      }

      fun toReceivedPhotoEntities(time: Long, receivedPhotosResponseList: List<ReceivePhotosResponse.ReceivedPhotoResponseData>): List<ReceivedPhotoEntity> {
        return receivedPhotosResponseList.map { toReceivedPhotoEntity(time, it) }
      }
    }

    object GetReceivedPhotos {
      fun toReceivedPhoto(receivedPhotosResponse: GetReceivedPhotosResponse.ReceivedPhotoResponseData): ReceivedPhoto {
        return ReceivedPhoto(
          receivedPhotosResponse.photoId,
          receivedPhotosResponse.uploadedPhotoName,
          receivedPhotosResponse.receivedPhotoName,
          receivedPhotosResponse.receiverLon,
          receivedPhotosResponse.receiverLat
        )
      }

      fun toReceivedPhotos(receivedPhotosResponseList: List<GetReceivedPhotosResponse.ReceivedPhotoResponseData>): MutableList<ReceivedPhoto> {
        return receivedPhotosResponseList.map { toReceivedPhoto(it) } as MutableList<ReceivedPhoto>
      }

      fun toReceivedPhotoEntity(time: Long, receivedPhotosResponse: GetReceivedPhotosResponse.ReceivedPhotoResponseData): ReceivedPhotoEntity {
        return ReceivedPhotoEntity.create(
          receivedPhotosResponse.photoId,
          receivedPhotosResponse.uploadedPhotoName,
          receivedPhotosResponse.receivedPhotoName,
          receivedPhotosResponse.receiverLon,
          receivedPhotosResponse.receiverLat,
          time
        )
      }

      fun toReceivedPhotoEntities(time: Long, receivedPhotosResponseList: List<GetReceivedPhotosResponse.ReceivedPhotoResponseData>): List<ReceivedPhotoEntity> {
        return receivedPhotosResponseList.map { toReceivedPhotoEntity(time, it) }
      }
    }
  }

  object FromObject {
    fun toReceivedPhotoEntity(time: Long, receivedPhotos: GetReceivedPhotosResponse.ReceivedPhotoResponseData): ReceivedPhotoEntity {
      return ReceivedPhotoEntity.create(
        receivedPhotos.uploadedPhotoName,
        receivedPhotos.receivedPhotoName,
        receivedPhotos.receiverLon,
        receivedPhotos.receiverLat,
        time
      )
    }
  }
}