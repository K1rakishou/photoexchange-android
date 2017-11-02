package com.kirakishou.photoexchange.routers

import org.springframework.web.reactive.function.server.router

class Router() {

    fun setUpRouter() = router {
        "/v1".nest {
            "/api".nest {

            }
        }
    }
}