package com.agon.app.data

/**
 * Kotlin/Native-safe subset of the original Android TmdbHelper.
 * Only the pure-string helper used by Catalog lives here; the JVM-based
 * TMDB network/caching code stays in pending/ until ported.
 */
object TmdbHelper {
    private const val HEX = "0123456789ABCDEF"

    fun cleanPosterUrl(url: String): String {
        if (url.isBlank()) return ""
        var clean = url.trim()
        if (clean.contains("grey.gif")) return ""
        if (clean.startsWith("//")) {
            clean = "https:$clean"
        } else if (clean.startsWith("/")) {
            clean = "https://e.3cktv.com$clean"
        } else if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://e.3cktv.com/$clean"
        }
        if (clean.startsWith("http://")) {
            clean = clean.replace("http://", "https://")
        }
        clean = clean.replace("\\/", "/")
        if (clean.contains("image.tmdb.org")) {
            return clean
        }
        if (clean.contains("/wp-content/")) {
            clean = clean.replace("https://e.3cktv.com/", "https://z.3cktv.com/")
                .replace("https://3iskk.xyz/", "https://z.3cktv.com/")
                .replace("https://3cktv.com/", "https://z.3cktv.com/")
                .replace("https://www.3cktv.com/", "https://z.3cktv.com/")
        } else {
            clean = clean.replace("https://z.3cktv.com/", "https://e.3cktv.com/")
                .replace("https://3iskk.xyz/", "https://e.3cktv.com/")
                .replace("https://3cktv.com/", "https://e.3cktv.com/")
                .replace("https://www.3cktv.com/", "https://e.3cktv.com/")
        }
        return try {
            val schemeEnd = clean.indexOf("://")
            if (schemeEnd != -1) {
                val pathStart = clean.indexOf('/', schemeEnd + 3)
                if (pathStart != -1) {
                    val domainPart = clean.substring(0, pathStart)
                    val pathAndQuery = clean.substring(pathStart)
                    val qIndex = pathAndQuery.indexOf('?')
                    val rawPath = if (qIndex != -1) pathAndQuery.substring(0, qIndex) else pathAndQuery
                    val query = if (qIndex != -1) pathAndQuery.substring(qIndex) else ""
                    val encodedSegments = rawPath.split("/").joinToString("/") { segment ->
                        if (segment.isEmpty()) "" else encodeSegment(segment)
                    }
                    "$domainPart$encodedSegments$query"
                } else clean
            } else clean
        } catch (e: Exception) {
            clean.replace(" ", "%20").replace("@", "%40")
        }
    }

    private fun encodeSegment(segment: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < segment.length) {
            val ch = segment[i]
            when {
                ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' ||
                    ch == '-' || ch == '_' || ch == '.' || ch == '~' ||
                    ch == '!' || ch == '$' || ch == '\'' ||
                    ch == '(' || ch == ')' || ch == '*' || ch == '+' ||
                    ch == ',' || ch == ';' || ch == '=' || ch == ':' -> {
                    sb.append(ch); i++
                }
                ch == '@' -> { sb.append("%40"); i++ }
                ch == ' ' -> { sb.append("%20"); i++ }
                ch == '%' && i + 2 < segment.length &&
                    isHex(segment[i + 1]) && isHex(segment[i + 2]) -> {
                    sb.append('%').append(segment[i + 1]).append(segment[i + 2])
                    i += 3
                }
                else -> {
                    val bytes = ch.toString().toByteArray(Charsets.UTF_8)
                    for (b in bytes) {
                        val v = b.toInt() and 0xFF
                        sb.append('%').append(HEX[v shr 4]).append(HEX[v and 0xF])
                    }
                    i++
                }
            }
        }
        return sb.toString()
    }

    private fun isHex(c: Char) = c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'
}
