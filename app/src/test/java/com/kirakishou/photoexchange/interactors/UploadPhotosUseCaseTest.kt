package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.gson.MyGson
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.BitmapUtils
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.nhaarman.mockito_kotlin.any
import io.reactivex.Single
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.File
import java.io.IOException

class UploadPhotosUseCaseTest {

    lateinit var database: MyDatabase
    lateinit var takenPhotosRepository: TakenPhotosRepository
    lateinit var uploadedPhotosRepository: UploadedPhotosRepository
    lateinit var apiClient: ApiClient
    lateinit var timeUtils: TimeUtils
    lateinit var bitmapUtils: BitmapUtils
    lateinit var fileUtils: FileUtils
    lateinit var gson: MyGson

    lateinit var uploadPhotosUseCase: UploadPhotosUseCase

    @Before
    fun setUp() {
        database = Mockito.mock(MyDatabase::class.java)
        timeUtils = Mockito.mock(TimeUtils::class.java)
        bitmapUtils = Mockito.mock(BitmapUtils::class.java)
        fileUtils = Mockito.mock(FileUtils::class.java)
        gson = Mockito.mock(MyGson::class.java)
        apiClient = Mockito.mock(ApiClient::class.java)

        takenPhotosRepository = Mockito.mock(TakenPhotosRepository::class.java)
        uploadedPhotosRepository = Mockito.mock(UploadedPhotosRepository::class.java)

        uploadPhotosUseCase = UploadPhotosUseCase(
            database,
            takenPhotosRepository,
            uploadedPhotosRepository,
            apiClient,
            timeUtils,
            bitmapUtils,
            fileUtils
        )
    }

    @After
    fun tearDown() {

    }

    @Test
    fun `should return LocalNoPhotoFileOnDisk when photoTempFile is null`() {
        val takenPhoto = TakenPhoto(1L, PhotoState.PHOTO_QUEUED_UP, true, "123", null)
        val userId = "user_id"

        uploadPhotosUseCase.uploadPhoto(takenPhoto, userId, LonLat.empty())
            .test()
            .assertNoErrors()
            .assertValueAt(0) { value ->
                value is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError
            }
            .assertValueAt(0) { value ->
                (value as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError).errorCode is ErrorCode.UploadPhotoErrors.LocalNoPhotoFileOnDisk
            }
            .assertTerminated()
            .awaitTerminalEvent()

        Mockito.verify(takenPhotosRepository, Mockito.times(1))
            .updatePhotoState(takenPhoto.id, PhotoState.FAILED_TO_UPLOAD)
    }

    @Test
    fun `should return LocalNoPhotoFileOnDisk when photoTempFile does not exist on disk`() {
        val tempFile = Mockito.mock(File::class.java)
        Mockito.`when`(tempFile.exists()).thenReturn(false)

        val takenPhoto = TakenPhoto(1L, PhotoState.PHOTO_QUEUED_UP, true, "123", tempFile)
        val userId = "user_id"

        uploadPhotosUseCase.uploadPhoto(takenPhoto, userId, LonLat.empty())
            .test()
            .assertNoErrors()
            .assertValueAt(0) { value ->
                value is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError
            }
            .assertValueAt(0) { value ->
                (value as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError).errorCode is ErrorCode.UploadPhotoErrors.LocalNoPhotoFileOnDisk
            }
            .assertTerminated()
            .awaitTerminalEvent()

        Mockito.verify(takenPhotosRepository, Mockito.times(1))
            .updatePhotoState(takenPhoto.id, PhotoState.FAILED_TO_UPLOAD)
    }

