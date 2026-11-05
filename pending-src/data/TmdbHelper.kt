package com.agon.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder

object TmdbHelper {
    private val posterCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val pendingFetches = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

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

        // Standardize image server: wp-content assets live on z.3cktv.com CDN, rest on e.3cktv.com
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

        // Manually & safely encode non-ASCII characters, spaces, and special symbols in path (like @) for OkHttp
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
                        if (segment.isEmpty()) ""
                        else {
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
                                        sb.append(ch)
                                        i++
                                    }
                                    ch == '@' -> {
                                        sb.append("%40")
                                        i++
                                    }
                                    ch == ' ' -> {
                                        sb.append("%20")
                                        i++
                                    }
                                    ch == '%' && i + 2 < segment.length &&
                                    (segment[i + 1] in '0'..'9' || segment[i + 1] in 'a'..'f' || segment[i + 1] in 'A'..'F') &&
                                    (segment[i + 2] in '0'..'9' || segment[i + 2] in 'a'..'f' || segment[i + 2] in 'A'..'F') -> {
                                        sb.append('%').append(segment[i + 1]).append(segment[i + 2])
                                        i += 3
                                    }
                                    else -> {
                                        val bytes = ch.toString().toByteArray(Charsets.UTF_8)
                                        for (b in bytes) {
                                            sb.append(String.format("%%%02X", b))
                                        }
                                        i++
                                    }
                                }
                            }
                            sb.toString()
                        }
                    }
                    "$domainPart$encodedSegments$query"
                } else clean
            } else clean
        } catch (e: Exception) {
            clean.replace(" ", "%20").replace("@", "%40")
        }
    }

    fun getCachedPoster(key: String): String? {
        if (key.isBlank()) return null
        return posterCache[key]
    }

    fun cachePoster(key: String, url: String) {
        if (key.isNotBlank() && url.isNotBlank()) {
            posterCache[key] = url
        }
    }

    suspend fun getPosterForTitle(
        rawTitle: String,
        showId: String = "",
        link: String = "",
        desc: String = ""
    ): String? = null

    private val ARABIC_STOP_WORDS = setOf(
        "في", "من", "على", "عن", "إلى", "الي", "مع", "هو", "هي", "ما", "لا", "كان", "أن", "ان", "يا", "بن", "ولد", "ابن", "ذو", "ذا", "ال", "لل", "التي", "الذي", "الذين"
    )

    fun normalizeToken(token: String): String {
        var t = Catalog.normalizeTitle(token)
        if (t.startsWith("بال") && t.length > 4) t = t.substring(3)
        if (t.startsWith("لل") && t.length > 3) t = t.substring(2)
        if (t.startsWith("ال") && t.length > 3) t = t.substring(2)
        return t
    }

    val KNOWN_ALIASES = mapOf(
        "تحت الارض" to listOf("في ظل", "تحت الأرض", "في الظل"),
        "في سابعة عشر" to listOf("Daha 17", "في السابعة عشر", "مازلت في سابعة عشر", "Torn Apart"),
        "الامام الغزالي" to listOf("الإمام الغزالي", "الغزالي"),
        "ميرا" to listOf("ميرا وسليم", "Aşk ve Gözyaşı", "ميرا"),
        "كان كل شيء" to listOf("Evlilik Hakkında Her Şey", "Her Şey", "كل شيء عن الزواج", "كان يا ما كان في تشوكورفا"),
        "حال ليلى" to listOf("Leyla", "حال ليلى")
    )

    fun scoreResult(
        res: JSONObject,
        targetTitle: String,
        candidateQuery: String = "",
        targetYear: String = ""
    ): Int {
        val name = res.optString("name", "").ifEmpty { res.optString("title", "") }
        val originalName = res.optString("original_name", "").ifEmpty { res.optString("original_title", "") }
        val firstAirDate = res.optString("first_air_date", "").ifEmpty { res.optString("release_date", "") }
        val originalLanguage = res.optString("original_language", "")
        val posterPath = res.optString("poster_path", "")

        val cleanTarget = Catalog.cleanSeriesTitle(targetTitle).ifEmpty { targetTitle }
        val normTarget = Catalog.normalizeTitle(cleanTarget)
        val normName = Catalog.normalizeTitle(name)
        val normOrig = Catalog.normalizeTitle(originalName)
        val normCand = Catalog.normalizeTitle(candidateQuery)

        if (normTarget.isBlank()) return -1

        var titleMatchScore = calculateTitleMatch(normTarget, normName, normOrig)
        
        // If we found a match using the exact candidate query (e.g. from aliases)
        if (normCand.isNotBlank() && (normName == normCand || normOrig == normCand)) {
            titleMatchScore = maxOf(titleMatchScore, 2000)
        } else if (normCand.isNotBlank() && calculateTitleMatch(normCand, normName, normOrig) > 0) {
            titleMatchScore = maxOf(titleMatchScore, calculateTitleMatch(normCand, normName, normOrig))
        }

        if (titleMatchScore <= 0) {
            return -1
        }

        var totalScore = titleMatchScore

        if (posterPath.isNotBlank()) {
            totalScore += 50
        }

        if (targetYear.isNotEmpty() && firstAirDate.contains(targetYear)) {
            totalScore += 30
        }

        if (originalLanguage.equals("tr", ignoreCase = true) && targetTitle.contains("تركي")) {
            totalScore += 20
        }

        return totalScore
    }

    private fun calculateTitleMatch(targetNorm: String, resultNormName: String, resultNormOrig: String): Int {
        if (targetNorm.isBlank()) return -1

        if (resultNormName == targetNorm || resultNormOrig == targetNorm) {
            return 2000
        }

        val targetTokens = targetNorm.split(" ").filter { it.isNotBlank() }
        val targetCoreTokens = targetTokens.filter { !ARABIC_STOP_WORDS.contains(it) }.map { normalizeToken(it) }.filter { it.isNotBlank() }
        val actualTargetTokens = if (targetCoreTokens.isNotEmpty()) targetCoreTokens else targetTokens.map { normalizeToken(it) }

        val nameTokens = resultNormName.split(" ").filter { it.isNotBlank() }.map { normalizeToken(it) }.filter { it.isNotBlank() }
        val origTokens = resultNormOrig.split(" ").filter { it.isNotBlank() }.map { normalizeToken(it) }.filter { it.isNotBlank() }

        val matchedInName = actualTargetTokens.count { t -> nameTokens.contains(t) }
        val matchedInOrig = actualTargetTokens.count { t -> origTokens.contains(t) }
        val maxMatched = maxOf(matchedInName, matchedInOrig)

        if (maxMatched == actualTargetTokens.size) {
            val matchedResultSize = if (matchedInName >= matchedInOrig) nameTokens.size else origTokens.size
            return if (matchedResultSize == actualTargetTokens.size) {
                1900
            } else if (resultNormName.startsWith(targetNorm) || resultNormOrig.startsWith(targetNorm) || targetNorm.startsWith(resultNormName)) {
                1600
            } else {
                1400
            }
        }

        if (actualTargetTokens.size >= 3 && maxMatched >= actualTargetTokens.size - 1) {
            val matchedResultSize = if (matchedInName >= matchedInOrig) nameTokens.size else origTokens.size
            if (matchedResultSize <= actualTargetTokens.size + 2) {
                return 1100
            }
        }

        return -1
    }
}




