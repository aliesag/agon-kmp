package com.agon.app.data

import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object LocalStreamProxy {

    private var serverSocket: ServerSocket? = null
    var port: Int = 0
        private set

    private val proxyScope = CoroutineScope(Dispatchers.IO)

    @Synchronized
    fun start() {
        if (serverSocket != null && !serverSocket!!.isClosed) return
        try {
            serverSocket = ServerSocket(0, 100, InetAddress.getByName("127.0.0.1"))
            port = serverSocket!!.localPort
            Thread {
                while (serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val client = serverSocket!!.accept()
                        proxyScope.launch {
                            handleClient(client)
                        }
                    } catch (e: Exception) {
                        break
                    }
                }
            }.apply {
                isDaemon = true
                name = "LocalStreamProxyThread"
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isAlooyCdnUrl(url: String): Boolean {
        if (url.isBlank() || url.startsWith("http://127.0.0.1") || url.contains("/stream?url=")) return false
        val lower = url.lowercase()
        return lower.contains("ylkqwsfghrt") ||
                lower.contains("zvde") ||
                lower.contains("ujeklsj") ||
                lower.contains("work456") ||
                lower.contains("work789") ||
                lower.contains("workers.dev") ||
                lower.contains("alooy") ||
                lower.contains(".click/") ||
                lower.contains(".site/") ||
                lower.contains(".online/") ||
                lower.contains("/file/") ||
                lower.contains("/wp-alooytv/") ||
                lower.contains("/uploads/") ||
                Regex("""https?://(?:vid\d+|w\d+)\.""", RegexOption.IGNORE_CASE).containsMatchIn(url)
    }

    fun wrapIfAlooy(url: String): String {
        if (url.isBlank() || url.startsWith("http://127.0.0.1") || url.contains("/stream?url=")) return url
        if (!isAlooyCdnUrl(url)) return url
        start()
        if (port == 0) return url
        return try {
            "http://127.0.0.1:$port/stream?url=" + URLEncoder.encode(url, "UTF-8")
        } catch (e: Exception) {
            url
        }
    }

    private fun handleClient(client: Socket) {
        try {
            client.soTimeout = 20000
            val input = client.getInputStream()
            val output = client.getOutputStream()

            val reader = input.bufferedReader()
            val requestLine = reader.readLine() ?: run {
                client.close()
                return
            }

            val isHead = requestLine.startsWith("HEAD ")
            if (!requestLine.startsWith("GET ") && !isHead) {
                client.close()
                return
            }

            val pathWithQuery = requestLine.split(" ").getOrNull(1) ?: run {
                client.close()
                return
            }

            if (!pathWithQuery.startsWith("/stream?url=")) {
                client.close()
                return
            }

            val rawEncodedUrl = pathWithQuery.substringAfter("/stream?url=").substringBefore(" ")
            val targetUrl = try {
                URLDecoder.decode(rawEncodedUrl, "UTF-8")
            } catch (e: Exception) {
                rawEncodedUrl
            }

            // Read headers from client (e.g. Range header)
            var rangeHeader: String? = null
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                if (line.startsWith("Range:", ignoreCase = true)) {
                    rangeHeader = line.substringAfter(":").trim()
                }
            }

            streamRemoteUrl(targetUrl, rangeHeader, output, 0, isHead)
        } catch (e: Exception) {
            // Socket closed or client disconnected
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }

    private fun streamRemoteUrl(targetUrl: String, rangeHeader: String?, output: OutputStream, redirectCount: Int = 0, isHead: Boolean = false) {
        if (redirectCount > 4) return

        var conn: HttpURLConnection? = null
        try {
            val urlObj = URL(targetUrl)
            conn = urlObj.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 12000
            conn.readTimeout = 25000
            conn.requestMethod = if (isHead) "HEAD" else "GET"

            // Inject official Alooytv Referer, Origin, and desktop User-Agent to bypass anti-hotlinking
            conn.setRequestProperty("Referer", "https://ep.alooytv16.xyz/")
            conn.setRequestProperty("Origin", "https://ep.alooytv16.xyz")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            conn.setRequestProperty("Accept", "*/*")
            if (!rangeHeader.isNullOrBlank()) {
                conn.setRequestProperty("Range", rangeHeader)
            }

            val responseCode = conn.responseCode

            // Reject anti-leech redirect video from archive.org completely
            if (responseCode in 300..399) {
                val location = conn.getHeaderField("Location")
                if (location != null && !location.contains("archive.org") && !location.contains("alooytv.com.mp4")) {
                    conn.disconnect()
                    streamRemoteUrl(location, rangeHeader, output, redirectCount + 1, isHead)
                    return
                } else {
                    // Refuse archive.org redirect
                    output.write("HTTP/1.1 404 Not Found\r\n\r\n".toByteArray())
                    output.flush()
                    return
                }
            }

            if (responseCode == 200 || responseCode == 206) {
                val statusText = if (responseCode == 206) "Partial Content" else "OK"
                val responseHeaders = StringBuilder()
                responseHeaders.append("HTTP/1.1 $responseCode $statusText\r\n")
                responseHeaders.append("Content-Type: video/mp4\r\n")
                responseHeaders.append("Accept-Ranges: bytes\r\n")
                responseHeaders.append("Access-Control-Allow-Origin: *\r\n")
                responseHeaders.append("Connection: close\r\n")

                val contentLength = conn.getHeaderField("Content-Length")
                if (!contentLength.isNullOrBlank()) {
                    responseHeaders.append("Content-Length: $contentLength\r\n")
                }

                val contentRange = conn.getHeaderField("Content-Range")
                if (!contentRange.isNullOrBlank()) {
                    responseHeaders.append("Content-Range: $contentRange\r\n")
                }

                responseHeaders.append("\r\n")

                output.write(responseHeaders.toString().toByteArray())
                output.flush()

                if (!isHead) {
                    conn.inputStream.use { remoteStream ->
                        val buffer = ByteArray(32768)
                        var bytesRead: Int
                        while (remoteStream.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                        output.flush()
                    }
                }
            }
        } catch (e: Exception) {
            // Client closed stream or stream ended
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }
}
