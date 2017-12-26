package com.kirakishou.photoexchange.helper

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.cancelAndJoin
import kotlinx.coroutines.experimental.runBlocking
import timber.log.Timber

/**
 * Created by kirakishou on 11/21/2017.
 */
class CompositeJob {

    private val jobs = ArrayList<Job>()

    operator fun plusAssign(job: Job) {
        jobs += job
    }

    fun cancelAll() {
        runBlocking {
            jobs.map { it.cancelAndJoin() }
            jobs.clear()
        }
    }
}