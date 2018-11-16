package com.kirakishou.photoexchange.helper.database.repository

import androidx.room.Room
import android.content.Context
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.helper.util.FileUtilsImpl
import com.kirakishou.photoexchange.helper.util.TimeUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.io.File

@RunWith(AndroidJUnit4::class)
class TempFileRepositoryTests {
  lateinit var appContext: Context
  lateinit var targetContext: Context
  lateinit var database: MyDatabase
  lateinit var timeUtils: TimeUtils
  lateinit var tempFilesDir: String
  lateinit var repository: TempFileRepository
  lateinit var fileUtils: FileUtils

  @Before
  fun init() {
    appContext = InstrumentationRegistry.getContext()
    targetContext = InstrumentationRegistry.getTargetContext()
    database = Room.inMemoryDatabaseBuilder(appContext, MyDatabase::class.java).build()
    timeUtils = Mockito.mock(TimeUtils::class.java)
    tempFilesDir = targetContext.getDir("test_temp_files", Context.MODE_PRIVATE).absolutePath
    fileUtils = Mockito.spy(FileUtils::class.java)

    repository = TempFileRepository(tempFilesDir, database, timeUtils, fileUtils)
      .also { it.init() }
  }

  @After
  fun tearDown() {
    FileUtilsImpl().deleteAllFiles(File(tempFilesDir))

    database.close()
  }

  @Test
  fun should_create_files_on_disk() {
    repository.create()
    repository.create()
    repository.create()
    repository.create()

    assertEquals(4, repository.findAll().size)
  }

  @Test
  fun should_update_entity_deleted_on_field_by_id() {
    Mockito.`when`(timeUtils.getTimeFast()).thenReturn(123L)
    repository.create()

    repository.markDeletedById(1)
    val markedEntity = repository.findAll().first()

    assertEquals(123L, markedEntity.deletedOn)
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

    repository.create()
    repository.create()
    repository.create()
    repository.create()

    repository.markDeletedById(1)
    repository.markDeletedById(2)
    repository.markDeletedById(3)
    repository.markDeletedById(4)

    val oldFiles = repository.findDeletedOld(deleteBeforeTime)

    assertEquals(2, oldFiles.size)
    assertEquals(1, oldFiles[0].id!!)
    assertEquals(2, oldFiles[1].id!!)
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

    repository.create()
    repository.create()
    repository.create()
    repository.create()

    repository.markDeletedById(1)
    repository.markDeletedById(2)
    repository.markDeletedById(3)
    repository.markDeletedById(4)

    repository.deleteOld(deleteBeforeTime)

    assertEquals(2, repository.findAll().size)
  }

  @Test
  fun should_remove_empty_temp_files() {
    repository.create()
    repository.create()
    repository.create()
    repository.create()

    assertEquals(4, repository.findAll().size)
    repository.deleteEmptyTempFiles()

    assertEquals(0, repository.findAll().size)
  }

  @Test
  fun should_find_four_oldest_deleted_photos_and_delete_them() {
    Mockito.`when`(timeUtils.getTimeFast()).thenReturn(
      100, 110, 120, 130, 250, 260, 270, 280
    )

    repository.create()
    repository.create()
    repository.create()
    repository.create()
    repository.create()
    repository.create()
    repository.create()
    repository.create()

    repository.markDeletedById(1)
    repository.markDeletedById(2)
    repository.markDeletedById(3)
    repository.markDeletedById(4)
    repository.markDeletedById(5)
    repository.markDeletedById(6)
    repository.markDeletedById(7)
    repository.markDeletedById(8)

    val oldestDeletedFiles = repository.findOldest(4)

    assertEquals(4, oldestDeletedFiles.size)
    assertEquals(100, oldestDeletedFiles[0].deletedOn)
    assertEquals(110, oldestDeletedFiles[1].deletedOn)
    assertEquals(120, oldestDeletedFiles[2].deletedOn)
    assertEquals(130, oldestDeletedFiles[3].deletedOn)

    repository.deleteMany(oldestDeletedFiles)

    val restOfFiles = repository.findAll()
    assertEquals(250, restOfFiles[0].deletedOn)
    assertEquals(260, restOfFiles[1].deletedOn)
    assertEquals(270, restOfFiles[2].deletedOn)
    assertEquals(280, restOfFiles[3].deletedOn)
  }

  @Test
  fun should_correctly_calculate_size_of_all_files_in_a_directory() {
    val tempFile1 = repository.create()
    tempFile1.asFile().writeText("1234567890")

    val tempFile2 = repository.create()
    tempFile2.asFile().writeText("12345678901234567890")

    val tempFile3 = repository.create()
    tempFile3.asFile().writeText("123456789012345678901234567890")

    val tempFile4 = repository.create()
    tempFile4.asFile().writeText("1234567890123456789012345678901234567890")

    assertEquals(100, FileUtilsImpl().calculateTotalDirectorySize(File(tempFilesDir)))
  }
}




















