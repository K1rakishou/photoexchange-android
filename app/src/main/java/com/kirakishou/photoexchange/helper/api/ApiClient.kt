package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.mwvm.model.dto.PhotoToBeUploaded
import com.kirakishou.photoexchange.mwvm.model.net.response.GetUserLocationResponse
import com.kirakishou.photoexchange.mwvm.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.mwvm.model.net.response.StatusResponse
import com.kirakishou.photoexchange.mwvm.model.net.response.UploadPhotoResponse
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.Path

/**
 * Created by kirakishou on 7/23/2017.
 */
interface ApiClient {
    fun uploadPhoto(info: PhotoToBeUploaded): Single<UploadPhotoResponse>
    fun findPhotoAnswer(userId: String): Single<PhotoAnswerResponse>
    fun markPhotoAsReceived(photoId: Long, userId: String): Single<StatusResponse>
    fun getPhotoRecipientsLocations(userId: String, photoIds: String): Single<GetUserLocationResponse>
}