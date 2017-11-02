package com.kirakishou.photoexchange.service

import java.security.SecureRandom

class GeneratorServiceImpl : GeneratorService {
    private val numericAlphabetic = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val random = SecureRandom()

    override fun generateRandomString(len: Int, alphabet: String): String {
        val bytes = ByteArray(len)
        random.nextBytes(bytes)

        val sb = StringBuilder()
        val alphabetLen = alphabet.length

        for (i in 0 until len) {
            sb.append(alphabet[Math.abs(bytes[i] % alphabetLen)])
        }

        return sb.toString()
    }

    override fun generateNewFileName(): String {
        return generateRandomString(64, numericAlphabetic)
    }
}