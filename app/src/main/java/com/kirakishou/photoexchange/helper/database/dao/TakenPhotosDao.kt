package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import com.kirakishou.photoexchange.mwvm.model.other.PhotoState
import io.reactivex.Single

/**
 * Created by kirakishou on 11/10/2017.
 */

@Dao
interface TakenPhotosDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveOne(takenPhotoEntity: TakenPhotoEntity): Long

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME} WHERE id = :arg0")
    fun findOne(id: Long): Single<TakenPhotoEntity>

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME} WHERE photo_state = \'uploaded\' " +
            "ORDER BY created_on DESC " +
            "LIMIT :arg1 OFFSET :arg0")
    fun findPage(page: Int, count: Int): Single<List<TakenPhotoEntity>>

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME} WHERE photo_state = \'queued_up\' ORDER BY created_on DESC ")
    fun findAllQueuedUp(): Single<List<TakenPhotoEntity>>

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME} ORDER BY created_on DESC ")
    fun findAll(): Single<List<TakenPhotoEntity>>

    @Query("SELECT COUNT(id) FROM ${TakenPhotoEntity.TABLE_NAME} WHERE photo_state = \'uploaded\'")
    fun countAll(): Single<Long>

    @Query("UPDATE ${TakenPhotoEntity.TABLE_NAME} SET photo_state = :arg0 WHERE id = :arg1")
    fun updateSetState(photoState: String, id: Long)

    @Query("UPDATE ${TakenPhotoEntity.TABLE_NAME} SET photo_name = :arg0 WHERE id = :arg1")
    fun updateSetPhotoName(photoName: String, id: Long)

    @Query("DELETE FROM ${TakenPhotoEntity.TABLE_NAME} WHERE id = :arg0")
    fun deleteOne(id: Long): Int

    /*@Query("DELETE FROM ${TakenPhotoEntity.TABLE_NAME} WHERE is_uploading = ${MyDatabase.SQLITE_FALSE} AND uploaded = ${MyDatabase.SQLITE_FALSE}")
    fun deleteAll(): Int*/
}


























