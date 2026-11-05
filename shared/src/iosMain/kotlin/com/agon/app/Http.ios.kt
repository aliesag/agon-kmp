package com.agon.app

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.engine.darwin.Darwin

actual fun platformHttpClient(): HttpClient = HttpClient(Darwin) {
    install(HttpTimeout) {
        // 3cktv pages take 20-30s sometimes — be generous
        requestTimeoutMillis = 45_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 45_000
    }
    followRedirects = true
    expectSuccess = false
    defaultRequest {
        // EXACT Android desktop Chrome UA — sites block iOS Safari UA
        header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
        header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
        header("Accept-Language", "ar-SA,ar;q=0.9,en-US;q=0.8,en;q=0.7")
        header("Cache-Control", "no-cache")
        header("Pragma", "no-cache")
    }
}
