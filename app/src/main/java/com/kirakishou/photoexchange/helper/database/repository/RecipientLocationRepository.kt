package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.dao.RecipientLocationDao
import com.kirakishou.photoexchange.helper.database.entity.RecipientLocationEntity
import com.kirakishou.photoexchange.helper.mapper.RecipientLocationMapper
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.model.other.RecipientLocation
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Created by kirakishou on 1/8/2018.
 */
class RecipientLocationRepository(
    private val database: MyDatabase,
    private val schedulers: SchedulerProvider,
    private val mapper: RecipientLocationMapper
) {
    private val recipientLocationDao: RecipientLocationDao by lazy { database.recipientLocationDao() }

    fun saveOne(recipientLocation: RecipientLocation): Single<Long> {
        val resultSingle = Single.fromCallable {
            val entity = RecipientLocationEntity.new(recipientLocation.photoName, recipientLocation.lon, recipientLocation.lat)
            return@fromCallable recipientLocationDao.saveOne(entity)
        }

        return resultSingle
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun saveMany(recipientLocationList: List<RecipientLocation>): Single<List<Long>> {
        val resultSingle = Single.fromCallable {
            val recipientLocationArray = recipientLocationList
                    .map { RecipientLocationEntity.fromRecipientLocation(it) }
                    .toTypedArray()

            return@fromCallable recipientLocationDao.saveMany(*recipientLocationArray)
        }

        return resultSingle
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
    }

    fun findMany(photoNameList: List<String>): Observable<List<RecipientLocation>> {
        return recipientLocationDao.findMany(photoNameList)
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .map(mapper::toRecipientLocations)
                .toObservable()
    }
}