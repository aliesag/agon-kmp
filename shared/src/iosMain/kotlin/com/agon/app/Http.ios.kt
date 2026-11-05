package com.agon.app

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.engine.darwin.Darwin

actual fun platformHttpClient(): HttpClient = HttpClient(Darwin) {
    install(HttpTimeout) {
        requestTimeoutMillis = 25_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 25_000
    }
    install(HttpRequestRetry) {
        maxRetries = 1
        retryOnServerErrors(maxRetries = 1)
    }
    followRedirects = true
    expectSuccess = false
    defaultRequest {
        header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1")
        header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
        header("Accept-Language", "ar-SA,ar;q=0.9,en-US;q=0.8,en;q=0.7")
        header("Accept-Encoding", "gzip, deflate, br")
        header("Cache-Control", "no-cache")
        header("Pragma", "no-cache")
    }
}
