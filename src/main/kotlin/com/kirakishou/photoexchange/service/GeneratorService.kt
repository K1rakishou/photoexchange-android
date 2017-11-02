package com.kirakishou.photoexchange.service

interface GeneratorService {
    fun generateRandomString(len: Int, alphabet: String): String
    fun generateNewFileName(): String
}