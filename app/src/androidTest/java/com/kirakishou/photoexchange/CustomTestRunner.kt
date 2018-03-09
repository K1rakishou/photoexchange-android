package com.kirakishou.photoexchange

import android.app.Application
import android.app.Instrumentation
import android.content.Context
import android.support.test.runner.AndroidJUnitRunner

/**
 * Created by kirakishou on 3/8/2018.
 */
class CustomTestRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        return Instrumentation.newApplication(MockApplication::class.java, context)
    }
}