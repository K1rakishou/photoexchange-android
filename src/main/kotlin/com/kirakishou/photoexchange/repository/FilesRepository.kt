package com.kirakishou.photoexchange.repository

import org.springframework.data.mongodb.core.ReactiveMongoTemplate

open class FilesRepository(private val template: ReactiveMongoTemplate) {

    companion object {
        const val COLLECTION_NAME = "files"
    }
}