package com.kirakishou.photoexchange.helper.util

import android.os.Looper
import okio.ByteString
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Created by kirakishou on 7/26/2017.
 */
object Utils {

    private val numericAlphabetic = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val random = SecureRandom()

    fun generateRandomString(len: Int, alphabet: String): String {
        val bytes = ByteArray(len)
        random.nextBytes(bytes)

        val sb = StringBuilder()
        val alphabetLen = alphabet.length

        for (i in 0 until len) {
            sb.append(alphabet[Math.abs(bytes[i] % alphabetLen)])
        }

        return sb.toString()
    }

    fun generateUserId(): String {
        return generateRandomString(64, numericAlphabetic) + "_photoexchange"
    }
}