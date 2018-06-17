package com.kirakishou.photoexchange.tests.repository

import android.arch.persistence.room.Room
import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.CachedPhotoIdEntity
import com.kirakishou.photoexchange.helper.database.repository.CachedPhotoIdRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CachedPhotoIdRepositoryTests {

    lateinit var appContext: Context
    lateinit var targetContext: Context
    lateinit var database: MyDatabase
    lateinit var cachedPhotoIdRepository: CachedPhotoIdRepository

    @Before
    fun init() {
        appContext = InstrumentationRegistry.getContext()
        targetContext = InstrumentationRegistry.getTargetContext()
        database = Room.inMemoryDatabaseBuilder(appContext, MyDatabase::class.java).build()

        cachedPhotoIdRepository = CachedPhotoIdRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun should_contain_same_photo_ids_with_different_types() {
        cachedPhotoIdRepository.insert(1, CachedPhotoIdEntity.PhotoType.UploadedPhoto)
        cachedPhotoIdRepository.insert(1, CachedPhotoIdEntity.PhotoType.ReceivedPhoto)
        cachedPhotoIdRepository.insert(1, CachedPhotoIdEntity.PhotoType.GalleryPhoto)

        assertEquals(3, cachedPhotoIdRepository.findAll().size)
    }

    @Test
    fun should_not_contain_duplicates_of_cached_photo_ids_with_same_photo_id_and_type() {
        cachedPhotoIdRepository.insert(1, CachedPhotoIdEntity.PhotoType.UploadedPhoto)
        cachedPhotoIdRepository.insert(1, CachedPhotoIdEntity.PhotoType.UploadedPhoto)
        cachedPhotoIdRepository.insert(1, CachedPhotoIdEntity.PhotoType.UploadedPhoto)
        cachedPhotoIdRepository.insert(1, CachedPhotoIdEntity.PhotoType.UploadedPhoto)

        cachedPhotoIdRepository.insert(1, CachedPhotoIdEntity.PhotoType.ReceivedPhoto)
        cachedPhotoIdRepository.insert(1, CachedPhotoIdEntity.PhotoType.ReceivedPhoto)
        cachedPhotoIdRepository.insert(1, CachedPhotoIdEntity.PhotoType.ReceivedPhoto)
        cachedPhotoIdRepository.insert(1, CachedPhotoIdEntity.PhotoType.ReceivedPhoto)
        cachedPhotoIdRepository.insert(1, CachedPhotoIdEntity.PhotoType.ReceivedPhoto)

        cachedPhotoIdRepository.insert(1, CachedPhotoIdEntity.PhotoType.GalleryPhoto)
        cachedPhotoIdRepository.insert(1, CachedPhotoIdEntity.PhotoType.GalleryPhoto)
        cachedPhotoIdRepository.insert(1, CachedPhotoIdEntity.PhotoType.GalleryPhoto)
        cachedPhotoIdRepository.insert(1, CachedPhotoIdEntity.PhotoType.GalleryPhoto)
        cachedPhotoIdRepository.insert(1, CachedPhotoIdEntity.PhotoType.GalleryPhoto)

        assertEquals(3, cachedPhotoIdRepository.findAll().size)
    }

    @Test
    fun should_insert_many_and_replace_duplicates() {
        val list = listOf<Long>(1, 1, 2, 2, 3, 3, 4, 4, 5, 6, 8, 8, 8, 324, 3, 52, 26, 1, 2, 3, 63)
        val goodIds = setOf<Long>(1, 2, 3, 4, 5, 6, 8, 324, 52, 26, 63)

        assertEquals(true, cachedPhotoIdRepository.insertMany(list, CachedPhotoIdEntity.PhotoType.UploadedPhoto))

        val foundIds = cachedPhotoIdRepository.findAll()

        assertEquals(11, foundIds.size)
        assertEquals(goodIds, foundIds.map { it.photoId }.toSet())
    }

    @Test
    fun should_return_cached_ids_sorted_descending() {
        val list = listOf<Long>(1, 1, 2, 2, 3, 3, 4, 4, 5, 6, 8, 8, 8, 324, 3, 52, 26, 1, 2, 3, 63)
        assertEquals(true, cachedPhotoIdRepository.insertMany(list, CachedPhotoIdEntity.PhotoType.UploadedPhoto))

        val foundIds = cachedPhotoIdRepository.findOnePageByType(700, CachedPhotoIdEntity.PhotoType.UploadedPhoto, 50)

        for ((id1, id2) in foundIds.windowed(2)) {
            assertEquals(true, id1 > id2)
        }
    }
}