    @Test
    fun `should return LocalCouldNotUpdatePhotoState and delete created photoFile when could not update photo state in the database`() {
        val tempFile = Mockito.mock(File::class.java)
        val rotatedPhotoFileMock = Mockito.mock(File::class.java)
        val takenPhoto = TakenPhoto(1L, PhotoState.PHOTO_QUEUED_UP, true, "123", tempFile)
        val userId = "user_id"

        Mockito.`when`(tempFile.exists()).thenReturn(true)
        Mockito.`when`(fileUtils.createTempFile(Mockito.anyString(), Mockito.anyString())).thenReturn(rotatedPhotoFileMock)
        Mockito.`when`(takenPhotosRepository.updatePhotoState(takenPhoto.id, PhotoState.PHOTO_UPLOADING)).thenReturn(false)

        uploadPhotosUseCase.uploadPhoto(takenPhoto, userId, LonLat.empty())
            .test()
            .assertNoErrors()
            .assertValueAt(0) { value ->
                value is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError
            }
            .assertValueAt(0) { value ->
                (value as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError).errorCode is ErrorCode.UploadPhotoErrors.LocalCouldNotUpdatePhotoState
            }
            .assertTerminated()
            .awaitTerminalEvent()

        Mockito.verify(fileUtils, Mockito.times(1)).deleteFile(rotatedPhotoFileMock)
        Mockito.verify(takenPhotosRepository, Mockito.times(1))
            .updatePhotoState(takenPhoto.id, PhotoState.FAILED_TO_UPLOAD)
    }

    @Test
    fun `should return LocalCouldNotRotatePhoto and delete created photoFile when could not rotate photo bitmap`() {
        val tempFile = Mockito.mock(File::class.java)
        val rotatedPhotoFileMock = Mockito.mock(File::class.java)
        val takenPhoto = TakenPhoto(1L, PhotoState.PHOTO_QUEUED_UP, true, "123", tempFile)
        val userId = "user_id"

        Mockito.`when`(tempFile.exists()).thenReturn(true)
        Mockito.`when`(fileUtils.createTempFile(Mockito.anyString(), Mockito.anyString())).thenReturn(rotatedPhotoFileMock)
        Mockito.`when`(takenPhotosRepository.updatePhotoState(takenPhoto.id, PhotoState.PHOTO_UPLOADING)).thenReturn(true)
        Mockito.`when`(bitmapUtils.rotatePhoto(tempFile, rotatedPhotoFileMock)).thenReturn(false)

        uploadPhotosUseCase.uploadPhoto(takenPhoto, userId, LonLat.empty())
            .test()
            .assertNoErrors()
            .assertValueAt(0) { value ->
                value is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError
            }
            .assertValueAt(0) { value ->
                (value as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError).errorCode is ErrorCode.UploadPhotoErrors.LocalCouldNotRotatePhoto
            }
            .assertTerminated()
            .awaitTerminalEvent()

        Mockito.verify(fileUtils, Mockito.times(1)).deleteFile(rotatedPhotoFileMock)
        Mockito.verify(takenPhotosRepository, Mockito.times(1))
            .updatePhotoState(takenPhoto.id, PhotoState.FAILED_TO_UPLOAD)
    }

    @Test
    fun `should return LocalDatabaseError when database error has occurred`() {
        val tempFile = Mockito.mock(File::class.java)
        val rotatedPhotoFileMock = Mockito.mock(File::class.java)
        val takenPhoto = TakenPhoto(1L, PhotoState.PHOTO_QUEUED_UP, true, "123", tempFile)
        val userId = "user_id"
        val filePath = "file/path"
        val location = LonLat.empty()

        Mockito.doAnswer { invocation ->
            val callback = invocation.arguments[4] as UploadPhotosUseCase.PhotoUploadProgressCallback

            callback.onProgress(0)
            callback.onProgress(15)
            callback.onProgress(30)
            callback.onProgress(45)
            callback.onProgress(60)
            callback.onProgress(65)
            callback.onProgress(72)
            callback.onProgress(80)
            callback.onProgress(90)
            callback.onProgress(93)
            callback.onProgress(99)
            callback.onProgress(100)

            return@doAnswer Single.just(UploadPhotoResponse.success(1L, "photo_name"))
        }
            .`when`(apiClient).uploadPhoto(Mockito.anyString(), any(), Mockito.anyString(), Mockito.anyBoolean(), any())
        Mockito.`when`(tempFile.exists())
            .thenReturn(true)
        Mockito.`when`(fileUtils.createTempFile(Mockito.anyString(), Mockito.anyString())).
            thenReturn(rotatedPhotoFileMock)
        Mockito.`when`(takenPhotosRepository.updatePhotoState(takenPhoto.id, PhotoState.PHOTO_UPLOADING))
            .thenReturn(true)
        Mockito.`when`(bitmapUtils.rotatePhoto(tempFile, rotatedPhotoFileMock))
            .thenReturn(true)
        Mockito.`when`(rotatedPhotoFileMock.absolutePath)
            .thenReturn(filePath)
        Mockito.`when`(gson.toJson(any()))
            .thenReturn("{ \"test\": \"123\" }")
        Mockito.`when`(database.transactional(any()))
            .thenReturn(false)

        val events = uploadPhotosUseCase.uploadPhoto(takenPhoto, userId, location)
            .test()
            .assertNoErrors()
            .assertTerminated()
            .values()

        assertEquals(13, events.size)

        val lastEvent = events.last()
        assertEquals(true, lastEvent is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError)
        assertEquals(true, (lastEvent as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError).errorCode is ErrorCode.UploadPhotoErrors.LocalDatabaseError)

        Mockito.verify(fileUtils, Mockito.times(1)).deleteFile(rotatedPhotoFileMock)
        Mockito.verify(takenPhotosRepository, Mockito.times(1))
            .updatePhotoState(takenPhoto.id, PhotoState.FAILED_TO_UPLOAD)
    }

