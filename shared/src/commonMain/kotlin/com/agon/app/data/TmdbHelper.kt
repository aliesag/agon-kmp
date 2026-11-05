package com.agon.app.data

/**
 * KMP port of TmdbHelper.
 * - JSONObject -> plain Map<String, String?> (callers convert from JsonObject)
 * - ConcurrentHashMap -> HashMap (single-threaded cache on iOS main; fine for posters)
 * - cleanPosterUrl byte-encoding uses encodeToByteArray (Kotlin/Native safe)
 * - getPosterForTitle is a stub until the TMDB fetcher is wired (see note below)
 */
object TmdbHelper {
    private val posterCache = HashMap<String, String>()
    private val pendingFetches = HashSet<String>()

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

        if (clean.contains("image.tmdb.org")) return clean

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
                    val encodedSegments = rawPath.split("/").joinToString("/") { seg ->
                        if (seg.isEmpty()) "" else encodeSegment(seg)
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
                    ch == ',' || ch == ';' || ch == '=' || ch == ':' -> { sb.append(ch); i++ }
                ch == '@' -> { sb.append("%40"); i++ }
                ch == ' ' -> { sb.append("%20"); i++ }
                ch == '%' && i + 2 < segment.length &&
                    isHex(segment[i + 1]) && isHex(segment[i + 2]) -> {
                    sb.append('%').append(segment[i + 1]).append(segment[i + 2]); i += 3
                }
                else -> {
                    for (b in ch.toString().encodeToByteArray()) {
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

    fun getCachedPoster(key: String): String? {
        if (key.isBlank()) return null
        return posterCache[key]
    }

    fun cachePoster(key: String, url: String) {
        if (key.isNotBlank() && url.isNotBlank()) posterCache[key] = url
    }

    /**
     * FILL: wire this once the TMDB fetch path is ported (uses Ktor + JSON parsing).
     * Original Android code returned null here too — the real callers build the
     * TMDB URL and parse results with scoreResult(). Keeping parity.
     */
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

    /**
     * KMP-safe replacement for the JSONObject-based original.
     * Callers build the Map from their parsed JSON (e.g. kotlinx.serialization JsonObject):
     *   scoreResult(jsonObj.mapValues { it.value.jsonPrimitive.contentOrNull }, ...)
     */
    fun scoreResult(
        res: Map<String, String?>,
        targetTitle: String,
        candidateQuery: String = "",
        targetYear: String = ""
    ): Int {
        val name = res["name"].orEmpty().ifEmpty { res["title"].orEmpty() }
        val originalName = res["original_name"].orEmpty().ifEmpty { res["original_title"].orEmpty() }
        val firstAirDate = res["first_air_date"].orEmpty().ifEmpty { res["release_date"].orEmpty() }
        val originalLanguage = res["original_language"].orEmpty()
        val posterPath = res["poster_path"].orEmpty()

        val cleanTarget = Catalog.cleanSeriesTitle(targetTitle).ifEmpty { targetTitle }
        val normTarget = Catalog.normalizeTitle(cleanTarget)
        val normName = Catalog.normalizeTitle(name)
        val normOrig = Catalog.normalizeTitle(originalName)
        val normCand = Catalog.normalizeTitle(candidateQuery)

        if (normTarget.isBlank()) return -1

        var titleMatchScore = calculateTitleMatch(normTarget, normName, normOrig)

        if (normCand.isNotBlank() && (normName == normCand || normOrig == normCand)) {
            titleMatchScore = maxOf(titleMatchScore, 2000)
        } else if (normCand.isNotBlank() && calculateTitleMatch(normCand, normName, normOrig) > 0) {
            titleMatchScore = maxOf(titleMatchScore, calculateTitleMatch(normCand, normName, normOrig))
        }

        if (titleMatchScore <= 0) return -1

        var totalScore = titleMatchScore
        if (posterPath.isNotBlank()) totalScore += 50
        if (targetYear.isNotEmpty() && firstAirDate.contains(targetYear)) totalScore += 30
        if (originalLanguage.equals("tr", ignoreCase = true) && targetTitle.contains("تركي")) totalScore += 20

        return totalScore
    }

    private fun calculateTitleMatch(targetNorm: String, resultNormName: String, resultNormOrig: String): Int {
        if (targetNorm.isBlank()) return -1
        if (resultNormName == targetNorm || resultNormOrig == targetNorm) return 2000

        val targetTokens = targetNorm.split(" ").filter { it.isNotBlank() }
        val targetCoreTokens = targetTokens.filter { !ARABIC_STOP_WORDS.contains(it) }.map { normalizeToken(it) }.filter { it.isNotBlank() }
        val actualTargetTokens = if (targetCoreTokens.isNotEmpty()) targetCoreTokens else targetTokens.map { normalizeToken(it) }

        val nameTokens = resultNormName.split(" ").filter { it.isNotBlank() }.map { normalizeToken(it) }.filter { it.isNotBlank() }
        val origTokens = resultNormOrig.split(" ").filter { it.isNotBlank() }.map { normalizeToken(it) }.filter { it.isNotBlank() }

        val matchedInName = actualTargetTokens.count { nameTokens.contains(it) }
        val matchedInOrig = actualTargetTokens.count { origTokens.contains(it) }
        val maxMatched = maxOf(matchedInName, matchedInOrig)

        if (maxMatched == actualTargetTokens.size) {
            val matchedResultSize = if (matchedInName >= matchedInOrig) nameTokens.size else origTokens.size
            return if (matchedResultSize == actualTargetTokens.size) 1900
            else if (resultNormName.startsWith(targetNorm) || resultNormOrig.startsWith(targetNorm) ||
                targetNorm.startsWith(resultNormName)) 1600 else 1400
        }

        if (actualTargetTokens.size >= 3 && maxMatched >= actualTargetTokens.size - 1) {
            val matchedResultSize = if (matchedInName >= matchedInOrig) nameTokens.size else origTokens.size
            if (matchedResultSize <= actualTargetTokens.size + 2) return 1100
        }

        return -1
    }
}
