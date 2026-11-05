package com.agon.app.data

import com.agon.app.Http
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** KMP replacements for the OkHttp PATCH/PUT/POST calls against Firebase RTDB. */
object FirebaseHttp {
    suspend fun patch(url: String, body: String): Boolean = try {
        Http.client.patch {
            url(url)
            contentType(ContentType.Application.Json)
            header("Content-Type", "application/json; charset=utf-8")
            setBody(body)
        }.bodyAsText(); true
    } catch (_: Exception) { false }

    suspend fun put(url: String, body: String): Boolean = try {
        Http.client.put {
            url(url)
            contentType(ContentType.Application.Json)
            header("Content-Type", "application/json; charset=utf-8")
            setBody(body)
        }.bodyAsText(); true
    } catch (_: Exception) { false }

    suspend fun post(url: String, body: String): String? = try {
        Http.client.post {
            url(url)
            contentType(ContentType.Application.Json)
            header("Content-Type", "application/json; charset=utf-8")
            setBody(body)
        }.bodyAsText()
    } catch (_: Exception) { null }

    suspend fun get(url: String): String? = try {
        Http.get(url)
    } catch (_: Exception) { null }
}