    @Test
    fun `should return errorCode when server did not return ok`() {
        val tempFile = Mockito.mock(File::class.java)
        val rotatedPhotoFileMock = Mockito.mock(File::class.java)
        val takenPhoto = TakenPhoto(1L, PhotoState.PHOTO_QUEUED_UP, true, "123", tempFile)
        val userId = "user_id"
        val filePath = "file/path"
        val location = LonLat.empty()
        val time = 555L

        Mockito.doAnswer { invocation ->
            val callback = invocation.arguments[4] as UploadPhotosUseCase.PhotoUploadProgressCallback

            callback.onProgress(0)
            callback.onProgress(15)
            callback.onProgress(30)
            callback.onProgress(45)
            callback.onProgress(60)
            callback.onProgress(65)
            callback.onProgress(72)
            callback.onProgress(80)
            callback.onProgress(90)
            callback.onProgress(93)
            callback.onProgress(99)
            callback.onProgress(100)

            return@doAnswer Single.just(UploadPhotoResponse.error(ErrorCode.UploadPhotoErrors.DatabaseError()))
        }
            .`when`(apiClient).uploadPhoto(Mockito.anyString(), any(), Mockito.anyString(), Mockito.anyBoolean(), any())
        Mockito.`when`(tempFile.exists())
            .thenReturn(true)
        Mockito.`when`(fileUtils.createTempFile(Mockito.anyString(), Mockito.anyString())).
            thenReturn(rotatedPhotoFileMock)
        Mockito.`when`(takenPhotosRepository.updatePhotoState(takenPhoto.id, PhotoState.PHOTO_UPLOADING))
            .thenReturn(true)
        Mockito.`when`(bitmapUtils.rotatePhoto(tempFile, rotatedPhotoFileMock))
            .thenReturn(true)
        Mockito.`when`(rotatedPhotoFileMock.absolutePath)
            .thenReturn(filePath)
        Mockito.`when`(gson.toJson(any()))
            .thenReturn("{ \"test\": \"123\" }")
        Mockito.`when`(database.transactional(any()))
            .thenReturn(true)
        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(time)

        val events = uploadPhotosUseCase.uploadPhoto(takenPhoto, userId, location)
            .test()
            .assertNoErrors()
            .assertTerminated()
            .values()

        assertEquals(13, events.size)

        val progressList = listOf(0, 15, 30, 45, 60, 65, 72, 80, 90, 93, 99, 100)

        for (index in 0 until 12) {
            assertEquals(true, events[index] is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress)
            assertEquals(progressList[index], (events[index] as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress).progress)
            assertEquals(takenPhoto, (events[index] as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress).photo)
        }

        val lastEvent = events.last()
        assertEquals(true, lastEvent is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError)
        assertEquals(true, (lastEvent as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError).errorCode is ErrorCode.UploadPhotoErrors.DatabaseError)

        Mockito.verify(fileUtils, Mockito.times(1)).deleteFile(rotatedPhotoFileMock)
    }

