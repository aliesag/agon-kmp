package com.agon.app.data

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.agon.app.Http
import kotlinx.serialization.json.contentOrNull
import com.fleeksoft.ksoup.nodes.Element
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

object EpisodeScraper {

    private val UA_LIST = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
    )

    private suspend fun fetchDocOrThrow(url: String, referrer: String? = null): Document {
        val headers = mutableMapOf(
            "User-Agent" to UA_LIST[0],
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
            "Accept-Language" to "ar-SA,ar;q=0.9,en-US;q=0.8,en;q=0.7",
            "Cache-Control" to "no-cache"
        )
        if (!referrer.isNullOrBlank()) headers["Referer"] = referrer
        return Ksoup.parse(Http.get(url, headers))
    }

    private fun hostOf(url: String): String? = try {
        val noScheme = url.substringAfter("://", url)
        noScheme.substringBefore("/").substringBefore("?").ifBlank { null }
    } catch (_: Exception) { null }

    private fun schemeOf(url: String): String = try {
        if (url.contains("://")) url.substringBefore("://") else "https"
    } catch (_: Exception) { "https" }

    private fun encodeQuery(s: String): String {
        val hex = "0123456789ABCDEF"
        val sb = StringBuilder()
        for (b in s.encodeToByteArray()) {
            val v = b.toInt() and 0xFF
            when {
                (v in 'A'.code..'Z'.code) || (v in 'a'.code..'z'.code) || (v in '0'.code..'9'.code) ||
                    v == '-'.code || v == '_'.code || v == '.'.code || v == '~'.code -> sb.append(v.toChar())
                v == ' '.code -> sb.append('+')
                else -> sb.append('%').append(hex[v shr 4]).append(hex[v and 0xF])
            }
        }
        return sb.toString()
    }

    private const val B64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    private fun base64Decode(input: String): ByteArray {
        val s = input.trim().replace("-", "+").replace("_", "/")
        val out = ArrayList<Byte>(s.length * 3 / 4)
        var buffer = 0
        var bits = 0
        for (c in s) {
            if (c == '=') break
            val v = B64_ALPHABET.indexOf(c)
            if (v < 0) continue
            buffer = (buffer shl 6) or v
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.add(((buffer shr bits) and 0xFF).toByte())
            }
        }
        return out.toByteArray()
    }

    private fun base64Encode(bytes: ByteArray): String {
        val sb = StringBuilder()
        var buffer = 0
        var bits = 0
        for (b in bytes) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bits += 8
            while (bits >= 6) {
                bits -= 6
                sb.append(B64_ALPHABET[(buffer shr bits) and 0x3F])
            }
        }
        if (bits > 0) sb.append(B64_ALPHABET[(buffer shl (6 - bits)) and 0x3F])
        while (sb.length % 4 != 0) sb.append('=')
        return sb.toString()
    }

    private val jsonLenient = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }

    private fun parseJsonMap(json: String): Map<String, String> {
        return try {
            val obj = jsonLenient.parseToJsonElement(json) as? kotlinx.serialization.json.JsonObject ?: return emptyMap()
            obj.mapValues { (_, v) -> (v as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull ?: "" }
        } catch (e: Exception) { emptyMap() }
    }

    private suspend fun fetchDocWithBypass(url: String, referrer: String? = null): Document? {
        for (ua in UA_LIST) {
            try {
                val headers = mutableMapOf(
                    "User-Agent" to ua,
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                    "Accept-Language" to "ar-SA,ar;q=0.9,en-US;q=0.8,en;q=0.7",
                    "Cache-Control" to "no-cache"
                )
                if (!referrer.isNullOrBlank()) headers["Referer"] = referrer
                val doc = Ksoup.parse(Http.get(url, headers))
                val html = doc.html()
                if (!html.contains("you have been blocked") &&
                    !html.contains("You are unable to access") &&
                    !html.contains("Sorry, you have been blocked") &&
                    !html.contains("cf-browser-verification")) {
                    return doc
                }
            } catch (e: Exception) { /* try next UA */ }
        }
        return null
    }

    private val bgRegex = Regex("url\\(['\"]?(.*?)['\"]?\\)")

    fun extractPosterFromElement(el: Element): String {
        // 1. Check style in .imgSer, .poster, .img, or any element with style attribute containing url
        val styleEls = el.select(".imgSer, .poster, .img, [style*=background-image], [style*=url]")
        for (sEl in styleEls) {
            val style = sEl.attr("style")
            val match = bgRegex.find(style)
            if (match != null) {
                val u = match.groupValues[1].trim()
                if (u.isNotEmpty() && !u.contains("grey.gif")) {
                    val fullUrl = if (u.startsWith("//")) "https:$u" else u
                    return TmdbHelper.cleanPosterUrl(fullUrl)
                }
            }
        }
        val elStyle = el.attr("style")
        val elMatch = bgRegex.find(elStyle)
        if (elMatch != null) {
            val u = elMatch.groupValues[1].trim()
            if (u.isNotEmpty() && !u.contains("grey.gif")) {
                val fullUrl = if (u.startsWith("//")) "https:$u" else u
                return TmdbHelper.cleanPosterUrl(fullUrl)
            }
        }

        // 2. Check img tags
        val imgEls = el.select("img")
        for (img in imgEls) {
            var src = img.attr("data-img").ifEmpty { null }
                ?: img.attr("data-src").ifEmpty { null }
                ?: img.attr("data-lazy-src").ifEmpty { null }
                ?: img.attr("data-original").ifEmpty { null }
                ?: img.attr("src").ifEmpty { null }
                ?: ""
            if (src.contains("grey.gif")) {
                src = img.attr("data-img").ifEmpty { null }
                    ?: img.attr("data-src").ifEmpty { null }
                    ?: img.attr("data-lazy-src").ifEmpty { null }
                    ?: img.attr("data-original").ifEmpty { null }
                    ?: ""
            }
            if (src.isNotEmpty() && !src.contains("grey.gif")) {
                val fullUrl = if (src.startsWith("//")) "https:$src" else src
                return TmdbHelper.cleanPosterUrl(fullUrl)
            }
        }

        return ""
    }

    suspend fun scrapeSeriesPage(page: Int, domain: String = "e.3cktv.com"): List<Show> {
        return withContext(Dispatchers.Default) {
            val shows = mutableListOf<Show>()
            try {
                val url = if (page <= 1) "https://$domain/video/series/" else "https://$domain/video/series/page/$page/"
                val doc = fetchDocWithBypass(url) ?: fetchDocOrThrow(url)

                val elements = doc.select("article.postEp, article.post").ifEmpty { doc.select(".block-post") }
                elements.forEachIndexed { index, el ->
                    val aTag = el.select("a").firstOrNull() ?: return@forEachIndexed
                    val link = aTag.attr("href")
                    if (link.isBlank() || !link.contains("/video/series/")) return@forEachIndexed

                    val rawTitle = el.select(".title").text().trim().ifEmpty { aTag.attr("title").trim() }
                    val cleanTitle = rawTitle.replace("قصة عشق", "").trim().removeSuffix("-").trim()
                    if (cleanTitle.isBlank()) return@forEachIndexed

                    val seasonSpan = el.select(".seasonName").text().trim()
                    val isDubbed = cleanTitle.contains("مدبلج") || seasonSpan.contains("مدبلج")

                    val poster = extractPosterFromElement(el)

                    val slug = link.removeSuffix("/").substringAfterLast("/")
                    val id = if (slug.isNotEmpty()) slug else "show_${page}_$index"

                    val genresList = mutableListOf("مسلسلات تركية")
                    if (isDubbed) genresList.add("مدبلج") else genresList.add("مترجم")
                    genresList.add("دراما")

                    shows.add(
                        Catalog.sanitizeShowToSeries(
                            Show(
                                id = id,
                                title = cleanTitle,
                                year = "2026",
                                rating = if (isDubbed) "مدبلج" else "مترجم",
                                imdb = 7.5 + (index % 10) * 0.1,
                                genres = genresList,
                                description = "مشاهدة وتحميل جميع حلقات $cleanTitle اونلاين بجودة عالية HD على حكايتنا.",
                                poster = poster,
                                backdrop = poster,
                                duration = "45 دقيقة",
                                isTop10 = page == 1 && index < 5,
                                isNew = page == 1 && index < 3,
                                link = link,
                                isMovie = false
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            shows.distinctBy { it.link }.filterNot { Catalog.isTashShow(it.title, it.link, it.id) }
        }
    }

    suspend fun getEpisodesForShow(showUrl: String): List<Episode> {
        return withContext(Dispatchers.Default) {
            val episodes = mutableListOf<Episode>()
            try {
                if (showUrl.isBlank() || Catalog.isTashShow("", showUrl, "")) return@withContext emptyList()
                
                val targetUrl = if (showUrl.contains("-ep-") || (showUrl.contains("/video/") && !showUrl.contains("/series/"))) {
                    val slug = showUrl.removeSuffix("/").substringAfterLast("/")
                    val seriesSlug = slug.replace(Regex("-ep-\\d+.*"), "")
                    "https://e.3cktv.com/video/series/$seriesSlug/"
                } else showUrl

                val showSlug = showUrl.removeSuffix("/").substringAfterLast("/").replace(".html", "").replace(Regex("[^a-zA-Z0-9_]"), "_").ifEmpty { "show" }

                val doc = fetchDocWithBypass(targetUrl) ?: fetchDocOrThrow(targetUrl)

                val pageTitle = doc.title().lowercase()
                if (pageTitle.contains("404") || pageTitle.contains("not found") || pageTitle.contains("غير موجود")) {
                    return@withContext emptyList()
                }

                if (targetUrl.contains("ffosta-tv") || targetUrl.contains("fosta")) {
                    // Strictly look for season containers to avoid sidebar contamination
                    val seriesContainers = doc.select(".SeasonsBox, .SeasonsEpisodesMain, .serie_eps, .TabE, .TabS, .content-serie, .main-serie, #mobileselect, .SeasonList")
                    
                    // Priority 1: Extract seasons from specific containers (Divs)
                    val seasonDivs = seriesContainers.select(".tabcontent, div[id^='Season'], div[id^='MSeason'], .tab-content .tab-pane, div.tab-pane, .SeasonList-item")
                    
                    if (seasonDivs.isNotEmpty()) {
                        seasonDivs.forEach { sDiv ->
                            val sId = sDiv.id().ifEmpty { sDiv.attr("id") }
                            // Try to get season number from ID (e.g., Season1, MSeason17, season-2)
                            var seasonNum = Regex("(\\d+)").find(sId)?.groupValues?.get(1)?.toIntOrNull()
                            
                            // If not in ID, check for labels/headers above or within the div
                            if (seasonNum == null) {
                                val prevEl = sDiv.previousElementSibling()
                                if (prevEl != null && (prevEl.tagName() == "h3" || prevEl.tagName() == "h2" || prevEl.hasClass("title"))) {
                                    seasonNum = Regex("(?:الموسم|موسم|S|الجزء|جزء)\\s*(\\d+)", RegexOption.IGNORE_CASE).find(prevEl.text())?.groupValues?.get(1)?.toIntOrNull()
                                }
                                if (seasonNum == null) {
                                    val headerEl = sDiv.select("h2, h3, .title, .season-title").firstOrNull()
                                    if (headerEl != null) {
                                        seasonNum = Regex("(?:الموسم|موسم|S|الجزء|جزء)\\s*(\\d+)", RegexOption.IGNORE_CASE).find(headerEl.text())?.groupValues?.get(1)?.toIntOrNull()
                                    }
                                }
                            }
                            
                            val finalSeasonNum = seasonNum ?: 1
                            val links = sDiv.select("a[href*='watch.php'], a[href*='video.php'], a[href*='view.php'], a[href*='episode.php']")

                            links.forEachIndexed { index, a ->
                                var link = a.attr("href").trim()
                                if (link.isBlank()) return@forEachIndexed
                                if (link.startsWith("/")) link = "https://ffosta-tv.quest$link"
                                else if (!link.startsWith("http")) link = "https://ffosta-tv.quest/$link"

                                val emText = a.select("em").text().trim()
                                val rawTitle = a.attr("title").ifEmpty { a.text() }.trim()
                                
                                // Skip navigation links and duplicates
                                if (rawTitle.contains("السابقة") || rawTitle.contains("التالية")) return@forEachIndexed

                                val epNum = if (emText.isNotEmpty() && emText.all { it.isDigit() }) {
                                    emText
                                } else {
                                    re_episode_number(rawTitle) ?: (index + 1).toString()
                                }

                                val epId = "fosta_${showSlug}_s${finalSeasonNum}_ep${epNum}"
                                if (episodes.none { it.id == epId || (it.seasonNumber == finalSeasonNum && it.episodeNum == epNum) }) {
                                    episodes.add(
                                        Episode(
                                            id = epId,
                                            title = "الحلقة $epNum",
                                            episodeNum = epNum,
                                            poster = "",
                                            link = link,
                                            ribbon = null,
                                            seasonNumber = finalSeasonNum
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Priority 2: Extract seasons from Select dropdowns if no Divs were found or if more seasons might be there
                    val seasonSelects = seriesContainers.select("select.episodeoption, select[id^='MSeason'], select.seasons_select, #mobileselect, select[name='season'], select.form-control")
                    if (seasonSelects.isNotEmpty()) {
                        seasonSelects.forEach { sel ->
                            val sId = sel.id().ifEmpty { sel.attr("id") }
                            var baseSeasonNum = Regex("(\\d+)").find(sId)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                            
                            val optElements = sel.select("option[value*='watch.php'], option[value*='video.php'], option[value*='view.php']")

                            optElements.forEachIndexed { index, opt ->
                                var link = opt.attr("value").trim()
                                if (link.isBlank() || link == "select-ep" || link == "0") return@forEachIndexed
                                if (link.startsWith("/")) link = "https://ffosta-tv.quest$link"
                                else if (!link.startsWith("http")) link = "https://ffosta-tv.quest/$link"

                                val rawText = opt.text().trim()
                                if (rawText.contains("السابقة") || rawText.contains("التالية")) return@forEachIndexed

                                val epNum = re_episode_number(rawText) ?: (index + 1).toString()
                                
                                // Some selects might have season info in the text
                                val seasonMatch = Regex("(?:الموسم|موسم|S|الجزء|جزء)\\s*(\\d+)", RegexOption.IGNORE_CASE).find(rawText)
                                val currentSeasonNum = seasonMatch?.groupValues?.get(1)?.toIntOrNull() ?: baseSeasonNum

                                val epId = "fosta_${showSlug}_s${currentSeasonNum}_ep${epNum}"
                                if (episodes.none { it.id == epId || (it.seasonNumber == currentSeasonNum && it.episodeNum == epNum) }) {
                                    episodes.add(
                                        Episode(
                                            id = epId,
                                            title = "الحلقة $epNum",
                                            episodeNum = epNum,
                                            poster = "",
                                            link = link,
                                            ribbon = null,
                                            seasonNumber = currentSeasonNum
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Priority 3: Fallback for fragmented series with links in sidebar or main block
                    // Enhanced for Tash Ma Tash specifically
                    val serieItems = doc.select("li[class*='col-'], .item-show, .block-post, .serie_eps a, .SeasonsEpisodesMain a, .block-series a, .block-series-item")
                    serieItems.forEachIndexed { index, el ->
                         val aTag = if (el.tagName() == "a") el else (el.selectFirst("a[href*='watch.php']") ?: el.selectFirst("a[href*='view-serie.php']") ?: el.selectFirst("a")) ?: return@forEachIndexed
                         var link = aTag.attr("href").trim()
                         if (link.isEmpty()) return@forEachIndexed
                         if (link.startsWith("/")) link = "https://ffosta-tv.quest$link"
                         else if (!link.startsWith("http")) link = "https://ffosta-tv.quest/$link"

                         val rawTitle = aTag.attr("title").ifEmpty { aTag.text() }.trim()
                         if (rawTitle.contains("السابقة") || rawTitle.contains("التالية")) return@forEachIndexed
                         
                         val epNum = re_episode_number(rawTitle) ?: (index + 1).toString()
                         
                         // Check for season in title (Tash Ma Tash Season 17, etc.)
                         val seasonMatch = Regex("(?:الموسم|موسم|S|الجزء|جزء)\\s*(\\d+)", RegexOption.IGNORE_CASE).find(rawTitle)
                         val currentSeasonNum = seasonMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1

                         val imgEl = el.selectFirst("img")
                         var poster = imgEl?.attr("data-echo")?.ifEmpty { null }
                             ?: imgEl?.attr("data-src")?.ifEmpty { null }
                             ?: imgEl?.attr("src") ?: ""
                         if (poster.startsWith("/")) poster = "https://ffosta-tv.quest$poster"
                         if (poster.contains("lzld.png") || poster.contains("echo-")) poster = ""
                         poster = TmdbHelper.cleanPosterUrl(poster)

                         val epId = "fosta_${showSlug}_s${currentSeasonNum}_ep_${epNum}_$index"
                         if (episodes.none { it.id == epId || (it.seasonNumber == currentSeasonNum && it.episodeNum == epNum) }) {
                             episodes.add(
                                 Episode(
                                     id = epId,
                                     title = "الحلقة $epNum",
                                     episodeNum = epNum,
                                     poster = poster,
                                     link = link,
                                     ribbon = if (rawTitle.contains("الموسم")) "موسم كامل" else null,
                                     seasonNumber = currentSeasonNum
                                 )
                             )
                         }
                    }

                    if (episodes.isEmpty()) {
                        val movieTitle = cleanFostaTitle(doc.title())
                        val vid = if (targetUrl.contains("vid=")) targetUrl.substringAfter("vid=").substringBefore("&") else targetUrl.hashCode().toString()
                        episodes.add(
                            Episode(
                                id = "fosta_single_$vid",
                                title = if (movieTitle.contains("فيلم") || movieTitle.contains("فلم")) movieTitle else "مشاهدة العرض كامل",
                                episodeNum = "1",
                                poster = "",
                                link = targetUrl,
                                ribbon = null,
                                seasonNumber = 1
                            )
                        )
                    }

                    if (episodes.isNotEmpty()) {
                        return@withContext episodes
                            .distinctBy { Pair(it.seasonNumber, it.episodeNum) }
                            .sortedWith(compareBy({ it.seasonNumber }, { it.episodeNum.toIntOrNull() ?: 0 }))
                    }
                }

                if (targetUrl.contains("alooytv")) {
                    val alooyEpButtons = doc.select(".season a.btn-ep, .season a, a.btn-ep, .episodes a, a[href*='key=']")
                    if (alooyEpButtons.isNotEmpty()) {
                        alooyEpButtons.forEachIndexed { index, el ->
                            var link = el.attr("href")
                            if (link.startsWith("/")) {
                                val _uriSrc = targetUrl
                                link = "${schemeOf(_uriSrc)}://${hostOf(_uriSrc) ?: ""}$link"
                            } else if (!link.startsWith("http")) {
                                val _uriSrc = targetUrl
                                link = "${schemeOf(_uriSrc)}://${hostOf(_uriSrc) ?: ""}/${link.removePrefix("/")}"
                            }
                            val rawText = el.text().trim()
                            val epNum = rawText.replace("Ep#", "").replace("Ep", "").replace("الحلقة", "").trim().ifEmpty { (index + 1).toString() }
                            
                            if (episodes.none { it.link == link || it.episodeNum == epNum }) {
                                episodes.add(
                                    Episode(
                                        id = "${showSlug}_ep_alooy_${index}_$epNum",
                                        title = "الحلقة $epNum",
                                        episodeNum = epNum,
                                        poster = "",
                                        link = link,
                                        ribbon = if (index == 0) "أحدث حلقة" else null
                                    )
                                )
                            }
                        }
                        if (episodes.isNotEmpty()) {
                            return@withContext episodes.distinctBy { it.episodeNum }.sortedBy { it.episodeNum.toIntOrNull() ?: 0 }
                        }
                    }
                    
                    if (episodes.isEmpty()) {
                        val epNum = "1"
                        episodes.add(
                            Episode(
                                id = "${showSlug}_ep_alooy_single",
                                title = doc.title().replace(" - Alooytv", "").trim(),
                                episodeNum = epNum,
                                poster = "",
                                link = targetUrl,
                                ribbon = null
                            )
                        )
                        return@withContext episodes
                    }
                }

                val epLinks = doc.select("ul.eplist a.epNum, a.epNum, article.postEp a, article.post a, .block-post a, .episodes a, .episode-item a, .season-list a, a.btn-ep, .season a")
                if (epLinks.isNotEmpty()) {
                    epLinks.forEachIndexed { index, el ->
                        val link = el.attr("href")
                        if (!link.contains("-ep-") && !link.contains("key=") && !link.contains("/video/")) return@forEachIndexed
                        
                        val rawTitle = el.attr("title").trim().ifEmpty { el.text().trim() }
                        val epNumSpan = el.select("span").text().trim()
                        
                        // Extract episode number from link if possible, as it's more reliable
                        var calculatedEpNum = link.substringAfter("-ep-").substringBefore("/").substringBefore(".html")
                        if (calculatedEpNum.isEmpty() || !calculatedEpNum.all { it.isDigit() }) {
                            calculatedEpNum = if (epNumSpan.isNotEmpty() && epNumSpan.all { it.isDigit() }) {
                                epNumSpan
                            } else {
                                re_episode_number(rawTitle) ?: (index + 1).toString()
                            }
                        }
                        
                        val title = if (rawTitle.isNotEmpty()) rawTitle else "الحلقة $calculatedEpNum"
                        
                        // Deduplicate by link and episode number
                        if (episodes.any { it.link == link || it.episodeNum == calculatedEpNum }) return@forEachIndexed

                        episodes.add(
                            Episode(
                                id = "${showSlug}_ep_${index}_${calculatedEpNum}",
                                title = title,
                                episodeNum = calculatedEpNum,
                                poster = extractPosterFromElement(el).ifEmpty { "" },
                                link = link,
                                ribbon = if (index == 0) "أحدث حلقة" else null
                            )
                        )
                    }
                    if (episodes.isNotEmpty()) return@withContext episodes.distinctBy { it.episodeNum }.sortedByDescending { it.episodeNum.toIntOrNull() ?: 0 }
                }

                val elements = doc.select("article.postEp, article.post, .block-post, .item-show")
                if (elements.isNotEmpty()) {
                    elements.forEachIndexed { index, el ->
                        val aTag = el.select("a").first() ?: return@forEachIndexed
                        val link = aTag.attr("href")
                        val title = el.select(".title").text().trim()
                        val siteRibbon = el.select(".ribbon").text().trim().ifEmpty { null }
                        val episodeNumSpans = el.select(".episodeNum span")
                        val calculatedEpNum = if (episodeNumSpans.size >= 2) {
                            episodeNumSpans[1].text().trim()
                        } else if (episodeNumSpans.isNotEmpty()) {
                            episodeNumSpans[0].text().trim()
                        } else {
                            re_episode_number(title) ?: (index + 1).toString()
                        }

                        val ribbon = siteRibbon ?: when {
                            index == 0 -> "أحدث حلقة"
                            index == elements.size - 1 && elements.size > 1 -> "بداية الموسم"
                            else -> null
                        }

                        var poster = extractPosterFromElement(el)
                        if (poster.isEmpty()) {
                            val imgEl = el.select("img").first()
                            poster = imgEl?.attr("data-img")?.ifEmpty { null }
                                ?: imgEl?.attr("data-src")?.ifEmpty { null }
                                ?: imgEl?.attr("src")?.ifEmpty { null }
                                ?: ""
                            if (poster.startsWith("//")) {
                                poster = "https:$poster"
                            } else if (poster.startsWith("/")) {
                                val _uriSrc = showUrl
                                poster = "${schemeOf(_uriSrc)}://${hostOf(_uriSrc) ?: ""}$poster"
                            }
                            poster = TmdbHelper.cleanPosterUrl(poster)
                        }

                        episodes.add(
                            Episode(
                                id = "${showSlug}_ep_${index}_${calculatedEpNum}",
                                title = title,
                                episodeNum = calculatedEpNum,
                                poster = poster,
                                link = link,
                                ribbon = ribbon
                            )
                        )
                    }
                    if (episodes.isNotEmpty()) return@withContext episodes.distinctBy { it.episodeNum }
                }

                val alooyEpButtons = doc.select(".season a.btn-ep, .season a, a.btn-ep, .episodes a, a[href*='key=']")
                if (alooyEpButtons.isNotEmpty()) {
                    alooyEpButtons.forEachIndexed { index, el ->
                        var link = el.attr("href")
                        if (link.startsWith("/")) {
                            val _uriSrc = targetUrl
                            link = "${schemeOf(_uriSrc)}://${hostOf(_uriSrc) ?: ""}$link"
                        }
                        val rawText = el.text().trim()
                        val epNum = rawText.replace("Ep#", "").replace("Ep", "").replace("الحلقة", "").trim().ifEmpty { (index + 1).toString() }
                        episodes.add(
                            Episode(
                                id = "${showSlug}_ep_alooy_${index}_$epNum",
                                title = "الحلقة $epNum",
                                episodeNum = epNum,
                                poster = "",
                                link = link,
                                ribbon = if (index == 0) "أحدث حلقة" else null
                            )
                        )
                    }
                    if (episodes.isNotEmpty()) return@withContext episodes.distinctBy { it.episodeNum }
                }

                if (episodes.isEmpty() && showUrl.contains("3cktv")) {
                    val cleanUrl = showUrl.removeSuffix("/")
                    val slug = cleanUrl.substringAfterLast("/series/").substringAfterLast("/")
                    val domain = hostOf(showUrl) ?: "e.3cktv.com"
                    if (slug.isNotEmpty()) {
                        // If we couldn't scrape, try to detect total from title or just a reasonable default
                        val totalEp = 20 
                        for (i in totalEp downTo 1) {
                            val numStr = if (i < 10) "0$i" else i.toString()
                            val epLink = "https://$domain/video/$slug-ep-$numStr/"
                            episodes.add(
                                Episode(
                                    id = "ep_${slug}_$i",
                                    title = "الحلقة $i",
                                    episodeNum = i.toString(),
                                    poster = "",
                                    link = epLink,
                                    ribbon = if (i == totalEp) "أحدث حلقة" else null
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            episodes.distinctBy { it.episodeNum }
        }
    }

    suspend fun scrapeAlooyGenreMultiPage(genreSlug: String, genreName: String, maxPages: Int = 3): List<Show> {
        return withContext(Dispatchers.Default) {
            val list = mutableListOf<Show>()
            val baseUrls = mutableListOf("https://ep.alooytv16.xyz/genre/$genreSlug.html")
            for (p in 1 until maxPages) {
                baseUrls.add("https://ep.alooytv16.xyz/genre/$genreSlug/${p * 50}.html")
            }

            for (url in baseUrls) {
                try {
                    val doc = fetchDocOrThrow(url)

                    val items = doc.select(".latest-movie-img-container, .movie-img")
                    if (items.isEmpty()) break

                    for (el in items) {
                        val aTag = el.select("a[href*='/watch/']").firstOrNull() ?: el.select("a").firstOrNull() ?: continue
                        var link = aTag.attr("href")
                        if (link.startsWith("/")) {
                            link = "https://ep.alooytv16.xyz$link"
                        }

                        val imgEl = el.select("img").firstOrNull()
                        var poster = imgEl?.attr("data-src")?.ifEmpty { null } ?: imgEl?.attr("src") ?: ""
                        if (poster.startsWith("/")) {
                            poster = "https://ep.alooytv16.xyz$poster"
                        }
                        if (poster.contains("blank_thumbnail.jpg") || poster.contains("default_image")) {
                            val ds = imgEl?.attr("data-src") ?: ""
                            if (ds.isNotBlank() && !ds.contains("blank_thumbnail.jpg")) {
                                poster = if (ds.startsWith("/")) "https://ep.alooytv16.xyz$ds" else ds
                            }
                        }

                        val rawTitle = el.select(".movie-title h3 a, .movie-title h3").text().trim()
                            .ifEmpty { imgEl?.attr("alt")?.trim() ?: "" }
                            .ifEmpty { aTag.attr("title").trim() }

                        if (rawTitle.isBlank() || link.isBlank()) continue

                        val quality = el.select(".video_quality span").text().trim()
                        val cleanCat = if (genreName.contains("رمضان") || genreSlug.contains("ramadan")) "رمضان" else "خليجي"
                        val slug = link.substringAfterLast("/watch/").substringBefore(".html")
                        val id = "alooy_${slug.ifEmpty { rawTitle.hashCode().toString() }}"

                        list.add(
                            Show(
                                id = id,
                                title = rawTitle.replace("مدبلج", "").replace("مترجم", "").trim(),
                                year = "2026",
                                rating = cleanCat,
                                imdb = 8.4,
                                genres = listOf(cleanCat),
                                description = "شاهد $rawTitle بجودة عالية HD أونلاين على حكايتنا.",
                                poster = poster,
                                backdrop = poster,
                                duration = quality.ifEmpty { "45 دقيقة" },
                                isTop10 = false,
                                isNew = true,
                                ribbon = cleanCat,
                                link = link
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            list.distinctBy { it.link }
        }
    }

    suspend fun scrapeAllAlooytvContent(): List<Show> {
        return emptyList()
    }

    suspend fun scrape3cktvRootHome(): List<Show> {
        return withContext(Dispatchers.Default) {
            val list = mutableListOf<Show>()
            try {
                val doc = fetchDocWithBypass("https://e.3cktv.com/") ?: fetchDocOrThrow("https://e.3cktv.com/")

                val elements = doc.select("article.post, article.postEp, .block-post")
                elements.forEach { el ->
                    val aTag = el.select("a").firstOrNull() ?: return@forEach
                    val rawLink = aTag.attr("href")
                    if (rawLink.isBlank()) return@forEach

                    val seriesLink = if (rawLink.contains("/video/") && !rawLink.contains("/series/")) {
                        val slug = rawLink.removeSuffix("/").substringAfterLast("/")
                        val seriesSlug = slug.replace(Regex("-(modablaj-)?ep-\\d+.*"), "").replace(Regex("-ep\\d+.*"), "")
                        "https://e.3cktv.com/video/series/$seriesSlug/"
                    } else rawLink

                    val rawTitle = el.select(".title").text().trim()
                        .ifEmpty { aTag.attr("title").replace(" قصّة عشق", "").replace(" قصة عشق", "").trim() }
                    val cleanTitle = Catalog.cleanSeriesTitle(rawTitle)
                    val poster = extractPosterFromElement(el)
                    val id = "3ck_home_${seriesLink.hashCode()}"

                    if (cleanTitle.isNotBlank() && seriesLink.isNotBlank()) {
                        list.add(
                            Show(
                                id = id,
                                title = cleanTitle,
                                year = "2026",
                                rating = "مترجم",
                                imdb = 8.8,
                                genres = listOf("مسلسلات تركية"),
                                description = "شاهد $cleanTitle مترجم أونلاين بجودة عالية HD على حكايتنا.",
                                poster = poster,
                                backdrop = poster,
                                duration = "45 دقيقة",
                                link = seriesLink,
                                ribbon = "حلقة جديدة"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list.distinctBy { it.link }.filterNot { Catalog.isTashShow(it.title, it.link, it.id) }
        }
    }

    suspend fun search3cktv(query: String, maxPages: Int = 20): List<Show> {
        if (Catalog.isTashShow(query)) return emptyList()
        return withContext(Dispatchers.Default) {
            coroutineScope {
                val allShows = mutableListOf<Show>()
                try {
                    val encodedQuery = encodeQuery(query)
                    val jobs = (1..maxPages).map { page ->
                        async {
                            val pageShows = mutableListOf<Show>()
                            try {
                                val url = if (page == 1) "https://e.3cktv.com/?s=$encodedQuery" else "https://e.3cktv.com/page/$page/?s=$encodedQuery"
                                val doc = fetchDocWithBypass(url) ?: fetchDocOrThrow(url)

                                doc.select("article.postEp, article.post, .block-post").forEach { el ->
                                    val aTag = el.selectFirst("a") ?: return@forEach
                                    val title = aTag.attr("title").replace("قصة عشق", "").trim().ifEmpty { el.select(".title").text().replace("قصة عشق", "").trim() }
                                    val link = aTag.attr("href")
                                    val imgStyle = aTag.selectFirst(".imgSer")?.attr("style") ?: ""
                                    var imageUrl = extractPosterFromElement(el)
                                    if (imageUrl.isBlank()) {
                                        val urlMatcher = Regex("url\\((.*?)\\)").find(imgStyle)
                                        if (urlMatcher != null) {
                                            imageUrl = urlMatcher.groupValues[1].removeSurrounding("'").removeSurrounding("\"")
                                        }
                                    }

                                    if (title.isNotEmpty() && link.isNotEmpty()) {
                                        val ribbon = aTag.selectFirst(".seasonName span:nth-child(2)")?.text() ?: ""
                                        pageShows.add(Show(
                                            id = link.hashCode().toString(),
                                            title = title,
                                            year = "2026",
                                            rating = "تركي",
                                            imdb = 8.5,
                                            genres = listOf("مسلسلات تركية"),
                                            description = "شاهد $title مترجم أونلاين بجودة عالية HD على حكايتنا.",
                                            poster = imageUrl,
                                            backdrop = imageUrl,
                                            duration = "45 دقيقة",
                                            link = link,
                                            ribbon = ribbon
                                        ))
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            pageShows
                        }
                    }
                    val results = jobs.awaitAll()
                    results.forEach { allShows.addAll(it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                allShows.distinctBy { it.link }.filterNot { Catalog.isTashShow(it.title, it.link, it.id) }
            }
        }
    }

    fun cleanFostaTitle(rawTitle: String): String {
        var t = rawTitle
            .replace("فوستا تي في", "")
            .replace("فوستا TV", "", ignoreCase = true)
            .replace("فوستا tv", "", ignoreCase = true)
            .replace("فوستا", "")
            .replace("fosta-tv", "", ignoreCase = true)
            .replace("fosta", "", ignoreCase = true)
            .replace("مشاهدة وتحميل", "")
            .replace("مشاهدة", "")
            .replace("تحميل", "")
            .replace("اون لاين", "")
            .replace("اونلاين", "")
            .replace("كامل بجودة عالية HD", "")
            .replace("بجودة عالية HD", "")
            .replace("كامل", "")
            .replace("HD", "", ignoreCase = true)
            .replace("على حكايتنا", "")
            .replace("على", "")
            .replace(Regex("الحلقة\\s+\\d+.*"), "")
            .replace(Regex("حلقة\\s+\\d+.*"), "")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-')
        return if (t.isBlank()) "عمل فني" else t
    }

    suspend fun scrapeFostaSeries(page: Int): List<Show> {
        return withContext(Dispatchers.Default) {
            val list = mutableListOf<Show>()
            try {
                val url = if (page <= 1) "https://ffosta-tv.quest/moslslat.php" else "https://ffosta-tv.quest/moslslat.php?&page=$page"
                val doc = fetchDocWithBypass(url) ?: fetchDocOrThrow(url)

                val items = doc.select("li.col-xs-6, li.col-sm-4, li.col-md-2, li[class*='col-']")
                items.forEachIndexed { idx, el ->
                    val aTag = el.selectFirst("a[href*='view-serie.php']") ?: el.selectFirst("a") ?: return@forEachIndexed
                    var link = aTag.attr("href").trim()
                    if (link.isBlank()) return@forEachIndexed
                    if (link.startsWith("/")) link = "https://ffosta-tv.quest$link"

                    val rawTitle = aTag.attr("title").ifEmpty { aTag.text() }.trim()
                    val cleanTitle = cleanFostaTitle(rawTitle)
                    if (cleanTitle.isBlank()) return@forEachIndexed

                    val img = el.selectFirst("img")
                    var poster = img?.attr("data-echo")?.ifEmpty { null }
                        ?: img?.attr("data-src")?.ifEmpty { null }
                        ?: img?.attr("src") ?: ""
                    if (poster.startsWith("/")) poster = "https://ffosta-tv.quest$poster"
                    if (poster.contains("lzld.png") || poster.contains("echo-")) poster = ""
                    poster = TmdbHelper.cleanPosterUrl(poster)

                    val slug = link.substringAfter("name=").ifEmpty { "fosta_ser_${page}_$idx" }
                    val id = "fosta_${slug.replace("%", "_")}"

                    list.add(
                        Show(
                            id = id,
                            title = cleanTitle,
                            year = "2026",
                            rating = "عربي",
                            imdb = 8.2 + (idx % 8) * 0.1,
                            genres = listOf("مسلسلات عربية", "دراما"),
                            description = "مشاهدة وتحميل $cleanTitle بجودة عالية HD حصرياً على حكايتنا.",
                            poster = poster,
                            backdrop = poster,
                            duration = "45 دقيقة",
                            isTop10 = page == 1 && idx < 4,
                            isNew = page == 1 && idx < 6,
                            link = link,
                            ribbon = if (idx < 3 && page == 1) "حلقة جديدة" else null
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list.distinctBy { it.link }.filterNot { Catalog.isTashShow(it.title, it.link, it.id) }
        }
    }

    suspend fun scrapeFostaMovies(category: String, genreName: String, page: Int = 1): List<Show> {
        return withContext(Dispatchers.Default) {
            val list = mutableListOf<Show>()
            try {
                val url = if (page <= 1) "https://ffosta-tv.quest/category.php?cat=$category" else "https://ffosta-tv.quest/category.php?cat=$category&page=$page"
                val doc = fetchDocWithBypass(url) ?: fetchDocOrThrow(url)

                val items = doc.select("li.col-xs-6, li.col-sm-4, li.col-md-2, li[class*='col-']")
                items.forEachIndexed { idx, el ->
                    val aTag = el.selectFirst("a[href*='watch.php']") ?: el.selectFirst("a") ?: return@forEachIndexed
                    var link = aTag.attr("href").trim()
                    if (link.isBlank()) return@forEachIndexed
                    if (link.startsWith("/")) link = "https://ffosta-tv.quest$link"

                    val rawTitle = aTag.attr("title").ifEmpty { aTag.text() }.trim()
                    val cleanTitle = cleanFostaTitle(rawTitle)
                    if (cleanTitle.isBlank()) return@forEachIndexed

                    val img = el.selectFirst("img")
                    var poster = img?.attr("data-echo")?.ifEmpty { null }
                        ?: img?.attr("data-src")?.ifEmpty { null }
                        ?: img?.attr("src") ?: ""
                    if (poster.startsWith("/")) poster = "https://ffosta-tv.quest$poster"
                    if (poster.contains("lzld.png") || poster.contains("echo-")) poster = ""
                    poster = TmdbHelper.cleanPosterUrl(poster)

                    val vid = link.substringAfter("vid=").substringBefore("&").ifEmpty { "fosta_mov_${idx}" }
                    val id = "fosta_m_$vid"

                    list.add(
                        Show(
                            id = id,
                            title = cleanTitle,
                            year = "2025",
                            rating = "فيلم",
                            imdb = 8.0 + (idx % 10) * 0.1,
                            genres = listOf(genreName, "أفلام"),
                            description = "شاهد $cleanTitle بجودة عالية HD حصرياً على حكايتنا.",
                            poster = poster,
                            backdrop = poster,
                            duration = "ساعة ونصف",
                            isTop10 = page == 1 && idx < 3,
                            isNew = page == 1 && idx < 5,
                            link = link,
                            ribbon = if (idx < 2) "حصري حكايتنا" else null
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list.distinctBy { it.link }.filterNot { Catalog.isTashShow(it.title, it.link, it.id) }
        }
    }

    suspend fun scrapeFostaHomePage(): List<Show> {
        return withContext(Dispatchers.Default) {
            val list = mutableListOf<Show>()
            try {
                val url = "https://ffosta-tv.quest/"
                val doc = fetchDocWithBypass(url) ?: fetchDocOrThrow(url)

                val items = doc.select("li.col-xs-6, li.col-sm-4, li.col-md-2, li[class*='col-'], .block-post, .block-series")
                items.forEachIndexed { idx, el ->
                    val aTag = el.selectFirst("a[href*='watch.php']") 
                        ?: el.selectFirst("a[href*='view-serie.php']") 
                        ?: el.selectFirst("a") 
                        ?: return@forEachIndexed
                    var link = aTag.attr("href").trim()
                    if (link.isBlank()) return@forEachIndexed
                    if (link.startsWith("/")) link = "https://ffosta-tv.quest$link"

                    val rawTitle = aTag.attr("title").ifEmpty { aTag.text() }.trim()
                    val cleanTitle = cleanFostaTitle(rawTitle)
                    if (cleanTitle.isBlank()) return@forEachIndexed

                    val img = el.selectFirst("img")
                    var poster = img?.attr("data-echo")?.ifEmpty { null }
                        ?: img?.attr("data-src")?.ifEmpty { null }
                        ?: img?.attr("src") ?: ""
                    if (poster.startsWith("/")) poster = "https://ffosta-tv.quest$poster"
                    if (poster.contains("lzld.png") || poster.contains("echo-")) poster = ""
                    poster = TmdbHelper.cleanPosterUrl(poster)

                    val isMovie = cleanTitle.contains("فيلم") || rawTitle.contains("فيلم")
                    val vid = if (link.contains("vid=")) link.substringAfter("vid=").substringBefore("&") else link.hashCode().toString()
                    val id = "fosta_h_$vid"

                    list.add(
                        Show(
                            id = id,
                            title = cleanTitle,
                            year = "2026",
                            rating = if (isMovie) "فيلم" else "عربي",
                            imdb = 8.5,
                            genres = if (isMovie) listOf("أفلام حصرية", "أفلام") else listOf("مسلسلات عربية", "دراما"),
                            description = "شاهد $cleanTitle بجودة عالية HD حصرياً على حكايتنا.",
                            poster = poster,
                            backdrop = poster,
                            duration = if (isMovie) "ساعة ونصف" else "45 دقيقة",
                            isTop10 = idx < 5,
                            isNew = true,
                            link = link,
                            ribbon = if (isMovie) "فيلم جديد" else "حلقة جديدة"
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list.distinctBy { it.link }.filterNot { Catalog.isTashShow(it.title, it.link, it.id) }
        }
    }

    suspend fun searchFosta(query: String, maxPages: Int = 1): List<Show> {
        if (Catalog.isTashShow(query)) return emptyList()
        return withContext(Dispatchers.Default) {
            val list = mutableListOf<Show>()
            try {
                val enc = encodeQuery(query)
                val url = "https://ffosta-tv.quest/search.php?keywords=$enc"
                val doc = fetchDocWithBypass(url) ?: fetchDocOrThrow(url)

                val items = doc.select("li.col-xs-6, li.col-sm-4, li.col-md-2, li[class*='col-']")
                items.forEachIndexed { idx, el ->
                    val aTag = el.selectFirst("a[href*='view-serie.php']") 
                        ?: el.selectFirst("a[href*='watch.php']") 
                        ?: el.selectFirst("a") 
                        ?: return@forEachIndexed
                    var link = aTag.attr("href").trim()
                    if (link.isBlank()) return@forEachIndexed
                    if (link.startsWith("/")) link = "https://ffosta-tv.quest$link"

                    val rawTitle = aTag.attr("title").ifEmpty { aTag.text() }.trim()
                    val cleanTitle = cleanFostaTitle(rawTitle)
                    if (cleanTitle.isBlank() || Catalog.isTashShow(cleanTitle, link, "")) return@forEachIndexed

                    val img = el.selectFirst("img")
                    var poster = img?.attr("data-echo")?.ifEmpty { null }
                        ?: img?.attr("data-src")?.ifEmpty { null }
                        ?: img?.attr("src") ?: ""
                    if (poster.startsWith("/")) poster = "https://ffosta-tv.quest$poster"
                    if (poster.contains("lzld.png") || poster.contains("echo-")) poster = ""
                    poster = TmdbHelper.cleanPosterUrl(poster)

                    val isSeries = link.contains("view-serie.php") || link.contains("moslslat") || Catalog.isSeriesLink(link)
                    val isMovie = !isSeries && (cleanTitle.contains("فيلم") || cleanTitle.contains("فلم") || rawTitle.contains("فيلم") || rawTitle.contains("فلم"))
                    
                    val vid = if (link.contains("vid=")) link.substringAfter("vid=").substringBefore("&") else link.hashCode().toString()
                    val id = "fosta_s_$vid"

                    val show = Show(
                        id = id,
                        title = cleanTitle,
                        year = "2026",
                        rating = if (isMovie) "فيلم" else "عربي",
                        imdb = 8.3,
                        genres = if (isMovie) listOf("أفلام حصرية", "أفلام") else listOf("مسلسلات عربية", "دراما"),
                        description = "شاهد $cleanTitle بجودة عالية HD حصرياً على حكايتنا.",
                        poster = poster,
                        backdrop = poster,
                        duration = if (isMovie) "ساعة ونصف" else "45 دقيقة",
                        isTop10 = false,
                        isNew = true,
                        link = link,
                        isMovie = isMovie
                    )
                    list.add(Catalog.sanitizeShowToSeries(show))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list.distinctBy { it.link }.filterNot { Catalog.isTashShow(it.title, it.link, it.id) }
        }
    }

    suspend fun scrapeHomePageLatest(): List<Show> {
        // Fetch series specifically from https://e.3cktv.com/video/series/ and NOT from https://e.3cktv.com/
        return scrapeSeriesPage(1, "e.3cktv.com")
    }

    suspend fun getHomeScrapedShows(): List<HomeScrapedShow> {
        return withContext(Dispatchers.Default) {
            val list = mutableListOf<HomeScrapedShow>()
            try {
                val doc = fetchDocWithBypass("https://e.3cktv.com/video/series/") ?: fetchDocOrThrow("https://e.3cktv.com/video/series/")
                
                val elements = doc.select("article.postEp, article.post").ifEmpty { doc.select(".block-post") }
                elements.forEach { el ->
                    val aTag = el.select("a").firstOrNull() ?: return@forEach
                    val link = aTag.attr("href")
                    if (!link.contains("/video/series/")) return@forEach
                    val title = el.select(".title").text().trim().ifEmpty { aTag.attr("title").replace(" قصة عشق", "").trim() }
                    val ribbon = el.select(".seasonName").text().trim().ifEmpty { null }
                    
                    val poster = extractPosterFromElement(el)
                    
                    list.add(HomeScrapedShow(title, link, ribbon, poster))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list
        }
    }

    private fun re_episode_number(title: String): String? {
        val cleanTitle = title
            .replace("مترجمة", "")
            .replace("مترجم", "")
            .replace("مدبلجة", "")
            .replace("مدبلج", "")
            .replace("كاملة", "")
            .replace("كامل", "")
            .trim()
        
        // Priority 1: Explicit patterns with Episode/Part
        val pattern = Regex("(?:الحلقة|حلقة|Episode|Ep|Part|P|Halka|الحلقه|حلقه)\\s*(\\d+)", RegexOption.IGNORE_CASE)
        val match = pattern.find(cleanTitle)
        if (match != null) return match.groupValues[1]
        
        // Priority 2: Standalone numbers that are NOT years (1900-2030)
        // We look for numbers from right to left as episodes are usually at the end
        val allMatches = Regex("\\b(\\d+)\\b").findAll(cleanTitle).toList()
        for (m in allMatches.asReversed()) {
            val num = m.groupValues[1]
            val n = num.toIntOrNull() ?: 0
            if (n in 1..2500) { // Most episodes are within this range
                // Check if it's NOT a year
                if (n < 1900 || n > 2030) return num
                // If it is a year but there are other numbers, continue searching
            }
        }
        
        // Final fallback: any number that doesn't look like a common year
        if (allMatches.isNotEmpty()) {
            val last = allMatches.last().groupValues[1]
            val ln = last.toIntOrNull() ?: 0
            if (ln < 1900 || ln > 2030) return last
        }
        return null
    }

    private fun unpackDeanEdwards(packed: String): String {
        try {
            val pStart = packed.indexOf("}('")
            if (pStart == -1) return ""
            
            val argsStr = packed.substring(pStart + 3)
            var pEnd = -1
            var escaped = false
            for (i in argsStr.indices) {
                if (escaped) {
                    escaped = false
                } else if (argsStr[i] == '\\') {
                    escaped = true
                } else if (argsStr[i] == '\'') {
                    pEnd = i
                    break
                }
            }
            if (pEnd == -1) return ""
            
            val p = argsStr.substring(0, pEnd)
            val remaining = argsStr.substring(pEnd + 1)
            
            val partsRegex = Regex("""\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*'(.*?)'\s*\.split""", kotlin.text.RegexOption.DOT_MATCHES_ALL)
            val match = partsRegex.find(remaining) ?: return ""
            
            val a = match.groupValues[1].toIntOrNull() ?: 36
            val c = match.groupValues[2].toIntOrNull() ?: 0
            val kStr = match.groupValues[3]
            val k = kStr.split('|')
            
            val cleanP = p.replace("\\'", "'").replace("\\\\", "\\")
            
            val wordPattern = Regex("\\b[a-zA-Z0-9_]+\\b")
            val replacements = HashMap<String, String>()
            
            fun baseN(num: Int, b: Int): String {
                val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                if (num == 0) return "0"
                var n = num
                val sb = StringBuilder()
                while (n > 0) {
                    sb.append(chars[n % b])
                    n /= b
                }
                return sb.reverse().toString()
            }
            
            for (i in 0 until c) {
                if (i < k.size && k[i].isNotEmpty()) {
                    val word = baseN(i, a)
                    replacements[word] = k[i]
                }
            }
            
            return wordPattern.replace(cleanP) { result ->
                val word = result.value
                replacements[word] ?: word
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    suspend fun extractDirectStreamUrl(embedUrl: String, referrer: String): String? {
        return withContext(Dispatchers.Default) {
            try {
                val doc = fetchDocOrThrow(embedUrl)
                
                val html = doc.html()
                val cleanHtml = html.replace("\\/", "/")
                
                val packedRegex = Regex("""eval\s*\(\s*function\s*\(\s*p\s*,\s*a\s*,\s*c\s*,\s*k\s*,\s*e\s*,\s*d\s*\).*?\}\s*\(.*?split.*?\)\s*\)""", kotlin.text.RegexOption.DOT_MATCHES_ALL)
                val packedMatch = packedRegex.find(html)
                
                val searchTarget = if (packedMatch != null) {
                    val unpacked = unpackDeanEdwards(packedMatch.value)
                    cleanHtml + "\n" + unpacked.replace("\\/", "/")
                } else {
                    cleanHtml
                }

                // 1. Check for explicit file: "https://..." in JS (like JWPlayer sources)
                val jwFileRegex = Regex("""file\s*:\s*["'](https?://[^\s"'`<>]+?\.(?:m3u8|mp4)[^\s"'`<>]*)["']""", RegexOption.IGNORE_CASE)
                val jwMatch = jwFileRegex.find(searchTarget)
                if (jwMatch != null) {
                    return@withContext jwMatch.groupValues[1]
                }
                
                // 2. Check for m3u8 regex
                val m3u8Regex = Regex("""(https?://[^\s"'`<>]+?\.m3u8[^\s"'`<>]*?)""", RegexOption.IGNORE_CASE)
                val m3u8Match = m3u8Regex.find(searchTarget)
                if (m3u8Match != null) {
                    return@withContext m3u8Match.value
                }
                
                // 3. Check for mp4 regex
                val mp4Regex = Regex("""(https?://[^\s"'`<>]+?\.mp4[^\s"'`<>]*?)""", RegexOption.IGNORE_CASE)
                val mp4Match = mp4Regex.find(searchTarget)
                if (mp4Match != null) {
                    return@withContext mp4Match.value
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            null
        }
    }

    suspend fun getServersForEpisode(episodeUrl: String): List<VideoServer> {
        return withContext(Dispatchers.Default) {
            val servers = mutableListOf<VideoServer>()
            try {
                if (episodeUrl.isBlank()) return@withContext emptyList()
                
                var targetUrl = episodeUrl
                if (targetUrl.contains("/series/") && !targetUrl.contains("-ep-")) {
                    val eps = getEpisodesForShow(targetUrl)
                    if (eps.isNotEmpty()) {
                        targetUrl = eps.first().link
                    }
                }

                if (targetUrl.contains("3cktv")) {
                    val watchUrl = if (!targetUrl.contains("do=watch")) {
                        if (targetUrl.contains("?")) "$targetUrl&do=watch" else "$targetUrl?do=watch"
                    } else targetUrl
                    targetUrl = watchUrl
                }

                val doc1 = fetchDocWithBypass(targetUrl, referrer = if (targetUrl.contains("3cktv")) "https://e.3cktv.com/" else "https://z.3cktv.com/")
                    ?: fetchDocOrThrow(targetUrl)

                // 0. Check for 3cktv direct iframe embed and server list
                if (targetUrl.contains("3cktv")) {
                    val primaryIframe = doc1.select(".postEmbed iframe, .singleInfo iframe, iframe[src*='embed'], iframe[src*='vid'], iframe[src*='stream']").firstOrNull()?.attr("src")
                    if (!primaryIframe.isNullOrBlank()) {
                        var iframeUrl = primaryIframe.trim()
                        if (iframeUrl.startsWith("//")) iframeUrl = "https:$iframeUrl"
                        val serverName = when {
                            iframeUrl.contains("vidsp") -> "سيرفر Vidsp ⚡"
                            iframeUrl.contains("vinovo") -> "سيرفر Vinovo ⚡"
                            iframeUrl.contains("vids") -> "سيرفر Vids ⚡"
                            else -> "سيرفر 1 (الأساسي) ⚡"
                        }
                        servers.add(VideoServer(name = serverName, url = iframeUrl))
                    }
                    servers.add(VideoServer(name = "مشغل حكايتنا المباشر 🌐", url = targetUrl))
                }

                // Check Fosta servers via play.php
                if (targetUrl.contains("ffosta-tv") || targetUrl.contains("fosta")) {
                    val vid = targetUrl.substringAfter("vid=").substringBefore("&")
                    val playUrl = if (vid.isNotEmpty() && !targetUrl.contains("play.php")) {
                        "https://ffosta-tv.quest/play.php?vid=$vid"
                    } else targetUrl

                    val playDoc = fetchDocWithBypass(playUrl, referrer = "https://ffosta-tv.quest/")
                        ?: fetchDocOrThrow(playUrl)

                    val serverLis = playDoc.select(".WatchList li[data-embed-url], li[data-embed-url]")
                    serverLis.forEachIndexed { idx, li ->
                        val embed = li.attr("data-embed-url").trim()
                        val rawName = li.text().trim()
                        if (embed.isNotBlank() && !embed.startsWith("javascript:") && servers.none { it.url == embed }) {
                            val serverName = when {
                                rawName.contains("1vid", ignoreCase = true) -> "🚀 سيرفر 1vid (سريع جداً HD)"
                                rawName.contains("Vidmoly", ignoreCase = true) -> "⚡ سيرفر Vidmoly (عالي الدقة FHD)"
                                rawName.contains("Vidspeed", ignoreCase = true) -> "⚡ سيرفر Vidspeed"
                                rawName.contains("Vkvideo", ignoreCase = true) -> "🌐 سيرفر Vkvideo"
                                rawName.contains("Ok", ignoreCase = true) -> "🌐 سيرفر Ok Ru"
                                rawName.contains("Bysesukior", ignoreCase = true) -> "⚡ سيرفر Bysesukior"
                                rawName.contains("Voe", ignoreCase = true) -> "⚡ سيرفر Voe"
                                rawName.contains("Hgcloud", ignoreCase = true) -> "☁️ سيرفر Hgcloud"
                                rawName.contains("Uqload", ignoreCase = true) -> "⚡ سيرفر Uqload"
                                rawName.contains("Mp4plus", ignoreCase = true) -> "🎬 سيرفر Mp4plus"
                                rawName.contains("Playmogo", ignoreCase = true) -> "▶️ سيرفر Playmogo"
                                else -> "سيرفر ${rawName.ifEmpty { (idx + 1).toString() }}"
                            }
                            servers.add(VideoServer(name = serverName, url = embed))
                        }
                    }

                    val iframes = playDoc.select("iframe[src]")
                    for (ifr in iframes) {
                        val iSrc = ifr.attr("src").trim()
                        if (iSrc.isNotBlank() && !iSrc.startsWith("javascript:") && servers.none { it.url == iSrc }) {
                            servers.add(VideoServer(name = "سيرفر خارجي HD ⚡", url = iSrc))
                        }
                    }

                    servers.add(VideoServer(name = "مشغل حكايتنا المباشر 🌐", url = playUrl))

                    if (servers.isNotEmpty()) {
                        return@withContext servers
                    }
                }

                // 1. Check for single-watch-btn with encoded post JSON containing all streaming servers
                val watchBtns = doc1.select("a.single-watch-btn, a#btnWatch, .single_buttons a, a[href*='post='], a[href*='lahda.site']")
                for (btn in watchBtns) {
                    val href = btn.attr("href")
                    if (href.contains("post=")) {
                        val postParam = href.substringAfter("post=").substringBefore("&")
                        try {
                            val decodedBytes = base64Decode(postParam)
                            val decodedJson = decodedBytes.decodeToString()
                            val map = parseJsonMap(decodedJson)
                            var sCount = servers.size + 1
for ((key, serverUrl) in map) {
                                if (serverUrl.isNotEmpty() && !serverUrl.startsWith("javascript:") && servers.none { it.url == serverUrl }) {
                                    val serverName = when (key.lowercase()) {
                                        "v", "vidsp" -> "سيرفر Vidsp"
                                        "vinovo" -> "سيرفر Vinovo"
                                        "vids" -> "سيرفر Vids"
                                        "vidaraa" -> "سيرفر Vidaraa"
                                        "luluvdo" -> "سيرفر Luluvdo"
                                        else -> "سيرفر $sCount"
                                    }
                                    servers.add(VideoServer(name = serverName, url = serverUrl))
                                    sCount++
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // Check Alooytv video links & extract all direct CDN and embed servers
                if (targetUrl.contains("alooytv")) {
                    var episodeDoc = doc1
                    // If the page doesn't contain a key, find the first episode link and fetch its page
                    if (!targetUrl.contains("key=")) {
                        val firstEpKeyLink = doc1.select("a[href*='key='], .season a.btn-ep, .season a").firstOrNull()?.attr("href")
                        if (!firstEpKeyLink.isNullOrBlank()) {
                            var epFullUrl = firstEpKeyLink.trim()
                            if (epFullUrl.startsWith("/")) epFullUrl = "https://ep.alooytv16.xyz$epFullUrl"
                            val fetched = fetchDocWithBypass(epFullUrl) ?: try {
                                fetchDocOrThrow(epFullUrl)
                            } catch (e: Exception) { null }
                            if (fetched != null) {
                                episodeDoc = fetched
                            }
                        }
                    }

                    val epHtml = episodeDoc.html()
                    val directSources = mutableListOf<String>()
                    var workerServerUrl: String? = null

                    // 1. Check HTML video sources
                    val vElements = episodeDoc.select("video source[src], video[src]")
                    for (v in vElements) {
                        var vSrc = v.attr("src").trim()
                        if (vSrc.startsWith("//")) vSrc = "https:$vSrc"
                        else if (vSrc.startsWith("/")) vSrc = "https://ep.alooytv16.xyz$vSrc"
                        if (vSrc.startsWith("http") && vSrc != "0" && vSrc != "MA==" && vSrc.length > 5 && !directSources.contains(vSrc)) {
                            directSources.add(vSrc)
                        }
                    }

                    // 2. Decode download button base64 URL or fetch download_video.php directly
                    val downloadBtns = episodeDoc.select("a[href*='download_video.php'], a[href*='video_url=']")
                    for (btn in downloadBtns) {
                        val href = btn.attr("href").trim()
                        var dlFullUrl = if (href.startsWith("/")) "https://ep.alooytv16.xyz$href" else href
                        if (dlFullUrl.contains("video_url=")) {
                            val param = dlFullUrl.substringAfter("video_url=").substringBefore("&")
                            try {
                                val decoded = base64Decode(param).decodeToString().trim()
                                if (decoded.startsWith("http") && decoded != "0" && decoded != "MA==" && decoded.length > 5 && !directSources.contains(decoded)) {
                                    directSources.add(decoded)
                                }
                            } catch (e: Exception) {}
                        }
                        
                        // Fetch download_video.php to extract Cloudflare worker CDN stream
                        try {
                            val dlDoc = fetchDocWithBypass(dlFullUrl) ?: fetchDocOrThrow(dlFullUrl)
                            val workerLink = dlDoc.select("a[href*='workers.dev'], a[href*='token='], a.btn-danger").attr("href").trim()
                            if (workerLink.startsWith("http") && workerServerUrl == null) {
                                workerServerUrl = workerLink
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    // 3. Extract direct mp4 from scripts/text
                    val directMatches = Regex("""https?://[^\s"'`<>]+?\.(mp4|m3u8|webm)(\?[^\s"'`<>]*)?""", RegexOption.IGNORE_CASE).findAll(epHtml)
                    for (m in directMatches) {
                        val sUrl = m.value.trim()
                        if (sUrl != "0" && sUrl != "MA==" && sUrl.length > 5 && !directSources.contains(sUrl)) {
                            directSources.add(sUrl)
                        }
                    }

                    val primaryVideo = directSources.firstOrNull()

                    // Ensure high-speed Cloudflare Worker CDN link is extracted
                    if (workerServerUrl == null && primaryVideo != null) {
                        try {
                            val b64 = base64Encode(primaryVideo.encodeToByteArray())
                            val dlUrl = "https://ep.alooytv16.xyz/download_video.php?video_url=$b64"
                            val dlDoc = fetchDocWithBypass(dlUrl) ?: fetchDocOrThrow(dlUrl)
                            val workerLink = dlDoc.select("a[href*='workers.dev'], a[href*='token='], a.btn-danger").attr("href").trim()
                            if (workerLink.startsWith("http")) {
                                workerServerUrl = workerLink
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    // 4. Build Servers list!
                    // Priority 1: High-Speed Cloudflare Worker Server (if available)
                    if (workerServerUrl != null) {
                        servers.add(VideoServer(name = "🚀 سيرفر 1 (سيرفر فائق السرعة Cloudflare)", url = workerServerUrl))
                    }

                    // Priority 2: Direct CDN Video & its Mirrors
                    if (primaryVideo != null) {
                        val cdnMatch = Regex("""https?://(?:vid\d+|w\d+)\.([^/]+)/(.*\.(?:mp4|jpg|webm).*)""", RegexOption.IGNORE_CASE).find(primaryVideo)
                        if (cdnMatch != null) {
                            val host = cdnMatch.groupValues[1]
                            val path = cdnMatch.groupValues[2]
                            
                            val sName1 = if (workerServerUrl != null) "⚡ سيرفر 2 (سيرفر أساسي مباشر)" else "⚡ سيرفر 1 (سيرفر أساسي مباشر)"
                            servers.add(VideoServer(name = sName1, url = primaryVideo))
                            
                            val baseVid1 = "https://vid1.$host/$path"
                            val baseVid2 = "https://vid2.$host/$path"
                            val baseVid3 = "https://vid3.$host/$path"
                            val baseVid4 = "https://vid4.$host/$path"
                            val baseVid5 = "https://vid5.$host/$path"
                            val baseVid7 = "https://vid7.$host/$path"
                            val baseVid8 = "https://vid8.$host/$path"
                            val baseVid9 = "https://vid9.$host/$path"
                            val baseVid10 = "https://vid10.$host/$path"
                            
                            var mCount = if (workerServerUrl != null) 3 else 2
                            if (baseVid2 != primaryVideo && servers.none { it.url == baseVid2 }) {
                                servers.add(VideoServer(name = "🚀 سيرفر $mCount (فائق السرعة HD)", url = baseVid2))
                                mCount++
                            }
                            if (baseVid1 != primaryVideo && servers.none { it.url == baseVid1 }) {
                                servers.add(VideoServer(name = "⚡ سيرفر $mCount (سريع FHD 1080p)", url = baseVid1))
                                mCount++
                            }
                            if (baseVid5 != primaryVideo && servers.none { it.url == baseVid5 }) {
                                servers.add(VideoServer(name = "⚡ سيرفر $mCount (خادم بديل 1)", url = baseVid5))
                                mCount++
                            }
                            if (baseVid7 != primaryVideo && servers.none { it.url == baseVid7 }) {
                                servers.add(VideoServer(name = "⚡ سيرفر $mCount (خادم بديل 2)", url = baseVid7))
                                mCount++
                            }
                            if (baseVid8 != primaryVideo && servers.none { it.url == baseVid8 }) {
                                servers.add(VideoServer(name = "⚡ سيرفر $mCount (بث مباشر عالي)", url = baseVid8))
                                mCount++
                            }
                            if (baseVid9 != primaryVideo && servers.none { it.url == baseVid9 }) {
                                servers.add(VideoServer(name = "⚡ سيرفر $mCount (خادم سريع 3)", url = baseVid9))
                                mCount++
                            }
                            if (baseVid3 != primaryVideo && servers.none { it.url == baseVid3 }) {
                                servers.add(VideoServer(name = "⚡ سيرفر $mCount (مباشر CDN)", url = baseVid3))
                                mCount++
                            }
                        } else {
                            val sName1 = if (workerServerUrl != null) "⚡ سيرفر 2 (سريع FHD)" else "⚡ سيرفر 1 (سريع FHD)"
                            servers.add(VideoServer(name = sName1, url = primaryVideo))
                            var idx = if (workerServerUrl != null) 3 else 2
                            for (i in 1 until directSources.size) {
                                if (servers.none { it.url == directSources[i] }) {
                                    servers.add(VideoServer(name = "⚡ سيرفر $idx (مباشر HD)", url = directSources[i]))
                                    idx++
                                }
                            }
                        }
                    }

                    // 5. Check iframes and watch buttons
                    val alooyWatchBtns = episodeDoc.select("a[href*='watch.php'], a[href*='player.php'], a[href*='embed.php'], a.btn-server, .servers a, .server-item a")
                    for (btn in alooyWatchBtns) {
                        var href = btn.attr("href").trim()
                        if (href.startsWith("//")) href = "https:$href"
                        else if (href.startsWith("/")) href = "https://ep.alooytv16.xyz$href"
                        if (href.startsWith("http") && !href.contains(".0") && servers.none { it.url == href }) {
                            val rawText = btn.text().trim()
                            val sName = if (rawText.isNotEmpty()) "سيرفر $rawText" else "سيرفر خارجي"
                            servers.add(VideoServer(name = sName, url = href))
                        }
                    }

                    val iframes = episodeDoc.select("iframe[src]")
                    for (ifr in iframes) {
                        var ifrSrc = ifr.attr("src").trim()
                        if (ifrSrc.startsWith("//")) ifrSrc = "https:$ifrSrc"
                        else if (ifrSrc.startsWith("/")) ifrSrc = "https://ep.alooytv16.xyz$ifrSrc"
                        if (ifrSrc.startsWith("http") && !ifrSrc.contains(".0") && servers.none { it.url == ifrSrc }) {
                            servers.add(VideoServer(name = "مشغل الويب الرسمي 🌐", url = ifrSrc))
                        }
                    }

                    // Always add the web player as a reliable fallback
                    if (servers.none { it.url == targetUrl }) {
                        servers.add(VideoServer(name = "مشغل الويب الأصلي 🌐", url = targetUrl))
                    }

                    if (servers.isNotEmpty()) {
                        return@withContext servers
                    }
                }

                val embedUrl = doc1.select("link[itemprop=embedURL]").attr("href")
                if (embedUrl.isNotEmpty()) {
                    servers.add(VideoServer(name = "سيرفر 1", url = embedUrl))
                }

                val servList = doc1.select(".servList li[data-server]")
                if (servList.isNotEmpty()) {
                    var sCount = 1
                    servList.forEach { li ->
                        val dataServer = li.attr("data-server")
                        val iframeMatch = Regex("""src=['"]([^'"]+)['"]""").find(dataServer)
                        var sUrl = iframeMatch?.groupValues?.get(1) ?: ""
                        if (sUrl.isEmpty()) {
                            val generalMatch = Regex("""https?://[^\s"'`<>]+""").find(dataServer)
                            sUrl = generalMatch?.value ?: ""
                        }
                        if (sUrl.isNotEmpty()) {
                            if (sUrl.startsWith("//")) sUrl = "https:$sUrl"
                            val serverName = "سيرفر $sCount"
                            if (servers.none { it.url == sUrl }) {
                                servers.add(VideoServer(name = serverName, url = sUrl))
                                sCount++
                            }
                        }
                    }
                    if (servers.isNotEmpty()) return@withContext servers
                }
                
                val iframe1 = doc1.select(".postEmbed iframe").firstOrNull()
                    ?: doc1.select(".singleInfo iframe").firstOrNull()
                    ?: doc1.select(".getEmbed iframe").firstOrNull() 
                    ?: doc1.select(".watch iframe").firstOrNull()
                    ?: doc1.select("iframe[src]").firstOrNull()
                
                var src1 = iframe1?.attr("src")
                if (!src1.isNullOrEmpty()) {
                    if (src1.startsWith("//")) {
                        src1 = "https:$src1"
                    } else if (src1.startsWith("/")) {
                        val _uriSrc = episodeUrl
                        src1 = "${schemeOf(_uriSrc)}://${hostOf(_uriSrc) ?: ""}$src1"
                    }

                    try {
                        val doc2 = fetchDocWithBypass(src1, episodeUrl) ?: fetchDocOrThrow(src1)

                        val menuLinks = doc2.select("ul.aplr-menu li a, .aplr-menu a, ul.servList li a, .servers-list a, .server-item a, div.servers a, ul.nav-tabs a")
                        if (menuLinks.isNotEmpty()) {
                            var count = 1
                            menuLinks.forEach { aTag ->
                                var href = aTag.attr("href")
                                val tagId = aTag.id()
                                if (href.isNotEmpty() && !href.startsWith("javascript:") && !href.contains("reload") && tagId != "refresh" && tagId != "share") {
                                    if (href.startsWith("//")) {
                                        href = "https:$href"
                                    } else if (href.startsWith("/")) {
                                        val _uriSrc = src1
                                        href = "${schemeOf(_uriSrc)}://${hostOf(_uriSrc) ?: ""}$href"
                                    }
                                    val serverName = "سيرفر $count"
                                    if (servers.none { it.url == href }) {
                                        servers.add(VideoServer(name = serverName, url = href))
                                        count++
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (servers.isEmpty()) {
                        servers.add(VideoServer(name = "سيرفر 1", url = src1))
                    }
                    return@withContext servers
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (servers.isEmpty()) {
                if (episodeUrl.isNotEmpty()) {
                    servers.add(VideoServer(name = "سيرفر 1", url = episodeUrl))
                }
            }

            servers
        }
    }

    suspend fun resolveServerDirectUrl(serverUrl: String, referrer: String): String? {
        return withContext(Dispatchers.Default) {
            try {
                if (serverUrl.isBlank()) return@withContext null
                // Proxied URLs, Cloudflare workers, or direct video files don't need redundant scraping
                if (serverUrl.startsWith("http://127.0.0.1") || serverUrl.contains("/stream?url=") || serverUrl.contains("workers.dev")) {
                    return@withContext serverUrl
                }
                if (serverUrl.endsWith(".mp4") || serverUrl.contains(".mp4?") || serverUrl.contains(".m3u8") || serverUrl.contains(".webm") ||
                    (serverUrl.endsWith(".jpg") && (serverUrl.contains("/file/") || serverUrl.contains("alooy") || serverUrl.contains("upload")))) {
                    return@withContext serverUrl
                }

                var targetUrl = serverUrl
                if (targetUrl.contains("vidsp.net") && !targetUrl.contains("embed-") && !targetUrl.contains("/e/")) {
                    targetUrl = targetUrl.replace("vidsp.net/", "vidsp.net/e/")
                }

                val doc = fetchDocWithBypass(targetUrl, if (targetUrl.contains("3cktv")) null else referrer)
                    ?: fetchDocOrThrow(targetUrl)

                // Check for direct video source tags
                val videoSrc = doc.select("video source[src], video[src]").firstOrNull()?.attr("src")
                if (!videoSrc.isNullOrEmpty()) {
                    var vUrl = videoSrc
                    if (vUrl.startsWith("//")) vUrl = "https:$vUrl"
                    else if (vUrl.startsWith("/")) {
                        val _uriSrc = targetUrl
                        vUrl = "${schemeOf(_uriSrc)}://${hostOf(_uriSrc) ?: ""}$vUrl"
                    }
                    return@withContext vUrl
                }

                // Check for Base64 encoded video_url in links
                val downloadLink = doc.select("a[href*='video_url='], a[href*='download_video.php']").firstOrNull()?.attr("href")
                if (downloadLink != null && downloadLink.contains("video_url=")) {
                    val param = downloadLink.substringAfter("video_url=").substringBefore("&")
                    try {
                        val decoded = base64Decode(param).decodeToString()
                        if (decoded.startsWith("http")) return@withContext decoded
                    } catch (e: Exception) {}
                }

                // Check for mp4 or m3u8 in html text
                val htmlContent = doc.html()
                val directMatch = Regex("""https?://[^\s"'`<>]+?\.(mp4|m3u8)(\?[^\s"'`<>]*)?""", RegexOption.IGNORE_CASE).find(htmlContent)
                if (directMatch != null) {
                    return@withContext directMatch.value
                }
                
                val iframe = doc.select("#iframe").firstOrNull() 
                    ?: doc.select(".video-con iframe").firstOrNull()
                    ?: doc.select(".postEmbed iframe").firstOrNull()
                    ?: doc.select(".singleInfo iframe").firstOrNull()
                    ?: doc.select("iframe[src]").firstOrNull()

                var src = iframe?.attr("src")
                if (src != null && src.isNotEmpty()) {
                    if (src.startsWith("//")) {
                        src = "https:$src"
                    } else if (src.startsWith("/")) {
                        val _uriSrc = targetUrl
                        src = "${schemeOf(_uriSrc)}://${hostOf(_uriSrc) ?: ""}$src"
                    }
                    if (src.contains("vidsp.net") && !src.contains("embed-") && !src.contains("/e/")) {
                        src = src.replace("vidsp.net/", "vidsp.net/e/")
                    }
                    return@withContext src
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (serverUrl.contains("vidsp.net") && !serverUrl.contains("embed-") && !serverUrl.contains("/e/")) {
                return@withContext serverUrl.replace("vidsp.net/", "vidsp.net/e/")
            }
            serverUrl
        }
    }

    suspend fun getVideoUrlForEpisode(episodeUrl: String): String? {
        return withContext(Dispatchers.Default) {
            try {
                if (episodeUrl.isBlank()) return@withContext null
                
                if (episodeUrl.contains("alooytv")) {
                    val servers = getServersForEpisode(episodeUrl)
                    if (servers.isNotEmpty()) {
                        return@withContext servers.first().url
                    }
                }
                
                if (episodeUrl.contains("3cktv")) {
                    val watchUrl = if (!episodeUrl.contains("do=watch")) {
                        if (episodeUrl.contains("?")) "$episodeUrl&do=watch" else "$episodeUrl?do=watch"
                    } else episodeUrl

                    val doc = fetchDocWithBypass(watchUrl, referrer = "https://e.3cktv.com/")
                        ?: fetchDocOrThrow(watchUrl)

                    val iframe = doc.select(".postEmbed iframe, .singleInfo iframe, iframe[src*='embed'], iframe[src*='vid'], iframe[src*='stream']").firstOrNull()?.attr("src")
                    if (!iframe.isNullOrBlank()) {
                        var cleanIframe = iframe.trim()
                        if (cleanIframe.startsWith("//")) cleanIframe = "https:$cleanIframe"
                        return@withContext cleanIframe
                    }
                    return@withContext watchUrl
                }

                val targetUrl = episodeUrl
                val doc1 = fetchDocWithBypass(targetUrl, referrer = "https://z.3cktv.com/")
                    ?: fetchDocOrThrow(targetUrl)

                // Check for single-watch-btn with encoded post JSON containing direct servers
                val watchBtns = doc1.select("a.single-watch-btn, a#btnWatch, .single_buttons a, a[href*='post='], a[href*='lahda.site']")
                for (btn in watchBtns) {
                    val href = btn.attr("href")
                    if (href.contains("post=")) {
                        val postParam = href.substringAfter("post=").substringBefore("&")
                        try {
                            val decodedBytes = base64Decode(postParam)
                            val decodedJson = decodedBytes.decodeToString()
                            val map = parseJsonMap(decodedJson)
for ((key, serverUrl) in map) {
                                if (serverUrl.isNotEmpty() && !serverUrl.startsWith("javascript:")) {
                                    return@withContext serverUrl
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                val embedUrl = doc1.select("link[itemprop=embedURL]").attr("href")
                if (embedUrl.isNotEmpty()) {
                    return@withContext embedUrl
                }

                val firstDataServer = doc1.select(".servList li[data-server]").firstOrNull()?.attr("data-server")
                if (!firstDataServer.isNullOrBlank()) {
                    val iframeMatch = Regex("""src=['"]([^'"]+)['"]""").find(firstDataServer)
                    var sUrl = iframeMatch?.groupValues?.get(1) ?: ""
                    if (sUrl.isEmpty()) {
                        val generalMatch = Regex("""https?://[^\s"'`<>]+""").find(firstDataServer)
                        sUrl = generalMatch?.value ?: ""
                    }
                    if (sUrl.isNotEmpty()) {
                        if (sUrl.startsWith("//")) sUrl = "https:$sUrl"
                        return@withContext sUrl
                    }
                }
                
                val iframe1 = doc1.select(".postEmbed iframe").firstOrNull()
                    ?: doc1.select(".singleInfo iframe").firstOrNull()
                    ?: doc1.select(".getEmbed iframe").firstOrNull() 
                    ?: doc1.select(".watch iframe").firstOrNull()
                    ?: doc1.select("iframe[src]").firstOrNull()
                
                var src1 = iframe1?.attr("src")
                if (!src1.isNullOrEmpty()) {
                    if (src1.startsWith("//")) {
                        src1 = "https:$src1"
                    } else if (src1.startsWith("/")) {
                        val _uriSrc = episodeUrl
                        src1 = "${schemeOf(_uriSrc)}://${hostOf(_uriSrc) ?: ""}$src1"
                    }
                    return@withContext src1
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (episodeUrl.isNotEmpty()) {
                return@withContext episodeUrl
            }
            null
        }
    }
}
