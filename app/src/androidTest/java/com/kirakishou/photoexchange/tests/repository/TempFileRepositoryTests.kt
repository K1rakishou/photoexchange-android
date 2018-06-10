package com.kirakishou.photoexchange.tests.repository

import android.arch.persistence.room.Room
import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.TempFileRepository
import com.kirakishou.photoexchange.helper.util.TimeUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class TempFileRepositoryTests {
    lateinit var appContext: Context
    lateinit var targetContext: Context
    lateinit var database: MyDatabase
    lateinit var timeUtils: TimeUtils
    lateinit var repository: TempFileRepository

    @Before
    fun init() {
        appContext = InstrumentationRegistry.getContext()
        targetContext = InstrumentationRegistry.getTargetContext()
        database = Room.inMemoryDatabaseBuilder(appContext, MyDatabase::class.java).build()
        timeUtils = Mockito.mock(TimeUtils::class.java)

        val tempFilesDir = targetContext.getDir("test_temp_files", Context.MODE_PRIVATE).absolutePath

        repository = TempFileRepository(tempFilesDir, database, timeUtils)
            .also { it.init() }
    }

    @After
    fun tearDown() {
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
}