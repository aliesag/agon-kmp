package com.agon.app.utils

import com.agon.app.Http
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ImageUploader {
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    private fun mimeFor(fileName: String): String = when {
        fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "image/jpeg"
        fileName.endsWith(".png", true) -> "image/png"
        fileName.endsWith(".gif", true) -> "image/gif"
        fileName.endsWith(".webp", true) -> "image/webp"
        fileName.endsWith(".m4a", true) || fileName.endsWith(".mp4", true) -> "audio/mp4"
        fileName.endsWith(".mp3", true) -> "audio/mpeg"
        else -> "application/octet-stream"
    }

    private fun videoMimeFor(fileName: String): String = when {
        fileName.endsWith(".mp4", true) -> "video/mp4"
        fileName.endsWith(".m4a", true) -> "audio/mp4"
        fileName.endsWith(".mp3", true) -> "audio/mpeg"
        fileName.endsWith(".png", true) -> "image/png"
        else -> "video/mp4"
    }

    suspend fun downloadAudioBytes(url: String): ByteArray? {
        if (url.isBlank()) return null
        return try {
            Http.client.get {
                url(url)
                header("User-Agent", UA)
            }.body<ByteArray>()
        } catch (e: Exception) { null }
    }

    suspend fun uploadImageBytes(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val fileName = "image_${now()}.jpg"
        uploadToTop4ToPDirect(bytes, fileName)?.let { return it }
        uploadToCatbox(bytes, fileName)?.let { return it }
        uploadToTmpFiles(bytes, fileName)?.let { return it }
        uploadToUguu(bytes, fileName)?.let { return it }
        return null
    }

    suspend fun uploadAudioBytes(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val fileName = "audio_${now()}.mp3"
        uploadToTop4ToPDirect(bytes, fileName)?.let { return it }
        uploadToCatbox(bytes, fileName)?.let { return it }
        uploadToTmpFiles(bytes, fileName)?.let { return it }
        uploadToUguu(bytes, fileName)?.let { return it }
        return null
    }

    suspend fun uploadVideoBytes(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val fileName = "video_${now()}.mp4"
        uploadToTmpFiles(bytes, fileName)?.let { return it }
        uploadToCatbox(bytes, fileName)?.let { return it }
        uploadToUguu(bytes, fileName)?.let { return it }
        uploadToTop4ToPDirect(bytes, fileName)?.let { return it }
        return null
    }

    private suspend fun uploadToTmpFiles(bytes: ByteArray, fileName: String): String? = try {
        val resp = Http.client.post {
            url("https://tmpfiles.org/api/v1/upload")
            header("User-Agent", UA)
            setBody(MultiPartFormDataContent(formData {
                append("file", bytes, Headers.build {
                    append(HttpHeaders.ContentType, mimeFor(fileName))
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }))
        }.bodyAsText()
        val url = Regex("\"url\"\\s*:\\s*\"([^\"]+)\"").find(resp)?.groupValues?.getOrNull(1) ?: return null
        if (url.startsWith("https://tmpfiles.org/")) {
            var clean = url.replace("tmpfiles.org/dl/dl/", "tmpfiles.org/dl/")
            if (!clean.contains("tmpfiles.org/dl/")) clean = clean.replace("tmpfiles.org/", "tmpfiles.org/dl/")
            clean
        } else null
    } catch (e: Exception) { null }

    private suspend fun uploadToUguu(bytes: ByteArray, fileName: String): String? = try {
        val resp = Http.client.post {
            url("https://uguu.se/api.php?d=upload-tool")
            header("User-Agent", UA)
            setBody(MultiPartFormDataContent(formData {
                append("files[]", bytes, Headers.build {
                    append(HttpHeaders.ContentType, mimeFor(fileName))
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }))
        }.bodyAsText().trim()
        if (resp.startsWith("https://")) resp else null
    } catch (e: Exception) { null }

    private suspend fun uploadToCatbox(bytes: ByteArray, fileName: String): String? = try {
        val resp = Http.client.post {
            url("https://catbox.moe/user/api.php")
            header("User-Agent", UA)
            setBody(MultiPartFormDataContent(formData {
                append("reqtype", "fileupload")
                append("fileToUpload", bytes, Headers.build {
                    append(HttpHeaders.ContentType, mimeFor(fileName))
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }))
        }.bodyAsText().trim()
        if (resp.startsWith("https://files.catbox.moe/")) resp else null
    } catch (e: Exception) { null }

    private suspend fun uploadToTop4ToPDirect(bytes: ByteArray, fileName: String): String? {
        if (bytes.isEmpty()) return null
        var sid = "75dESY6Cyj5qJ4BgDGo14a8PEF1"
        var cookieHeader = ""
        try {
            val homeResp = Http.client.post {
                url("https://top4top.io/")
                header("User-Agent", UA)
            }
            val html = homeResp.bodyAsText()
            Regex("name=\"sid\"\\s+value=\"([^\"]+)\"").find(html)?.groupValues?.getOrNull(1)?.let { sid = it }
            val cookies = homeResp.headers.getAll(HttpHeaders.SetCookie).orEmpty()
            if (cookies.isNotEmpty()) cookieHeader = cookies.joinToString("; ") { it.substringBefore(";") }
        } catch (_: Exception) { }

        return try {
            val resp = Http.client.post {
                url("https://top4top.io/index.php")
                header("User-Agent", UA)
                if (cookieHeader.isNotEmpty()) header("Cookie", cookieHeader)
                setBody(MultiPartFormDataContent(formData {
                    append("sid", sid)
                    append("submitr", "[ رفع الملفات ]")
                    append("file_0_", bytes, Headers.build {
                        append(HttpHeaders.ContentType, mimeFor(fileName))
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                }))
            }.bodyAsText()
            Regex("https://[a-zA-Z0-9.-]+\\.top4top\\.io/[a-zA-Z0-9]_[a-zA-Z0-9._-]+(\\.[a-zA-Z0-9]+)?")
                .find(resp)?.value
        } catch (e: Exception) { null }
    }

    private fun percentEncode(s: String): String {
        val hex = "0123456789ABCDEF"
        val sb = StringBuilder()
        for (b in s.encodeToByteArray()) {
            val v = b.toInt() and 0xFF
            when {
                (v in 'A'.code..'Z'.code) || (v in 'a'.code..'z'.code) || (v in '0'.code..'9'.code) ||
                    v == '-'.code || v == '_'.code || v == '.'.code || v == '~'.code -> sb.append(v.toChar())
                else -> sb.append('%').append(hex[v shr 4]).append(hex[v and 0xF])
            }
        }
        return sb.toString()
    }
}
