package com.kirakishou.photoexchange.tests.viewmodel

import android.arch.persistence.room.Room
import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.tests.AbstractTest
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import java.io.File

/**
 * Created by kirakishou on 3/8/2018.
 */

@RunWith(AndroidJUnit4::class)
class TakePhotoActivityViewModelTests : AbstractTest() {

    lateinit var appContext: Context
    lateinit var targetContext: Context
    lateinit var database: MyDatabase
    lateinit var tempFilesDir: String

    @Before
    fun setup() {
        appContext = InstrumentationRegistry.getContext()
        targetContext = InstrumentationRegistry.getTargetContext()

        database = Room.inMemoryDatabaseBuilder(appContext, MyDatabase::class.java).build()
        tempFilesDir = targetContext.getDir("test_temp_files", Context.MODE_PRIVATE).absolutePath
    }

    @After
    fun tearDown() {
        //hack
        if (::database.isInitialized) {
            database.close()
        }

        if (::tempFilesDir.isInitialized) {
            deleteDir(File(tempFilesDir))
        }
    }
}

























