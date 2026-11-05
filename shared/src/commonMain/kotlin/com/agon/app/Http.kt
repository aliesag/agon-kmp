package com.agon.app

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText

expect fun platformHttpClient(): HttpClient

object Http {
    val client: HttpClient by lazy { platformHttpClient() }

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String =
        client.get(url) { headers.forEach { (k, v) -> header(k, v) } }.bodyAsText()
}
