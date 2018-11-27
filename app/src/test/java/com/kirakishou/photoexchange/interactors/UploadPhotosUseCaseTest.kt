package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.BitmapUtils
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import com.kirakishou.photoexchange.helper.exception.ApiErrorException
import com.kirakishou.photoexchange.helper.LonLat
import com.nhaarman.mockito_kotlin.any
import core.ErrorCode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.File
import java.io.IOException
import java.lang.IllegalStateException
import java.lang.RuntimeException

class UploadPhotosUseCaseTest {

  lateinit var database: MyDatabase
  lateinit var takenPhotosRepository: TakenPhotosRepository
  lateinit var uploadedPhotosRepository: UploadedPhotosRepository
  lateinit var apiClient: ApiClient
  lateinit var timeUtils: TimeUtils
  lateinit var bitmapUtils: BitmapUtils
  lateinit var fileUtils: FileUtils
  lateinit var gson: JsonConverter

  lateinit var uploadPhotosUseCase: UploadPhotosUseCase

  @Before
  fun setUp() {
    database = Mockito.mock(MyDatabase::class.java)
    timeUtils = Mockito.mock(TimeUtils::class.java)
    bitmapUtils = Mockito.mock(BitmapUtils::class.java)
    fileUtils = Mockito.mock(FileUtils::class.java)
    gson = Mockito.mock(JsonConverter::class.java)
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

  private suspend fun <T> readChannelUntil(channel: Channel<T>, block: suspend (T) -> Boolean): List<T> {
    val elements = mutableListOf<T>()

    for (element in channel) {
      elements += element

      if (!block(element)) {
        return elements
      }
    }

    throw RuntimeException("Should not happen")
  }

  @Test(expected = UploadPhotosUseCase.PhotoUploadingException.PhotoDoesNotExistOnDisk::class)
  fun `should throw PhotoDoesNotExistOnDisk when photoTempFile is null`() {
    val takenPhoto = TakenPhoto(1L, PhotoState.PHOTO_QUEUED_UP, true, "123", null)
    val userId = "user_id"

    runBlocking {
      val channel = Channel<UploadedPhotosFragmentEvent.PhotoUploadEvent>()
      uploadPhotosUseCase.uploadPhoto(takenPhoto, LonLat.empty(), userId, channel)

      Mockito.verify(takenPhotosRepository, Mockito.times(1))
        .updatePhotoState(takenPhoto.id, PhotoState.FAILED_TO_UPLOAD)
    }
  }

  @Test(expected = UploadPhotosUseCase.PhotoUploadingException.PhotoDoesNotExistOnDisk::class)
  fun `should return PhotoDoesNotExistOnDisk when photoTempFile does not exist on disk`() {
    val tempFile = Mockito.mock(File::class.java)
    Mockito.`when`(tempFile.exists()).thenReturn(false)

    val takenPhoto = TakenPhoto(1L, PhotoState.PHOTO_QUEUED_UP, true, "123", tempFile)
    val userId = "user_id"

    runBlocking {
      val channel = Channel<UploadedPhotosFragmentEvent.PhotoUploadEvent>()
      uploadPhotosUseCase.uploadPhoto(takenPhoto, LonLat.empty(), userId, channel)

      Mockito.verify(takenPhotosRepository, Mockito.times(1))
        .updatePhotoState(takenPhoto.id, PhotoState.FAILED_TO_UPLOAD)
    }
  }

  @Test(expected = UploadPhotosUseCase.PhotoUploadingException.CouldNotUpdatePhotoState::class)
  fun `should throw CouldNotUpdatePhotoState and delete created photoFile when could not update photo state in the database`() {
    val tempFile = Mockito.mock(File::class.java)
    val rotatedPhotoFileMock = Mockito.mock(File::class.java)
    val takenPhoto = TakenPhoto(1L, PhotoState.PHOTO_QUEUED_UP, true, "123", tempFile)
    val userId = "user_id"

    runBlocking {
      Mockito.`when`(tempFile.exists()).thenReturn(true)
      Mockito.`when`(fileUtils.createTempFile(Mockito.anyString(), Mockito.anyString())).thenReturn(rotatedPhotoFileMock)
      Mockito.`when`(takenPhotosRepository.updatePhotoState(takenPhoto.id, PhotoState.PHOTO_UPLOADING)).thenReturn(false)

      val channel = Channel<UploadedPhotosFragmentEvent.PhotoUploadEvent>()
      uploadPhotosUseCase.uploadPhoto(takenPhoto, LonLat.empty(), userId, channel)

      Mockito.verify(fileUtils, Mockito.times(1)).deleteFile(rotatedPhotoFileMock)
      Mockito.verify(takenPhotosRepository, Mockito.times(1))
        .updatePhotoState(takenPhoto.id, PhotoState.FAILED_TO_UPLOAD)
    }
  }

  @Test(expected = UploadPhotosUseCase.PhotoUploadingException.CouldNotRotatePhoto::class)
  fun `should return LocalCouldNotRotatePhoto and delete created photoFile when could not rotate photo bitmap`() {
    val tempFile = Mockito.mock(File::class.java)
    val rotatedPhotoFileMock = Mockito.mock(File::class.java)
    val takenPhoto = TakenPhoto(1L, PhotoState.PHOTO_QUEUED_UP, true, "123", tempFile)
    val userId = "user_id"

    runBlocking {
      Mockito.`when`(tempFile.exists()).thenReturn(true)
      Mockito.`when`(fileUtils.createTempFile(Mockito.anyString(), Mockito.anyString())).thenReturn(rotatedPhotoFileMock)
      Mockito.`when`(takenPhotosRepository.updatePhotoState(takenPhoto.id, PhotoState.PHOTO_UPLOADING)).thenReturn(true)
      Mockito.`when`(bitmapUtils.rotatePhoto(tempFile, rotatedPhotoFileMock)).thenReturn(false)

      val channel = Channel<UploadedPhotosFragmentEvent.PhotoUploadEvent>()
      uploadPhotosUseCase.uploadPhoto(takenPhoto, LonLat.empty(), userId, channel)

      Mockito.verify(fileUtils, Mockito.times(1)).deleteFile(rotatedPhotoFileMock)
      Mockito.verify(takenPhotosRepository, Mockito.times(1))
        .updatePhotoState(takenPhoto.id, PhotoState.FAILED_TO_UPLOAD)
    }
  }

  @Test(expected = UploadPhotosUseCase.PhotoUploadingException.DatabaseException::class)
  fun `should throw DatabaseException when database error has occurred`() {
    val tempFile = Mockito.mock(File::class.java)
    val rotatedPhotoFileMock = Mockito.mock(File::class.java)
    val takenPhoto = TakenPhoto(1L, PhotoState.PHOTO_QUEUED_UP, true, "123", tempFile)
    val userId = "user_id"
    val filePath = "file/path"
    val location = LonLat.empty()

    runBlocking {
      val channel = Channel<UploadedPhotosFragmentEvent.PhotoUploadEvent>(Channel.UNLIMITED)

      Mockito.doAnswer { invocation ->
        val photo = invocation.arguments[4] as TakenPhoto
        val ch = invocation.arguments[5] as Channel<UploadedPhotosFragmentEvent.PhotoUploadEvent>

        runBlocking {
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 0))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 15))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 30))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 45))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 60))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 65))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 72))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 80))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 90))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 93))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 99))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 100))
        }

        return@doAnswer UploadPhotosUseCase.UploadPhotoResult(1L, "photo_name")
      }.`when`(apiClient).uploadPhoto(filePath, location, userId, takenPhoto.isPublic, takenPhoto, channel)

      Mockito.`when`(tempFile.exists())
        .thenReturn(true)
      Mockito.`when`(fileUtils.createTempFile(Mockito.anyString(), Mockito.anyString())).thenReturn(rotatedPhotoFileMock)
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

      uploadPhotosUseCase.uploadPhoto(takenPhoto, location, userId, channel)

      val events = readChannelUntil(channel) { event ->
        if (event !is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress) {
          throw IllegalStateException("Bad event: ${event::class.java}")
        }

        if (event.progress >= 100) {
          return@readChannelUntil false
        }

        return@readChannelUntil true
      }

      assertEquals(13, events.size)

      Mockito.verify(fileUtils, Mockito.times(1)).deleteFile(rotatedPhotoFileMock)
      Mockito.verify(takenPhotosRepository, Mockito.times(1))
        .updatePhotoState(takenPhoto.id, PhotoState.FAILED_TO_UPLOAD)
    }
  }

  @Test(expected = UploadPhotosUseCase.PhotoUploadingException.ApiException::class)
  fun `should throw ApiException when server did not return ok`() {
    val tempFile = Mockito.mock(File::class.java)
    val rotatedPhotoFileMock = Mockito.mock(File::class.java)
    val takenPhoto = TakenPhoto(1L, PhotoState.PHOTO_QUEUED_UP, true, "123", tempFile)
    val userId = "user_id"
    val filePath = "file/path"
    val location = LonLat.empty()
    val time = 555L

    runBlocking {
      val channel = Channel<UploadedPhotosFragmentEvent.PhotoUploadEvent>(capacity = Channel.UNLIMITED)

      Mockito.doThrow(ApiErrorException(ErrorCode.DatabaseError))
        .`when`(apiClient).uploadPhoto(filePath, location, userId, takenPhoto.isPublic, takenPhoto, channel)
      Mockito.`when`(tempFile.exists())
        .thenReturn(true)
      Mockito.`when`(fileUtils.createTempFile(Mockito.anyString(), Mockito.anyString())).thenReturn(rotatedPhotoFileMock)
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

      uploadPhotosUseCase.uploadPhoto(takenPhoto, location, userId, channel)

      Mockito.verify(fileUtils, Mockito.times(1)).deleteFile(rotatedPhotoFileMock)
    }
  }

  @Test(expected = IOException::class)
  fun `should throw the exception when unknown exception has occurred while trying to upload a photo`() {
    val tempFile = Mockito.mock(File::class.java)
    val rotatedPhotoFileMock = Mockito.mock(File::class.java)
    val takenPhoto = TakenPhoto(1L, PhotoState.PHOTO_QUEUED_UP, true, "123", tempFile)
    val userId = "user_id"
    val filePath = "file/path"
    val location = LonLat.empty()
    val time = 555L

    runBlocking {
      val channel = Channel<UploadedPhotosFragmentEvent.PhotoUploadEvent>()

      Mockito.doAnswer { throw IOException("Something went wrong") }
        .`when`(apiClient).uploadPhoto(filePath, location, userId, takenPhoto.isPublic, takenPhoto, channel)
      Mockito.`when`(tempFile.exists())
        .thenReturn(true)
      Mockito.`when`(fileUtils.createTempFile(Mockito.anyString(), Mockito.anyString())).thenReturn(rotatedPhotoFileMock)
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

      uploadPhotosUseCase.uploadPhoto(takenPhoto, location, userId, channel)

      val events = readChannelUntil(channel) { event ->
        if (event !is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress) {
          throw IllegalStateException("Bad event: ${event::class.java}")
        }

        if (event.progress >= 100) {
          return@readChannelUntil false
        }

        return@readChannelUntil true
      }

      Mockito.verify(fileUtils, Mockito.times(1)).deleteFile(rotatedPhotoFileMock)
    }
  }

  @Test
  fun `should OnProgress event with 100 progress when server returned ok`() {
    val tempFile = Mockito.mock(File::class.java)
    val rotatedPhotoFileMock = Mockito.mock(File::class.java)
    val takenPhoto = TakenPhoto(1L, PhotoState.PHOTO_QUEUED_UP, true, "123", tempFile)
    val userId = "user_id"
    val filePath = "file/path"
    val location = LonLat.empty()
    val time = 555L

    runBlocking {
      val channel = Channel<UploadedPhotosFragmentEvent.PhotoUploadEvent>(Channel.UNLIMITED)

      Mockito.doAnswer { invocation ->
        val photo = invocation.arguments[4] as TakenPhoto
        val ch = invocation.arguments[5] as Channel<UploadedPhotosFragmentEvent.PhotoUploadEvent>

        runBlocking {
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 0))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 15))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 30))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 45))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 60))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 65))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 72))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 80))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 90))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 93))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 99))
          ch.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(photo, 100))
        }

        return@doAnswer UploadPhotosUseCase.UploadPhotoResult(1L, "photo_name")
      }.`when`(apiClient).uploadPhoto(filePath, location, userId, takenPhoto.isPublic, takenPhoto, channel)
      Mockito.`when`(tempFile.exists())
        .thenReturn(true)
      Mockito.`when`(fileUtils.createTempFile(Mockito.anyString(), Mockito.anyString())).thenReturn(rotatedPhotoFileMock)
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

      uploadPhotosUseCase.uploadPhoto(takenPhoto, location, userId, channel)

      val events = readChannelUntil(channel) { event ->
        if (event !is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress) {
          throw IllegalStateException("Bad event: ${event::class.java}")
        }

        if (event.progress >= 100) {
          return@readChannelUntil false
        }

        return@readChannelUntil true
      }

      assertEquals(12, events.size)

      val progressList = listOf(0, 15, 30, 45, 60, 65, 72, 80, 90, 93, 99, 100)

      for (index in 0 until 12) {
        assertEquals(true, events[index] is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress)
        assertEquals(progressList[index], (events[index] as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress).progress)
        assertEquals(takenPhoto, (events[index] as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress).photo)
      }

      Mockito.verify(fileUtils, Mockito.times(1)).deleteFile(rotatedPhotoFileMock)
    }
  }
}























