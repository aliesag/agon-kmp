package com.agon.app

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.engine.darwin.Darwin

actual fun platformHttpClient(): HttpClient = HttpClient(Darwin) {
    install(HttpTimeout) { requestTimeoutMillis = 8_000; connectTimeoutMillis = 6_000; socketTimeoutMillis = 8_000 }
    engine { configureRequest { } }
}
