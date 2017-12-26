package com.kirakishou.photoexchange.helper

import kotlinx.coroutines.experimental.Job
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
        val results = jobs.map { it.cancel() }
        val anyNotCanceled = results.any { !it }

        if (anyNotCanceled) {
            Timber.w("One or more jobs was already canceled before")
        }

        jobs.clear()
    }
}