    @Test
    fun `should return OnUnknownError when unknown error has occurred while trying to upload a photo`() {
        val tempFile = Mockito.mock(File::class.java)
        val rotatedPhotoFileMock = Mockito.mock(File::class.java)
        val takenPhoto = TakenPhoto(1L, PhotoState.PHOTO_QUEUED_UP, true, "123", tempFile)
        val userId = "user_id"
        val filePath = "file/path"
        val location = LonLat.empty()
        val time = 555L

        Mockito.doAnswer { _ -> throw IOException("Something went wrong") }
            .`when`(apiClient).uploadPhoto(Mockito.anyString(), any(), Mockito.anyString(), Mockito.anyBoolean(), any())
        Mockito.`when`(tempFile.exists())
            .thenReturn(true)
        Mockito.`when`(fileUtils.createTempFile(Mockito.anyString(), Mockito.anyString())).
            thenReturn(rotatedPhotoFileMock)
        Mockito.`when`(takenPhotosRepository.updatePhotoState(takenPhoto.id, PhotoState.PHOTO_UPLOADING))
            .thenReturn(true)
        Mockito.`when`(bitmapUtils.rotatePhoto(tempFile, rotatedPhotoFileMock))
            .thenReturn(true)
        Mockito.`when`(rotatedPhotoFileMock.absolutePath)
            .thenReturn(filePath)
        Mockito.`when`(gson.toJson(any()))
            .thenReturn("{ \"test\": \"123\" }")
        Mockito.`when`(database.transactional(any()))
            .thenReturn(true)
        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(time)

        uploadPhotosUseCase.uploadPhoto(takenPhoto, userId, location)
            .test()
            .assertNoErrors()
            .assertValueAt(0) { value ->
                value is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUnknownError
            }
            .assertValueAt(0) { value ->
                (value as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUnknownError).error is IOException
            }
            .assertTerminated()

        Mockito.verify(fileUtils, Mockito.times(1)).deleteFile(rotatedPhotoFileMock)
    }

    @Test
    fun `should return OnUploaded when server returned ok`() {
        val tempFile = Mockito.mock(File::class.java)
        val rotatedPhotoFileMock = Mockito.mock(File::class.java)
        val takenPhoto = TakenPhoto(1L, PhotoState.PHOTO_QUEUED_UP, true, "123", tempFile)
        val userId = "user_id"
        val filePath = "file/path"
        val location = LonLat.empty()
        val time = 555L

        Mockito.doAnswer { invocation ->
            val callback = invocation.arguments[4] as UploadPhotosUseCase.PhotoUploadProgressCallback

            callback.onProgress(0)
            callback.onProgress(15)
            callback.onProgress(30)
            callback.onProgress(45)
            callback.onProgress(60)
            callback.onProgress(65)
            callback.onProgress(72)
            callback.onProgress(80)
            callback.onProgress(90)
            callback.onProgress(93)
            callback.onProgress(99)
            callback.onProgress(100)

            return@doAnswer Single.just(UploadPhotoResponse.success(1L, "photo_name"))
        }
            .`when`(apiClient).uploadPhoto(Mockito.anyString(), any(), Mockito.anyString(), Mockito.anyBoolean(), any())
        Mockito.`when`(tempFile.exists())
            .thenReturn(true)
        Mockito.`when`(fileUtils.createTempFile(Mockito.anyString(), Mockito.anyString())).
            thenReturn(rotatedPhotoFileMock)
        Mockito.`when`(takenPhotosRepository.updatePhotoState(takenPhoto.id, PhotoState.PHOTO_UPLOADING))
            .thenReturn(true)
        Mockito.`when`(bitmapUtils.rotatePhoto(tempFile, rotatedPhotoFileMock))
            .thenReturn(true)
        Mockito.`when`(rotatedPhotoFileMock.absolutePath)
            .thenReturn(filePath)
        Mockito.`when`(gson.toJson(any()))
            .thenReturn("{ \"test\": \"123\" }")
        Mockito.`when`(database.transactional(any()))
            .thenReturn(true)
        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(time)

        val events = uploadPhotosUseCase.uploadPhoto(takenPhoto, userId, location)
            .test()
            .assertNoErrors()
            .assertTerminated()
            .values()

        assertEquals(12, events.size)

        val progressList = listOf(0, 15, 30, 45, 60, 65, 72, 80, 90, 93, 99, 100)

        for (index in 0 until 12) {
            assertEquals(true, events[index] is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress)
            assertEquals(progressList[index], (events[index] as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress).progress)
            assertEquals(takenPhoto, (events[index] as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress).photo)
        }

        Mockito.verify(fileUtils, Mockito.times(1)).deleteFile(rotatedPhotoFileMock)
    }
}























