package com.kirakishou.photoexchange.routers

import com.kirakishou.photoexchange.handlers.UploadPhotoHandler
import org.springframework.web.reactive.function.server.router

class Router(private val uploadPhotoHandler: UploadPhotoHandler) {

    fun setUpRouter() = router {
        "/v1".nest {
            "/api".nest {
                POST("/upload", uploadPhotoHandler::handle)
            }
        }
    }
}