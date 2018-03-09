package com.kirakishou.photoexchange.tests

import java.io.File

/**
 * Created by kirakishou on 3/9/2018.
 */
abstract class AbstractTest {

    protected fun deleteDir(file: File) {
        val contents = file.listFiles()
        if (contents != null) {
            for (f in contents) {
                deleteDir(f)
            }
        }
        file.delete()
    }
}