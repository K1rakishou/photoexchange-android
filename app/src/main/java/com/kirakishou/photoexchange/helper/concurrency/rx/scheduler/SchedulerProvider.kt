package com.kirakishou.photoexchange.helper.concurrency.scheduler

import io.reactivex.Scheduler

/**
 * Created by kirakishou on 9/17/2017.
 */
interface SchedulerProvider {
    fun provideIo(): Scheduler
    fun provideMain(): Scheduler
}