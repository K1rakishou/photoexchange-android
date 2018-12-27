package com.kirakishou.photoexchange.helper.database.repository

import androidx.room.Room
import android.content.Context
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.concurrency.coroutines.TestDispatchers
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.util.*
import com.kirakishou.photoexchange.helper.database.source.local.TakenPhotosLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.TempFileLocalSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
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

  lateinit var takenPhotosLocalSource: TakenPhotosLocalSource
  lateinit var tempFilesLocalSource: TempFileLocalSource
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

    takenPhotosLocalSource = Mockito.spy(TakenPhotosLocalSource(database, timeUtils))
    tempFilesLocalSource = Mockito.spy(
      TempFileLocalSource(
        database,
        tempFilesDir,
        timeUtils,
        fileUtils
      )
    )

    takenPhotosRepository = TakenPhotosRepository(
      timeUtils,
      database,
      takenPhotosLocalSource,
      tempFilesLocalSource
    )
  }

  @After
  fun tearDown() {
    FileUtilsImpl().deleteAllFiles(File(tempFilesDir))

    database.close()
  }

  @Test
  fun should_save_taken_photo_should_be_able_to_find_photo_file_by_id() {
    runBlocking {
      val photoFile = tempFilesLocalSource.create()
      val takenPhoto = takenPhotosRepository.saveTakenPhoto(photoFile)
      val tempFile = takenPhotosRepository.findTempFile(takenPhoto!!.id)

      assertEquals(false, tempFile.isEmpty())
    }
  }

  @Test
  fun should_delete_photo_file_from_disk_when_could_not_save_temp_file_info_in_the_database() {
    runBlocking {
      val tempFile = tempFilesLocalSource.create()
      Mockito.`when`(timeUtils.getTimeFast()).thenReturn(444L)
      Mockito.`when`(tempFilesLocalSource.updateTakenPhotoId(tempFile, 1)).thenReturn(-1)

      val takenPhoto = takenPhotosRepository.saveTakenPhoto(tempFile)

      assertEquals(true, tempFile.fileExists())
      assertNull(takenPhoto)

      val deletedFiles = tempFilesLocalSource.findDeletedOld(Long.MAX_VALUE)
      assertEquals(1, deletedFiles.size)

      tempFilesLocalSource.deleteOld(Long.MAX_VALUE)
      assertEquals(true, tempFilesLocalSource.findDeletedOld(Long.MAX_VALUE).isEmpty())
      assertEquals(false, tempFile.asFile().exists())
    }
  }

  @Test
  fun should_create_files_on_disk() {
    runBlocking {
      tempFilesLocalSource.create()
      tempFilesLocalSource.create()
      tempFilesLocalSource.create()
      tempFilesLocalSource.create()

      assertEquals(4, tempFilesLocalSource.findAll().size)
    }
  }

  @Test
  fun should_update_entity_deleted_on_field_by_id() {
    runBlocking {
      Mockito.`when`(timeUtils.getTimeFast()).thenReturn(123L)
      tempFilesLocalSource.create()

      tempFilesLocalSource.markDeletedById(1)
      val markedEntity = tempFilesLocalSource.findAll().first()

      assertEquals(123L, markedEntity.deletedOn)
    }
  }

  @Test
  fun should_find_old_files() {
    val firstPhotoDeletionTime = 100L
    val secondPhotoDeletionTime = 110L
    val thirdPhotoDeletionTime = 160L
    val fourthPhotoDeletionTime = 190L
    val deleteBeforeTime = 150L

    Mockito.`when`(timeUtils.getTimeFast()).thenReturn(
      firstPhotoDeletionTime,
      secondPhotoDeletionTime,
      thirdPhotoDeletionTime,
      fourthPhotoDeletionTime
    )

    runBlocking {
      tempFilesLocalSource.create()
      tempFilesLocalSource.create()
      tempFilesLocalSource.create()
      tempFilesLocalSource.create()

      tempFilesLocalSource.markDeletedById(1)
      tempFilesLocalSource.markDeletedById(2)
      tempFilesLocalSource.markDeletedById(3)
      tempFilesLocalSource.markDeletedById(4)

      val oldFiles = tempFilesLocalSource.findDeletedOld(deleteBeforeTime)

      assertEquals(2, oldFiles.size)
      assertEquals(1, oldFiles[0].id!!)
      assertEquals(2, oldFiles[1].id!!)
    }
  }

  @Test
  fun should_delete_old_files() {
    val firstPhotoDeletionTime = 100L
    val secondPhotoDeletionTime = 110L
    val thirdPhotoDeletionTime = 160L
    val fourthPhotoDeletionTime = 190L
    val deleteBeforeTime = 150L

    Mockito.`when`(timeUtils.getTimeFast()).thenReturn(
      firstPhotoDeletionTime,
      secondPhotoDeletionTime,
      thirdPhotoDeletionTime,
      fourthPhotoDeletionTime
    )

    runBlocking {
      tempFilesLocalSource.create()
      tempFilesLocalSource.create()
      tempFilesLocalSource.create()
      tempFilesLocalSource.create()

      tempFilesLocalSource.markDeletedById(1)
      tempFilesLocalSource.markDeletedById(2)
      tempFilesLocalSource.markDeletedById(3)
      tempFilesLocalSource.markDeletedById(4)

      tempFilesLocalSource.deleteOld(deleteBeforeTime)

      assertEquals(2, tempFilesLocalSource.findAll().size)
    }
  }

  @Test
  fun should_remove_empty_temp_files() {
    runBlocking {
     tempFilesLocalSource.create()
     tempFilesLocalSource.create()
     tempFilesLocalSource.create()
     tempFilesLocalSource.create()

      assertEquals(4, tempFilesLocalSource.findAll().size)
      tempFilesLocalSource.deleteEmptyTempFiles()

      assertEquals(0, tempFilesLocalSource.findAll().size)
    }
  }

  @Test
  fun should_find_four_oldest_deleted_photos_and_delete_them() {
    Mockito.`when`(timeUtils.getTimeFast()).thenReturn(
      100, 110, 120, 130, 250, 260, 270, 280
    )

    runBlocking {
      tempFilesLocalSource.create()
      tempFilesLocalSource.create()
      tempFilesLocalSource.create()
      tempFilesLocalSource.create()
      tempFilesLocalSource.create()
      tempFilesLocalSource.create()
      tempFilesLocalSource.create()
      tempFilesLocalSource.create()

      tempFilesLocalSource.markDeletedById(1)
      tempFilesLocalSource.markDeletedById(2)
      tempFilesLocalSource.markDeletedById(3)
      tempFilesLocalSource.markDeletedById(4)
      tempFilesLocalSource.markDeletedById(5)
      tempFilesLocalSource.markDeletedById(6)
      tempFilesLocalSource.markDeletedById(7)
      tempFilesLocalSource.markDeletedById(8)

      val oldestDeletedFiles = tempFilesLocalSource.findOldest(4)

      assertEquals(4, oldestDeletedFiles.size)
      assertEquals(100, oldestDeletedFiles[0].deletedOn)
      assertEquals(110, oldestDeletedFiles[1].deletedOn)
      assertEquals(120, oldestDeletedFiles[2].deletedOn)
      assertEquals(130, oldestDeletedFiles[3].deletedOn)

      tempFilesLocalSource.deleteMany(oldestDeletedFiles)

      val restOfFiles = tempFilesLocalSource.findAll()
      assertEquals(250, restOfFiles[0].deletedOn)
      assertEquals(260, restOfFiles[1].deletedOn)
      assertEquals(270, restOfFiles[2].deletedOn)
      assertEquals(280, restOfFiles[3].deletedOn)
    }
  }

  @Test
  fun should_correctly_calculate_size_of_all_files_in_a_directory() {
    runBlocking {
      val tempFile1 = tempFilesLocalSource.create()
      tempFile1.asFile().writeText("1234567890")

      val tempFile2 = tempFilesLocalSource.create()
      tempFile2.asFile().writeText("12345678901234567890")

      val tempFile3 = tempFilesLocalSource.create()
      tempFile3.asFile().writeText("123456789012345678901234567890")

      val tempFile4 = tempFilesLocalSource.create()
      tempFile4.asFile().writeText("1234567890123456789012345678901234567890")

      assertEquals(100, FileUtilsImpl().calculateTotalDirectorySize(File(tempFilesDir)))
    }
  }
}























