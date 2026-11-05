package com.agon.app.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object ImageUploader {
    // Highly-configured OkHttpClient with custom timeouts to prevent hanging
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Highly robust file downloading with automatic local caching and custom User-Agent
    suspend fun downloadAudioFile(context: Context, url: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                val hash = url.hashCode().toString()
                val ext = if (url.contains(".mp3", ignoreCase = true)) "mp3" else "m4a"
                val cacheFile = File(context.cacheDir, "cached_voice_${hash}.${ext}")

                if (cacheFile.exists() && cacheFile.length() > 500) {
                    return@withContext cacheFile
                }

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body ?: return@withContext null
                        FileOutputStream(cacheFile).use { output ->
                            body.byteStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        if (cacheFile.exists() && cacheFile.length() > 500) {
                            return@withContext cacheFile
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }

    // 1. Tmpfiles.org API (Extremely fast, returns JSON, we convert URL to direct /dl/ link)
    private suspend fun uploadToTmpFiles(bytes: ByteArray, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (bytes.isEmpty()) return@withContext null

                val mimeType = when {
                    fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> "image/jpeg"
                    fileName.endsWith(".png") -> "image/png"
                    fileName.endsWith(".gif") -> "image/gif"
                    fileName.endsWith(".webp") -> "image/webp"
                    fileName.endsWith(".m4a") -> "audio/mp4"
                    fileName.endsWith(".mp4") -> "audio/mp4"
                    fileName.endsWith(".mp3") -> "audio/mpeg"
                    else -> "application/octet-stream"
                }

                val fileBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, fileBody)
                    .build()

                val request = Request.Builder()
                    .url("https://tmpfiles.org/api/v1/upload")
                    .post(requestBody)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        // Response pattern: "url":"https://tmpfiles.org/7552554/image.png"
                        val urlPattern = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"")
                        val matcher = urlPattern.matcher(body)
                        if (matcher.find()) {
                            val rawUrl = matcher.group(1) ?: ""
                            if (rawUrl.startsWith("https://tmpfiles.org/")) {
                                var clean = rawUrl
                                clean = clean.replace("tmpfiles.org/dl/dl/", "tmpfiles.org/dl/")
                                if (!clean.contains("tmpfiles.org/dl/")) {
                                    clean = clean.replace("tmpfiles.org/", "tmpfiles.org/dl/")
                                }
                                return@withContext clean
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }

    // 2. Uguu.se API (Extremely reliable, returns plain text direct link)
    private suspend fun uploadToUguu(bytes: ByteArray, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (bytes.isEmpty()) return@withContext null

                val mimeType = when {
                    fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> "image/jpeg"
                    fileName.endsWith(".png") -> "image/png"
                    fileName.endsWith(".gif") -> "image/gif"
                    fileName.endsWith(".webp") -> "image/webp"
                    fileName.endsWith(".m4a") -> "audio/mp4"
                    fileName.endsWith(".mp4") -> "audio/mp4"
                    fileName.endsWith(".mp3") -> "audio/mpeg"
                    else -> "application/octet-stream"
                }

                val fileBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("files[]", fileName, fileBody)
                    .build()

                val request = Request.Builder()
                    .url("https://uguu.se/api.php?d=upload-tool")
                    .post(requestBody)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val result = response.body?.string()?.trim() ?: ""
                        if (result.startsWith("https://")) {
                            return@withContext result
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }

    // 3. Catbox.moe API (High-reliability direct file upload API)
    private suspend fun uploadToCatbox(bytes: ByteArray, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (bytes.isEmpty()) return@withContext null

                val mimeType = when {
                    fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> "image/jpeg"
                    fileName.endsWith(".png") -> "image/png"
                    fileName.endsWith(".gif") -> "image/gif"
                    fileName.endsWith(".webp") -> "image/webp"
                    fileName.endsWith(".m4a") -> "audio/mp4"
                    fileName.endsWith(".mp4") -> "audio/mp4"
                    fileName.endsWith(".mp3") -> "audio/mpeg"
                    else -> "application/octet-stream"
                }

                val fileBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("reqtype", "fileupload")
                    .addFormDataPart("fileToUpload", fileName, fileBody)
                    .build()

                val request = Request.Builder()
                    .url("https://catbox.moe/user/api.php")
                    .post(requestBody)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val result = response.body?.string()?.trim() ?: ""
                        if (result.startsWith("https://files.catbox.moe/")) {
                            return@withContext result
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }

    // 4. Robust Top4toP Upload & Parser
    private suspend fun uploadToTop4ToPDirect(bytes: ByteArray, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (bytes.isEmpty()) return@withContext null

                // Step 4a: Fetch top4top.io home page to retrieve current session ID (sid) & cookie
                val getRequest = Request.Builder()
                    .url("https://top4top.io/")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                var sid = "75dESY6Cyj5qJ4BgDGo14a8PEF1" // Default fallback sid
                var cookieHeader = ""

                try {
                    client.newCall(getRequest).execute().use { getResponse ->
                        val bodyString = getResponse.body?.string() ?: ""
                        val matcher = Pattern.compile("name=\"sid\"\\s+value=\"([^\"]+)\"").matcher(bodyString)
                        if (matcher.find()) {
                            sid = matcher.group(1) ?: sid
                        }
                        val cookies = getResponse.headers("Set-Cookie")
                        if (cookies.isNotEmpty()) {
                            cookieHeader = cookies.joinToString("; ") { it.split(";")[0] }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Step 4b: Build form parts and post
                val mimeType = when {
                    fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> "image/jpeg"
                    fileName.endsWith(".png") -> "image/png"
                    fileName.endsWith(".gif") -> "image/gif"
                    fileName.endsWith(".webp") -> "image/webp"
                    fileName.endsWith(".m4a") -> "audio/mp4"
                    fileName.endsWith(".mp4") -> "audio/mp4"
                    fileName.endsWith(".mp3") -> "audio/mpeg"
                    else -> "application/octet-stream"
                }
                val fileBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("sid", sid)
                    .addFormDataPart("submitr", "[ رفع الملفات ]")
                    .addFormDataPart("file_0_", fileName, fileBody)
                    .build()

                val postRequest = Request.Builder()
                    .url("https://top4top.io/index.php")
                    .post(requestBody)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .apply {
                        if (cookieHeader.isNotEmpty()) {
                            header("Cookie", cookieHeader)
                        }
                    }
                    .build()

                client.newCall(postRequest).execute().use { postResponse ->
                    if (!postResponse.isSuccessful) return@withContext null
                    val responseHtml = postResponse.body?.string() ?: ""

                    // Search for direct links: matches any sub-domain of top4top with letter_ prefix and optional extension
                    val pattern = Pattern.compile("https://[a-zA-Z0-9.-]+\\.top4top\\.io/[a-zA-Z0-9]_[a-zA-Z0-9._-]+(\\.[a-zA-Z0-9]+)?")
                    val matcher = pattern.matcher(responseHtml)
                    if (matcher.find()) {
                        return@withContext matcher.group(0)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }

    // Firebase Storage Direct Upload
    private suspend fun uploadToFirebaseStorage(bytes: ByteArray, fileName: String, folder: String = "shorts"): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (bytes.isEmpty()) return@withContext null
                val bucket = "chat-516a8.firebasestorage.app"
                val encodedPath = java.net.URLEncoder.encode("$folder/$fileName", "UTF-8")
                val uploadUrl = "https://firebasestorage.googleapis.com/v0/b/$bucket/o?name=$encodedPath"

                val mimeType = when {
                    fileName.endsWith(".mp4") -> "video/mp4"
                    fileName.endsWith(".m4a") -> "audio/mp4"
                    fileName.endsWith(".mp3") -> "audio/mpeg"
                    fileName.endsWith(".png") -> "image/png"
                    else -> "video/mp4"
                }

                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .header("User-Agent", "Mozilla/5.0 (Android) AgonApp")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = org.json.JSONObject(body)
                        val name = json.optString("name", "")
                        val downloadTokens = json.optString("downloadTokens", "")
                        if (name.isNotEmpty()) {
                            val encName = java.net.URLEncoder.encode(name, "UTF-8")
                            val downloadUrl = if (downloadTokens.isNotEmpty()) {
                                "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$encName?alt=media&token=$downloadTokens"
                            } else {
                                "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$encName?alt=media"
                            }
                            return@withContext downloadUrl
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }

    // File-based Catbox.moe Upload (Streams from disk without memory overhead)
    private suspend fun uploadToCatboxFile(file: java.io.File, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (!file.exists() || file.length() == 0L) return@withContext null
                val fileBody = file.asRequestBody("video/mp4".toMediaTypeOrNull())
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("reqtype", "fileupload")
                    .addFormDataPart("fileToUpload", fileName, fileBody)
                    .build()

                val request = Request.Builder()
                    .url("https://catbox.moe/user/api.php")
                    .post(requestBody)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val result = response.body?.string()?.trim() ?: ""
                        if (result.startsWith("https://files.catbox.moe/")) {
                            return@withContext result
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }

    // File-based Top4toP Upload & Parser (Streams from disk without memory overhead)
    private suspend fun uploadToTop4ToPFile(file: java.io.File, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (!file.exists() || file.length() == 0L) return@withContext null

                // Step 1: Fetch top4top.io home page to retrieve current session ID (sid) & cookie
                val getRequest = Request.Builder()
                    .url("https://top4top.io/")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                var sid = "75dESY6Cyj5qJ4BgDGo14a8PEF1"
                var cookieHeader = ""

                try {
                    client.newCall(getRequest).execute().use { getResponse ->
                        val bodyString = getResponse.body?.string() ?: ""
                        val matcher = Pattern.compile("name=\"sid\"\\s+value=\"([^\"]+)\"").matcher(bodyString)
                        if (matcher.find()) {
                            sid = matcher.group(1) ?: sid
                        }
                        val cookies = getResponse.headers("Set-Cookie")
                        if (cookies.isNotEmpty()) {
                            cookieHeader = cookies.joinToString("; ") { it.split(";")[0] }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Step 2: Build multipart post streaming from disk
                val fileBody = file.asRequestBody("video/mp4".toMediaTypeOrNull())
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("sid", sid)
                    .addFormDataPart("submitr", "[ رفع الملفات ]")
                    .addFormDataPart("file_0_", fileName, fileBody)
                    .build()

                val postRequest = Request.Builder()
                    .url("https://top4top.io/index.php")
                    .post(requestBody)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .apply {
                        if (cookieHeader.isNotEmpty()) {
                            header("Cookie", cookieHeader)
                        }
                    }
                    .build()

                client.newCall(postRequest).execute().use { postResponse ->
                    if (!postResponse.isSuccessful) return@withContext null
                    val responseHtml = postResponse.body?.string() ?: ""

                    // Search for direct links: matches any sub-domain of top4top
                    val pattern = Pattern.compile("https://[a-zA-Z0-9.-]+\\.top4top\\.io/[a-zA-Z0-9]_[a-zA-Z0-9._-]+(\\.[a-zA-Z0-9]+)?")
                    val matcher = pattern.matcher(responseHtml)
                    if (matcher.find()) {
                        return@withContext matcher.group(0)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }

    // File-based TmpFiles Upload
    private suspend fun uploadToTmpFilesFile(file: java.io.File, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (!file.exists() || file.length() == 0L) return@withContext null
                val fileBody = file.asRequestBody("video/mp4".toMediaTypeOrNull())
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, fileBody)
                    .build()

                val request = Request.Builder()
                    .url("https://tmpfiles.org/api/v1/upload")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = org.json.JSONObject(body)
                        // TmpFiles returns {"status":"success","data":{"url":"https://tmpfiles.org/XXXXX/file.mp4"}}
                        val dataObj = json.optJSONObject("data")
                        val url = dataObj?.optString("url") ?: ""
                        if (url.isNotEmpty()) {
                            var clean = url
                            clean = clean.replace("tmpfiles.org/dl/dl/", "tmpfiles.org/dl/")
                            if (!clean.contains("tmpfiles.org/dl/")) {
                                clean = clean.replace("tmpfiles.org/", "tmpfiles.org/dl/")
                            }
                            return@withContext clean
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }

    // File-based Uguu Upload
    private suspend fun uploadToUguuFile(file: java.io.File, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (!file.exists() || file.length() == 0L) return@withContext null
                val fileBody = file.asRequestBody("video/mp4".toMediaTypeOrNull())
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("files[]", fileName, fileBody)
                    .build()

                val request = Request.Builder()
                    .url("https://uguu.se/upload.php")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = org.json.JSONObject(body)
                        val files = json.optJSONArray("files")
                        if (files != null && files.length() > 0) {
                            return@withContext files.getJSONObject(0).optString("url")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }

    // Unified Video/Media Upload Flow for Shorts (Top4toP + Catbox + TmpFiles + Uguu) - Zero-Crash Memory Stream
    suspend fun uploadVideoToFirebaseAndFallbacks(context: Context, uri: Uri): String? {
        return uploadVideoToTop4ToPAndFallbacks(context, uri)
    }

    suspend fun uploadVideoToTop4ToPAndFallbacks(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            var tempFile: java.io.File? = null
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                tempFile = java.io.File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
                java.io.FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                inputStream.close()

                if (!tempFile.exists() || tempFile.length() == 0L) return@withContext null
                val fileName = "video_${System.currentTimeMillis()}.mp4"

                // Strategy 1: TmpFiles (Fast & Reliable direct video streaming)
                val urlTmp = uploadToTmpFilesFile(tempFile, fileName)
                if (urlTmp != null) return@withContext urlTmp

                // Strategy 2: Catbox
                val urlCatbox = uploadToCatboxFile(tempFile, fileName)
                if (urlCatbox != null) return@withContext urlCatbox

                // Strategy 3: Uguu.se
                val urlUguu = uploadToUguuFile(tempFile, fileName)
                if (urlUguu != null) return@withContext urlUguu

                // Strategy 4: Top4toP (Fallback)
                val urlTop4 = uploadToTop4ToPFile(tempFile, fileName)
                if (urlTop4 != null) return@withContext urlTop4

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    tempFile?.delete()
                } catch (_: Exception) {}
            }
            return@withContext null
        }
    }

    // Unified Image Upload Flow with Intelligent Parallel & Sequential Fallbacks
    suspend fun uploadToTop4ToP(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Read image content resolver bytes
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val bytes = inputStream.readBytes()
                inputStream.close()

                if (bytes.isEmpty()) return@withContext null
                val fileName = "image_${System.currentTimeMillis()}.jpg"

                // Strategy 1: Top4toP (Classic backup with parsed direct link)
                val urlTop4 = uploadToTop4ToPDirect(bytes, fileName)
                if (urlTop4 != null) return@withContext urlTop4

                // Strategy 2: Catbox (Permanent storage, very stable)
                val urlCatbox = uploadToCatbox(bytes, fileName)
                if (urlCatbox != null) return@withContext urlCatbox

                // Strategy 3: TmpFiles (Fast, but temporary)
                val urlTmp = uploadToTmpFiles(bytes, fileName)
                if (urlTmp != null) return@withContext urlTmp

                // Strategy 4: Uguu.se (Reliable backup but temporary)
                val urlUguu = uploadToUguu(bytes, fileName)
                if (urlUguu != null) return@withContext urlUguu

            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }

    // Unified Audio Upload Flow with Intelligent Parallel & Sequential Fallbacks
    suspend fun uploadAudioToTop4ToP(context: Context, bytes: ByteArray): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (bytes.isEmpty()) return@withContext null
                val fileName = "audio_${System.currentTimeMillis()}.mp3"

                // Strategy 1: Top4toP (Classic backup with parsed direct link)
                val urlTop4 = uploadToTop4ToPDirect(bytes, fileName)
                if (urlTop4 != null) return@withContext urlTop4

                // Strategy 2: Catbox (Permanent storage, very stable)
                val urlCatbox = uploadToCatbox(bytes, fileName)
                if (urlCatbox != null) return@withContext urlCatbox

                // Strategy 3: TmpFiles (Fast, but temporary)
                val urlTmp = uploadToTmpFiles(bytes, fileName)
                if (urlTmp != null) return@withContext urlTmp

                // Strategy 4: Uguu.se (Reliable backup but temporary)
                val urlUguu = uploadToUguu(bytes, fileName)
                if (urlUguu != null) return@withContext urlUguu

            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }
}
