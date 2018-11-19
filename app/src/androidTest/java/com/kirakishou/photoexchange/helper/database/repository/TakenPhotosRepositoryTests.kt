package com.kirakishou.photoexchange.helper.database.repository

import androidx.room.Room
import android.content.Context
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.concurrency.coroutines.TestDispatchers
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.util.*
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.io.File

/**
 * Created by kirakishou on 3/10/2018.
 */


@RunWith(AndroidJUnit4::class)
class TakenPhotosRepositoryTests {

  lateinit var appContext: Context
  lateinit var targetContext: Context
  lateinit var database: MyDatabase
  lateinit var timeUtils: TimeUtils
  lateinit var tempFilesDir: String
  lateinit var fileUtils: FileUtils
  lateinit var dispatchersProvider: DispatchersProvider

  lateinit var tempFilesRepository: TempFileRepository
  lateinit var takenPhotosRepository: TakenPhotosRepository

  @Before
  fun init() {
    appContext = InstrumentationRegistry.getContext()
    targetContext = InstrumentationRegistry.getTargetContext()
    database = Room.inMemoryDatabaseBuilder(appContext, MyDatabase::class.java).build()
    timeUtils = Mockito.spy(TimeUtils::class.java)
    fileUtils = Mockito.spy(FileUtils::class.java)
    tempFilesDir = targetContext.getDir("test_temp_files", Context.MODE_PRIVATE).absolutePath
    dispatchersProvider = TestDispatchers()

    tempFilesRepository = Mockito.spy(TempFileRepository(tempFilesDir, database, timeUtils, fileUtils, dispatchersProvider))
    takenPhotosRepository = TakenPhotosRepository(timeUtils, database, tempFilesRepository, dispatchersProvider)
  }

  @After
  fun tearDown() {
    FileUtilsImpl().deleteAllFiles(File(tempFilesDir))

    database.close()
  }

  @Test
  fun should_save_taken_photo_should_be_able_to_find_photo_file_by_id() {
    runBlocking {
      val photoFile = tempFilesRepository.create()
      val takenPhoto = takenPhotosRepository.saveTakenPhoto(photoFile)
      val tempFile = takenPhotosRepository.findTempFile(takenPhoto.id)

      assertEquals(false, tempFile.isEmpty())
    }
  }

  @Test
  fun should_delete_photo_file_from_disk_when_could_not_save_temp_file_info_in_the_database() {
    runBlocking {
      val tempFile = tempFilesRepository.create()
      Mockito.`when`(timeUtils.getTimeFast()).thenReturn(444L)
      Mockito.`when`(tempFilesRepository.updateTakenPhotoId(tempFile, 1)).thenReturn(-1)

      val takenPhoto = takenPhotosRepository.saveTakenPhoto(tempFile)

      assertEquals(true, tempFile.fileExists())
      assertEquals(true, takenPhoto.isEmpty())

      val deletedFiles = tempFilesRepository.findDeletedOld(Long.MAX_VALUE)
      assertEquals(1, deletedFiles.size)

      tempFilesRepository.deleteOld(Long.MAX_VALUE)
      assertEquals(true, tempFilesRepository.findDeletedOld(Long.MAX_VALUE).isEmpty())
      assertEquals(false, tempFile.asFile().exists())
    }
  }

  @Test
  fun should_update_location_for_all_photos_with_empty_location() {
    runBlocking {
      val tempFile1 = tempFilesRepository.create()
      takenPhotosRepository.saveTakenPhoto(tempFile1)

      val tempFile2 = tempFilesRepository.create()
      takenPhotosRepository.saveTakenPhoto(tempFile2)

      val tempFile3 = tempFilesRepository.create()
      takenPhotosRepository.saveTakenPhoto(tempFile3)

      val tempFile4 = tempFilesRepository.create()
      takenPhotosRepository.saveTakenPhoto(tempFile4)

      assertEquals(true, takenPhotosRepository.hasPhotosWithEmptyLocation())

      takenPhotosRepository.updateAllPhotosLocation(LonLat(11.1, 12.2))

      assertEquals(false, takenPhotosRepository.hasPhotosWithEmptyLocation())
    }
  }
}























