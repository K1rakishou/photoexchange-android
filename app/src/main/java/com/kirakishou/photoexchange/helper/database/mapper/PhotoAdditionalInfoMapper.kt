package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.PhotoAdditionalInfoEntity
import com.kirakishou.photoexchange.mvrx.model.photo.PhotoAdditionalInfo
import net.response.data.PhotoAdditionalInfoResponseData

object PhotoAdditionalInfoMapper {

  object FromResponse {

    object ToEntity {

      fun toEntity(
        time: Long,
        photoAdditionalInfoResponseData: PhotoAdditionalInfoResponseData
      ): PhotoAdditionalInfoEntity {
        return PhotoAdditionalInfoEntity(
          photoAdditionalInfoResponseData.photoName,
          photoAdditionalInfoResponseData.isFavourited,
          photoAdditionalInfoResponseData.favouritesCount,
          photoAdditionalInfoResponseData.isReported,
          time
        )
      }

      fun toEntities(
        time: Long,
        photoAdditionalInfoResponseDataList: List<PhotoAdditionalInfoResponseData>
      ): List<PhotoAdditionalInfoEntity> {
        return photoAdditionalInfoResponseDataList.map { toEntity(time, it) }
      }

    }

    fun toPhotoAdditionalInfo(
      photoAdditionalInfoResponseData: PhotoAdditionalInfoResponseData
    ): PhotoAdditionalInfo {
      return PhotoAdditionalInfo(
        photoAdditionalInfoResponseData.photoName,
        photoAdditionalInfoResponseData.isFavourited,
        photoAdditionalInfoResponseData.favouritesCount,
        photoAdditionalInfoResponseData.isReported
      )
    }

    fun toPhotoAdditionalInfoList(
      photoAdditionalInfoResponseDataList: List<PhotoAdditionalInfoResponseData>
    ): List<PhotoAdditionalInfo> {
      return photoAdditionalInfoResponseDataList.map { toPhotoAdditionalInfo(it) }
    }
  }

  object FromEntity {

    fun toPhotoAdditionalInfo(
      photoAdditionalInfoEntity: PhotoAdditionalInfoEntity
    ): PhotoAdditionalInfo {
      return PhotoAdditionalInfo(
        photoAdditionalInfoEntity.photoName,
        photoAdditionalInfoEntity.isFavourited,
        photoAdditionalInfoEntity.favouritesCount,
        photoAdditionalInfoEntity.isReported
      )
    }

    fun toPhotoAdditionalInfoList(
      photoAdditionalInfoEntityList: List<PhotoAdditionalInfoEntity>
    ): List<PhotoAdditionalInfo> {
      return photoAdditionalInfoEntityList.map { toPhotoAdditionalInfo(it) }
    }

  }

  object ToEntity {

    fun toPhotoAdditionalInfoEntity(
      time: Long,
      photoAdditionalInfo: PhotoAdditionalInfo
    ): PhotoAdditionalInfoEntity {
      return PhotoAdditionalInfoEntity(
        photoAdditionalInfo.photoName,
        photoAdditionalInfo.isFavourited,
        photoAdditionalInfo.favouritesCount,
        photoAdditionalInfo.isReported,
        time
      )
    }

  }

}