package com.agon.app.data

import androidx.compose.ui.graphics.Color

data class ShowArt(val c1: Color, val c2: Color, val accent: Color)

object Catalog {
    private val artMap = mutableMapOf<String, ShowArt>()

    fun artFor(id: String): ShowArt = artMap[id] ?: ShowArt(
        Color(0xFF2B2B33), Color(0xFF0B0B0F), Color(0xFFE50914)
    )

    fun cleanSeriesTitle(rawTitle: String): String {
        if (rawTitle.isBlank()) return ""
        var title = rawTitle
            .replace("فوستا تي في", "")
            .replace("فوستا TV", "", ignoreCase = true)
            .replace("فوستا tv", "", ignoreCase = true)
            .replace("فوستا", "")
            .replace("fosta-tv", "", ignoreCase = true)
            .replace("fosta", "", ignoreCase = true)
            .replace("قصة عشق", "")
            .replace("قناة", "")
            .replace("مترجمة للعربية", "")
            .replace("مدبلجة للعربية", "")
            .replace("مترجم للعربية", "")
            .replace("مدبلج للعربية", "")
            .replace("مترجمة", "")
            .replace("مدبلجة", "")
            .replace("مترجم", "")
            .replace("مدبلج", "")
            .replace("للعربية", "")
            .replace("العربية", "")
            .replace("مسلسل", "")
            .replace("برنامج", "")
            .replace("انمي", "")
            .replace("شاهد", "")
            .replace("موقع", "")
            .replace("تحميل", "")
            .replace("اون لاين", "")
            .replace("اونلاين", "")
            .replace("كاملة", "")
            .replace("كامل", "")
            .replace("بجودة", "")
            .replace("عالية", "")
            .replace("HD", "")
            .replace("hd", "")
            .replace("FHD", "")
            .replace("fhd", "")
            .replace("4K", "")
            .replace("4k", "")
            .replace("SD", "")
            .replace("sd", "")
            .replace("الأخيرة", "")
            .replace("الاخيرة", "")
            .replace("نهاية الموسم", "")
            .replace(Regex("الحلقة\\s*\\d+.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("حلقة\\s*\\d+.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("الحلقه\\s*\\d+.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("حلقه\\s*\\d+.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("الموسم\\s*\\d+.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("موسم\\s*\\d+.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("الجزء\\s*\\d+.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("جزء\\s*\\d+.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("ep\\s*\\d+.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-', '•', '·')
            .trim()

        // Don't remove 'فيلم' if it's the only word or it's actually a movie title
        if (rawTitle.contains("فيلم") || rawTitle.contains("فلم")) {
            if (title.isBlank()) return rawTitle
            if (!title.contains("فيلم") && !title.contains("فلم")) {
                // Keep 'فيلم' if it was in the original and the remaining title is short
                if (title.length < 10) return "فيلم $title"
            }
        }

        if (title.isBlank()) return rawTitle
        return title
    }

    fun isSeriesLink(link: String): Boolean {
        val l = link.lowercase()
        return l.contains("/series/") || 
               l.contains("view-serie.php") || 
               l.contains("moslslat") || 
               l.contains("serie") ||
               l.contains("musalsal") ||
               l.contains("season") ||
               (l.contains("alooytv") && !l.contains("/watch/"))
    }

    fun normalizeTitle(text: String): String {
        if (text.isBlank()) return ""
        return text.lowercase()
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .replace('ئ', 'ي')
            .replace('ؤ', 'و')
            // Remove common prefixes/suffixes for better matching
            .replace("مسلسل", "")
            .replace("فيلم", "")
            .replace("فلم", "")
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun sanitizeShowToSeries(show: Show): Show {
        val cleanTitle = cleanSeriesTitle(show.title)
        val isExplicitMovie = show.title.contains("فيلم") || show.title.contains("فلم") || show.link.contains("watch.php")
        val isExplicitSeries = show.title.contains("مسلسل") || isSeriesLink(show.link)
        
        val finalIsMovie = when {
            isExplicitMovie && !isExplicitSeries -> true
            isExplicitSeries -> false
            else -> show.isMovie
        }
        
        return show.copy(
            title = cleanTitle,
            isMovie = finalIsMovie,
            rating = if (finalIsMovie) "فيلم" else if (show.rating == "فيلم") "عربي" else show.rating
        )
    }

    fun isGeneralGreetingOrConversation(msg: String): Boolean {
        val clean = normalizeTitle(msg)
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .trim()
            .lowercase()

        if (clean.isBlank()) return true

        val greetings = setOf(
            "سلام", "سلام عليكم", "السلام عليكم", "سلام عليكم ورحمة الله", "مرحبا", "أهلا", "اهلاً",
            "أهلاً", "اهلين", "أهلين", "هلا", "هلا والله", "هلا فيك", "كيفك", "كيف الحال", "كيف حالك",
            "اخبارك", "أخبارك", "علومك", "صباح الخير", "مساء الخير", "شكرا", "شكراً", "تسلم", "يعطيك العافية",
            "من انت", "مين انت", "من تكون", "اسمك", "شنو اسمك", "ما اسمك", "هههه", "ههههه", "هههههه",
            "حي على الفلاح", "منورة", "منور", "شكرا لك", "تسلم ايدك"
        )

        if (greetings.contains(clean)) return true

        val generalWords = listOf("سلام", "مرحبا", "هلا", "كيفك", "اخبارك", "من انت", "اسمك", "شكرا", "هههه")
        val searchKeywords = listOf("مسلسل", "فيلم", "فلم", "شاهد", "قصة", "شورت", "تقييم", "حلقة")

        if (generalWords.any { clean.startsWith(it) || clean.endsWith(it) }) {
            if (searchKeywords.none { clean.contains(it) }) {
                return true
            }
        }

        return false
    }

    fun isFollowUpAboutContextShow(msg: String): Boolean {
        val clean = normalizeTitle(msg).lowercase()
        val followUpKeywords = listOf(
            "تقييم", "تقييمه", "تقييمك", "تقييم عمل", "كم تقييم", "كم تقييمه", "كم تقييمك",
            "قصة", "قصته", "قصة عمل", "وش قصته", "ما هي قصته", "ماهي قصته", "شنو قصته",
            "ابطال", "أبطال", "ابطاله", "أبطاله", "مين الابطال", "من الأبطال", "مين أبطاله",
            "مخرج", "مخرجه", "سنة", "سنة كم", "متى نزل", "كم حلقة", "عدد الحلقات",
            "زر", "اخش", "ادخل", "اشوف", "شاهد", "مشاهدة", "رابط", "وين", "وين القاه",
            "عطني اياه", "عطني إياه", "ابيه", "أبيه", "كيف اشوفه", "كيف ألقاه", "رايك فيه", "رأيك فيه"
        )
        return followUpKeywords.any { clean.contains(it) }
    }


    fun findMatchingShow(query: String, allShows: List<Show>): Show? {
        if (query.isBlank()) return null
        if (isGeneralGreetingOrConversation(query)) return null

        val normQuery = normalizeTitle(query)

        val stopWords = listOf(
            "مسلسل", "مسلسلات", "فيلم", "فلم", "أفلام", "افلام", "قصة", "قصه", "قصته",
            "عطني", "عطيني", "احكي", "ماهو", "ماهي", "ما هي", "ما هو", "معلومات عن", "عن",
            "ابطال", "أبطال", "بطل", "حكيم", "ممكن", "ابي", "اريد", "بغيت", "شوف", "شاهد",
            "كم", "تقييم", "تقييمه", "تقييمك", "عمل", "عن عمل", "رأيك", "رايك", "كيف", "وين", "عليكم", "السلام"
        )

        var cleanQuery = normQuery
        stopWords.forEach { word ->
            cleanQuery = cleanQuery.replace(word, " ")
        }
        cleanQuery = cleanQuery.replace(Regex("""[^\p{L}\p{N}\s]"""), " ").trim()
        while (cleanQuery.contains("  ")) {
            cleanQuery = cleanQuery.replace("  ", " ")
        }

        if (cleanQuery.isBlank() || cleanQuery.length < 2) return null

        val exact = allShows.firstOrNull { show ->
            if (isTashShow(show.title, show.link, show.id)) return@firstOrNull false
            val norm = normalizeTitle(show.title)
            norm.equals(cleanQuery, ignoreCase = true) || norm.equals(normQuery, ignoreCase = true)
        }
        if (exact != null) return exact

        if (cleanQuery.length >= 3) {
            val sub = allShows.firstOrNull { show ->
                if (isTashShow(show.title, show.link, show.id)) return@firstOrNull false
                val norm = normalizeTitle(show.title)
                norm.equals(cleanQuery, ignoreCase = true) || norm.contains(cleanQuery, ignoreCase = true)
            }
            if (sub != null) return sub
        }

        val words = cleanQuery.split(" ").filter { it.length >= 3 }
        if (words.isNotEmpty()) {
            val candidates = allShows.filterNot { isTashShow(it.title, it.link, it.id) }.mapNotNull { show ->
                val norm = normalizeTitle(show.title)
                val matchCount = words.count { norm.contains(it, ignoreCase = true) }
                if (matchCount >= 1) Pair(show, matchCount) else null
            }

            val best = candidates.maxByOrNull { it.second }
            if (best != null) {
                val norm = normalizeTitle(best.first.title)
                if (words.any { norm.contains(it, ignoreCase = true) && norm.length <= it.length + 15 }) {
                    return best.first
                }
            }
        }
        return null
    }

    fun isTashShow(title: String, link: String = "", id: String = ""): Boolean {
        if (title.isBlank() && link.isBlank() && id.isBlank()) return false
        
        val t = title.lowercase()
        if (t.contains("طاش ما طاش") || 
            t.contains("طاش مطاش") || 
            t.contains("طاش ماطاش") || 
            t.contains("طاش العودة") ||
            t.contains("طاش_ما_طاش")) {
            return true
        }
        
        val words = title.split(Regex("[\\s_\\-–—:،,.]+"))
        if (words.any { it == "طاش" || it == "وطاش" || it == "لطاش" || it == "بطاش" }) {
            return true
        }
        
        val lowerId = id.lowercase()
        val lowerLink = link.lowercase()
        if (lowerId.contains("tash_ma_tash") || lowerId.contains("tash-ma-tash") || lowerId.contains("tashmatash")) {
            return true
        }
        if (lowerLink.contains("tash-ma-tash") || lowerLink.contains("tash_ma_tash") || lowerLink.contains("tashmatash") ||
            lowerLink.contains("%d8%b7%d8%a7%d8%b4")) {
            return true
        }
        
        return false
    }

    private fun show(
        id: String, title: String, year: String, rating: String, imdb: Double,
        genres: List<String>, desc: String, dur: String,
        c1: Long, c2: Long, accent: Long,
        top10: Boolean = false, isNew: Boolean = false,
        cast: List<String> = emptyList(), poster: String = "", backdrop: String = "",
        link: String = ""
    ): Show {
        artMap[id] = ShowArt(Color(c1), Color(c2), Color(accent))
        val cleanedPoster = TmdbHelper.cleanPosterUrl(poster)
        val cleanedBackdrop = TmdbHelper.cleanPosterUrl(backdrop)
        return Show(
            id = id, title = title, year = year, rating = rating, imdb = imdb,
            genres = genres, description = desc, poster = cleanedPoster, backdrop = cleanedBackdrop,
            duration = dur, isTop10 = top10, isNew = isNew,
            cast = cast, link = link
        )
    }

    val all: List<Show> = listOf(
        // ==========================================
        // 0. مسلسلات وأفلام حصرية على حكايتنا ★
        // ==========================================
        show(
            id = "fosta_khamsin_khamsin",
            title = "مسلسل خمسين خمسين",
            year = "2025",
            rating = "عربي",
            imdb = 8.7,
            genres = listOf("مسلسلات عربية", "كوميدي", "دراما"),
            desc = "مسلسل الكوميديا والدراما المصري خمسين خمسين بطولة كندة علوش وانتصار وياسمينا العبد بجودة عالية HD على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF2A1C11, c2 = 0xFF0D0805, accent = 0xFFE50914,
            top10 = true, isNew = true,
            poster = "https://ffosta-tv.quest/uploads/thumbs/e8553d4d0-1.jpg",
            backdrop = "https://ffosta-tv.quest/uploads/thumbs/e8553d4d0-1.jpg",
            link = "https://ffosta-tv.quest/view-serie.php?name=khamsin-khamsin-series"
        ),
        show(
            id = "fosta_tale_nazel",
            title = "مسلسل طالع نازل",
            year = "2026",
            rating = "عربي",
            imdb = 8.5,
            genres = listOf("مسلسلات عربية", "دراما", "تشويق"),
            desc = "شاهد مسلسل طالع نازل أونلاين بجودة عالية HD حصرياً على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF1E1E24, c2 = 0xFF0F0F12, accent = 0xFFE50914,
            top10 = true, isNew = true,
            poster = "https://ffosta-tv.quest/uploads/thumbs/5c950d81-1.jpg",
            backdrop = "https://ffosta-tv.quest/uploads/thumbs/5c950d81-1.jpg",
            link = "https://ffosta-tv.quest/view-serie.php?name=%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%b7%d8%a7%d9%84%d8%b9-%d9%86%d8%a7%d8%b2%d9%84"
        ),
        show(
            id = "fosta_aser",
            title = "مسلسل آسر",
            year = "2026",
            rating = "عربي",
            imdb = 8.6,
            genres = listOf("مسلسلات عربية", "دراما"),
            desc = "شاهد مسلسل آسر بجودة عالية HD حصرياً على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF142A11, c2 = 0xFF060D05, accent = 0xFFE50914,
            top10 = false, isNew = true,
            poster = "https://ffosta-tv.quest/uploads/thumbs/a29c17ab-1.jpg",
            backdrop = "https://ffosta-tv.quest/uploads/thumbs/a29c17ab-1.jpg",
            link = "https://ffosta-tv.quest/view-serie.php?name=%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a2%d8%b3%d8%b1"
        ),
        show(
            id = "fosta_harb_elgabaly",
            title = "مسلسل حرب الجبالي",
            year = "2026",
            rating = "عربي",
            imdb = 8.4,
            genres = listOf("مسلسلات عربية", "دراما", "أكشن"),
            desc = "شاهد مسلسل حرب الجبالي بجودة عالية HD حصرياً على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF2A1111, c2 = 0xFF0D0505, accent = 0xFFE50914,
            top10 = false, isNew = true,
            poster = "https://ffosta-tv.quest/uploads/thumbs/5c950d81-1.jpg",
            backdrop = "https://ffosta-tv.quest/uploads/thumbs/5c950d81-1.jpg",
            link = "https://ffosta-tv.quest/view-serie.php?name=%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%ad%d8%b1%d8%a8-%d8%a7%d9%84%d8%ac%d8%a8%d8%a7%d9%84%d9%8a"
        ),
        show(
            id = "fosta_movie_alb_elleil",
            title = "فيلم قلب الليل",
            year = "1989",
            rating = "فيلم",
            imdb = 8.3,
            genres = listOf("افلام عربية", "أفلام", "دراما"),
            desc = "شاهد فيلم قلب الليل كامل بجودة عالية HD حصرياً على حكايتنا.",
            dur = "ساعتان",
            c1 = 0xFF1C112A, c2 = 0xFF08050D, accent = 0xFFE50914,
            top10 = true, isNew = false,
            poster = "https://ffosta-tv.quest/uploads/thumbs/3cba4f3a7-1.jpg",
            backdrop = "https://ffosta-tv.quest/uploads/thumbs/3cba4f3a7-1.jpg",
            link = "https://ffosta-tv.quest/watch.php?vid=3cba4f3a7"
        ),
        show(
            id = "fosta_movie_qabt_elhelaly",
            title = "فيلم قبضة الهلالي",
            year = "1991",
            rating = "فيلم",
            imdb = 8.1,
            genres = listOf("افلام عربية", "أفلام", "أكشن"),
            desc = "شاهد فيلم قبضة الهلالي كامل بجودة عالية HD حصرياً على حكايتنا.",
            dur = "ساعة ونصف",
            c1 = 0xFF2A2211, c2 = 0xFF0D0B05, accent = 0xFFE50914,
            top10 = false, isNew = false,
            poster = "https://ffosta-tv.quest/uploads/thumbs/9784f8f6b-1.jpg",
            backdrop = "https://ffosta-tv.quest/uploads/thumbs/9784f8f6b-1.jpg",
            link = "https://ffosta-tv.quest/watch.php?vid=9784f8f6b"
        ),

        // ==========================================
        // 1. مسلسلات تركية (حكايتنا) ★
        // ==========================================
        show(
            id = "3ck_sevdan_bir_ates",
            title = "مسلسل حبك نار",
            year = "2026",
            rating = "تركي",
            imdb = 8.5,
            genres = listOf("مسلسلات تركية", "دراما", "رومانسي"),
            desc = "شاهد مسلسل حبك نار مترجم أونلاين بجودة عالية HD على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF2A1111, c2 = 0xFF0D0505, accent = 0xFFE50914,
            top10 = true, isNew = true,
            poster = "https://e.3cktv.com/wp-content/uploads/2026/09/1-1-349x520.jpg",
            backdrop = "https://e.3cktv.com/wp-content/uploads/2026/09/1-1-349x520.jpg",
            link = "https://e.3cktv.com/video/series/musalsal-sevdan-bir-ates/"
        ),
        show(
            id = "3ck_ask_ve_taht",
            title = "مسلسل العشق والعرش",
            year = "2026",
            rating = "تركي",
            imdb = 8.6,
            genres = listOf("مسلسلات تركية", "تاريخي", "دراما"),
            desc = "شاهد مسلسل العشق والعرش مترجم أونلاين بجودة عالية HD على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF1E1E24, c2 = 0xFF0F0F12, accent = 0xFFE50914,
            top10 = true, isNew = true,
            poster = "https://e.3cktv.com/wp-content/uploads/2026/09/1-5-349x520.png",
            backdrop = "https://e.3cktv.com/wp-content/uploads/2026/09/1-5-349x520.png",
            link = "https://e.3cktv.com/video/series/musalsal-ask-ve-taht/"
        ),
        show(
            id = "3ck_haysiyet",
            title = "مسلسل الكرامة",
            year = "2026",
            rating = "تركي",
            imdb = 8.4,
            genres = listOf("مسلسلات تركية", "جريمة", "دراما"),
            desc = "شاهد مسلسل الكرامة مترجم أونلاين بجودة عالية HD على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF1C112A, c2 = 0xFF08050D, accent = 0xFFE50914,
            top10 = true, isNew = true,
            poster = "https://e.3cktv.com/wp-content/uploads/2026/09/2-2-347x520.jpg",
            backdrop = "https://e.3cktv.com/wp-content/uploads/2026/09/2-2-347x520.jpg",
            link = "https://e.3cktv.com/video/series/musalsal-haysiyet/"
        ),
        show(
            id = "3ck_muhtemel_ask_dubbed",
            title = "مسلسل احتمال حب مدبلج",
            year = "2026",
            rating = "مدبلج",
            imdb = 8.2,
            genres = listOf("مسلسلات تركية", "مدبلج", "كوميدي"),
            desc = "شاهد مسلسل احتمال حب مدبلج أونلاين بجودة عالية HD على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF112A1B, c2 = 0xFF050D08, accent = 0xFFE50914,
            top10 = false, isNew = true,
            poster = "https://e.3cktv.com/wp-content/uploads/2026/06/1-2-351x520.jpg",
            backdrop = "https://e.3cktv.com/wp-content/uploads/2026/06/1-2-351x520.jpg",
            link = "https://e.3cktv.com/video/series/musalsal-modablaj-muhtemel-ask/"
        ),
        show(
            id = "3ck_tuzlu_kahve",
            title = "مسلسل القهوة المالحة",
            year = "2026",
            rating = "تركي",
            imdb = 8.3,
            genres = listOf("مسلسلات تركية", "رومانسي", "دراما"),
            desc = "شاهد مسلسل القهوة المالحة مترجم أونلاين بجودة عالية HD على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF2A1C11, c2 = 0xFF0D0805, accent = 0xFFE50914,
            top10 = false, isNew = true,
            poster = "https://e.3cktv.com/wp-content/uploads/2026/09/1-349x520.png",
            backdrop = "https://e.3cktv.com/wp-content/uploads/2026/09/1-349x520.png",
            link = "https://e.3cktv.com/video/series/musalsal-tuzlu-kahve/"
        ),
        show(
            id = "3ck_mehmed_fetihler_sultani",
            title = "مسلسل محمد الفاتح سلطان الفتوحات",
            year = "2024",
            rating = "تركي",
            imdb = 8.9,
            genres = listOf("مسلسلات تركية", "تاريخي", "حربي"),
            desc = "شاهد مسلسل محمد الفاتح سلطان الفتوحات مترجم أونلاين بجودة عالية HD على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF2A2211, c2 = 0xFF0D0B05, accent = 0xFFE50914,
            top10 = true, isNew = false,
            poster = "https://e.3cktv.com/wp-content/uploads/2024/03/mehmed-fetihler-sultani2-480x480.jpg",
            backdrop = "https://e.3cktv.com/wp-content/uploads/2024/03/mehmed-fetihler-sultani2-480x480.jpg",
            link = "https://e.3cktv.com/video/series/musalsal-mehmed-fetihler-sultani/"
        ),
        show(
            id = "3ck_gonul_dagi",
            title = "مسلسل جبل جونول",
            year = "2024",
            rating = "تركي",
            imdb = 8.8,
            genres = listOf("مسلسلات تركية", "دراما", "عائلي"),
            desc = "شاهد مسلسل جبل جونول مترجم أونلاين بجودة عالية HD على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF11242A, c2 = 0xFF050B0D, accent = 0xFFE50914,
            top10 = false, isNew = false,
            poster = "https://e.3cktv.com/wp-content/uploads/2024/01/series.jabal_.jonol_.online.season.1-293x520.jpg",
            backdrop = "https://e.3cktv.com/wp-content/uploads/2024/01/series.jabal_.jonol_.online.season.1-293x520.jpg",
            link = "https://e.3cktv.com/video/series/gonul-dagi/"
        ),
        show(
            id = "3ck_a_b_i",
            title = "مسلسل أخي",
            year = "2026",
            rating = "تركي",
            imdb = 8.1,
            genres = listOf("مسلسلات تركية", "دراما", "إثارة"),
            desc = "شاهد مسلسل أخي مترجم أونلاين بجودة عالية HD على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF1C112A, c2 = 0xFF08050D, accent = 0xFFE50914,
            top10 = false, isNew = true,
            poster = "https://e.3cktv.com/wp-content/uploads/2026/01/9-318x520.png",
            backdrop = "https://e.3cktv.com/wp-content/uploads/2026/01/9-318x520.png",
            link = "https://e.3cktv.com/video/series/musalsal-a-b-i/"
        ),
        show(
            id = "3ck_arafta",
            title = "مسلسل الأعراف",
            year = "2025",
            rating = "تركي",
            imdb = 8.0,
            genres = listOf("مسلسلات تركية", "دراما", "غموض"),
            desc = "شاهد مسلسل الأعراف مترجم أونلاين بجودة عالية HD على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF1E1E24, c2 = 0xFF0F0F12, accent = 0xFFE50914,
            top10 = false, isNew = false,
            poster = "https://e.3cktv.com/wp-content/uploads/2025/12/1122-347x520.jpg",
            backdrop = "https://e.3cktv.com/wp-content/uploads/2025/12/1122-347x520.jpg",
            link = "https://e.3cktv.com/video/series/musalsal-arafta/"
        ),
        show(
            id = "3ck_tasacak_bu_deniz_dubbed",
            title = "مسلسل هذا البحر سوف يفيض مدبلج",
            year = "2026",
            rating = "مدبلج",
            imdb = 8.3,
            genres = listOf("مسلسلات تركية", "مدبلج", "رومانسي"),
            desc = "شاهد مسلسل هذا البحر سوف يفيض مدبلج أونلاين بجودة عالية HD على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF2A111E, c2 = 0xFF0D0509, accent = 0xFFE50914,
            top10 = false, isNew = true,
            poster = "https://e.3cktv.com/wp-content/uploads/2026/09/1-7-349x520.png",
            backdrop = "https://e.3cktv.com/wp-content/uploads/2026/09/1-7-349x520.png",
            link = "https://e.3cktv.com/video/series/musalsal-modablaj-tasacak-bu-deniz/"
        ),
        show(
            id = "3ck_uzak_sehir",
            title = "مسلسل المدينة البعيدة",
            year = "2024",
            rating = "تركي",
            imdb = 8.7,
            genres = listOf("مسلسلات تركية", "دراما", "أكشن"),
            desc = "شاهد مسلسل المدينة البعيدة مترجم أونلاين بجودة عالية HD على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF1A1A24, c2 = 0xFF0D0D12, accent = 0xFFE50914,
            top10 = true, isNew = false,
            poster = "https://e.3cktv.com/wp-content/uploads/2024/11/E8tR39iX2XNZdqeDA5REFDquU6-medium-347x520-1.jpg",
            backdrop = "https://e.3cktv.com/wp-content/uploads/2024/11/E8tR39iX2XNZdqeDA5REFDquU6-medium-347x520-1.jpg",
            link = "https://e.3cktv.com/video/series/musalsal-uzak-sehir/"
        ),
        show(
            id = "3ck_daha_on_yedi_dubbed",
            title = "مسلسل ما زلت في 17 مدبلج",
            year = "2026",
            rating = "مدبلج",
            imdb = 8.1,
            genres = listOf("مسلسلات تركية", "مدبلج", "دراما"),
            desc = "شاهد مسلسل ما زلت في 17 مدبلج أونلاين بجودة عالية HD على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF1E2A11, c2 = 0xFF090D05, accent = 0xFFE50914,
            top10 = false, isNew = true,
            poster = "https://e.3cktv.com/wp-content/uploads/2026/07/2-1-322x520.png",
            backdrop = "https://e.3cktv.com/wp-content/uploads/2026/07/2-1-322x520.png",
            link = "https://e.3cktv.com/video/series/musalsal-modablaj-daha-on-yedi/"
        ),
        show(
            id = "3ck_sevdigim_sensin",
            title = "مسلسل أنت من أحب",
            year = "2026",
            rating = "تركي",
            imdb = 8.4,
            genres = listOf("مسلسلات تركية", "رومانسي", "كوميدي"),
            desc = "شاهد مسلسل أنت من أحب مترجم أونلاين بجودة عالية HD على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF2A1E11, c2 = 0xFF0D0905, accent = 0xFFE50914,
            top10 = false, isNew = true,
            poster = "https://e.3cktv.com/wp-content/uploads/2026/02/1-349x520.jpg",
            backdrop = "https://e.3cktv.com/wp-content/uploads/2026/02/1-349x520.jpg",
            link = "https://e.3cktv.com/video/series/musalsal-sevdigim-sensin/"
        ),
        show(
            id = "3ck_alikara",
            title = "مسلسل علي كارا",
            year = "2026",
            rating = "تركي",
            imdb = 8.5,
            genres = listOf("مسلسلات تركية", "أكشن", "جريمة"),
            desc = "شاهد مسلسل علي كارا مترجم أونلاين بجودة عالية HD على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF111C2A, c2 = 0xFF05080D, accent = 0xFFE50914,
            top10 = true, isNew = true,
            poster = "https://e.3cktv.com/wp-content/uploads/2026/09/2-4-347x520.png",
            backdrop = "https://e.3cktv.com/wp-content/uploads/2026/09/2-4-347x520.png",
            link = "https://e.3cktv.com/video/series/musalsal-alikara/"
        ),
        show(
            id = "3ck_muhtemel_ask",
            title = "مسلسل حب محتمل",
            year = "2026",
            rating = "تركي",
            imdb = 8.3,
            genres = listOf("مسلسلات تركية", "رومانسي", "دراما"),
            desc = "شاهد مسلسل حب محتمل مترجم أونلاين بجودة عالية HD على حكايتنا.",
            dur = "45 دقيقة",
            c1 = 0xFF2A1122, c2 = 0xFF0D050B, accent = 0xFFE50914,
            top10 = false, isNew = true,
            poster = "https://e.3cktv.com/wp-content/uploads/2026/06/3-416x520.jpg",
            backdrop = "https://e.3cktv.com/wp-content/uploads/2026/06/3-416x520.jpg",
            link = "https://e.3cktv.com/video/series/musalsal-muhtemel-ask/"
        ),

        // ==========================================
        // 8. افلام عربية
        // ==========================================
        show(
            id = "movie_arabi_welad_rizk_3",
            title = "فيلم ولاد رزق 3: القاضية",
            year = "2024",
            rating = "16+",
            imdb = 8.5,
            genres = listOf("افلام عربية", "أكشن", "جريمة"),
            desc = "يعود الأخوة رزق في مغامرة جديدة محفوفة بالمخاطر في شوارع الرياض بحثاً عن غنيمة أسطورية.",
            dur = "125 دقيقة",
            c1 = 0xFF2A1111, c2 = 0xFF0D0505, accent = 0xFFE50914,
            top10 = true, isNew = false,
            poster = "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
            backdrop = "https://image.tmdb.org/t/p/w1280/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
            link = "https://ep.alooytv16.xyz/watch/welad-rizk-3.html"
        ),
        show(
            id = "movie_arabi_beit_el_ruby",
            title = "فيلم بيت الروبي",
            year = "2023",
            rating = "العائلة",
            imdb = 8.0,
            genres = listOf("افلام عربية", "كوميدي", "عائلي"),
            desc = "إبراهيم الروبي يترك عزلته في الصحراء ويعود إلى القاهرة ليتورط في عالم السوشيال ميديا.",
            dur = "110 دقيقة",
            c1 = 0xFF11222A, c2 = 0xFF050B0D, accent = 0xFF1D83D4,
            top10 = false, isNew = false,
            poster = "https://image.tmdb.org/t/p/w500/bOGkgRGdhrBYJSLpXaxhXVstddV.jpg",
            backdrop = "https://image.tmdb.org/t/p/w1280/bOGkgRGdhrBYJSLpXaxhXVstddV.jpg",
            link = "https://ep.alooytv16.xyz/watch/beit-el-ruby.html"
        ),
        show(
            id = "movie_arabi_fasel_lahazat",
            title = "فيلم فاصل من اللحظات اللذيذة",
            year = "2024",
            rating = "13+",
            imdb = 8.2,
            genres = listOf("افلام عربية", "كوميدي", "خيال"),
            desc = "زوجان يعيشان حياة تعيسة حتى تفتح بوابة لعالم مواز يغير حياتهما بالكامل.",
            dur = "105 دقيقة",
            c1 = 0xFF2A2411, c2 = 0xFF0D0B05, accent = 0xFFD4A31D,
            top10 = false, isNew = false,
            poster = "https://image.tmdb.org/t/p/w500/8b8R8l88Qje9dn9OE8PY05Nxl1X.jpg",
            backdrop = "https://image.tmdb.org/t/p/w1280/8b8R8l88Qje9dn9OE8PY05Nxl1X.jpg",
            link = "https://ep.alooytv16.xyz/watch/fasel-lahazat.html"
        ),

        // ==========================================
        // 9. افلام اجنبية
        // ==========================================
        show(
            id = "movie_foreign_dune_2",
            title = "فيلم Dune: Part Two",
            year = "2024",
            rating = "13+",
            imdb = 8.9,
            genres = listOf("افلام اجنبية", "خيال علمي", "مغامرة"),
            desc = "بول أتريدس يتحد مع تشاني وفريمن سعياً للانتقام من المتآمرين الذين دمروا عائلته.",
            dur = "166 دقيقة",
            c1 = 0xFF2A1C11, c2 = 0xFF0D0805, accent = 0xFFD46D1D,
            top10 = true, isNew = false,
            poster = "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
            backdrop = "https://image.tmdb.org/t/p/w1280/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
            link = "https://ep.alooytv16.xyz/watch/dune-2.html"
        ),
        show(
            id = "movie_foreign_oppenheimer",
            title = "فيلم Oppenheimer",
            year = "2023",
            rating = "18+",
            imdb = 8.9,
            genres = listOf("افلام اجنبية", "دراما", "سيرة ذاتية", "تاريخي"),
            desc = "قصة الفيزيائي الأمريكي روبرت أوبنهايمر ودوره في تطوير القنبلة الذرية خلال مشروع مانهاتن.",
            dur = "180 دقيقة",
            c1 = 0xFF142A11, c2 = 0xFF060D05, accent = 0xFF35D41D,
            top10 = true, isNew = false,
            poster = "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
            backdrop = "https://image.tmdb.org/t/p/w1280/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
            link = "https://ep.alooytv16.xyz/watch/oppenheimer.html"
        ),
        show(
            id = "movie_foreign_deadpool_wolverine",
            title = "فيلم Deadpool & Wolverine",
            year = "2024",
            rating = "18+",
            imdb = 8.8,
            genres = listOf("افلام اجنبية", "أكشن", "كوميدي", "خيال علمي"),
            desc = "ديدبول وولفرين يجتمعان معاً لإنقاذ عالمهما في مغامرة مليئة بالدم والضحك والأكشن.",
            dur = "128 دقيقة",
            c1 = 0xFF2A1111, c2 = 0xFF0D0505, accent = 0xFFE50914,
            top10 = true, isNew = false,
            poster = "https://image.tmdb.org/t/p/w500/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg",
            backdrop = "https://image.tmdb.org/t/p/w1280/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg",
            link = "https://ep.alooytv16.xyz/watch/deadpool-wolverine.html"
        ),
        show(
            id = "movie_foreign_titanic",
            title = "فيلم تايتانك Titanic",
            year = "1997",
            rating = "فيلم",
            imdb = 7.9,
            genres = listOf("افلام اجنبية", "أفلام", "دراما", "رومانسي"),
            desc = "عالم يحكمه الفارق الطبقي، يقع 'جواك' الفنان الفقير و'روز' الفتاة الأرستقراطية الحسناء في الحب على ظهر السفينة الفارهة تايتانك قبل أن تتجه نحو مصيرها المأساوي المحتوم في مياه المحيط المتجمدة.",
            dur = "195 دقيقة",
            c1 = 0xFF11242A, c2 = 0xFF050B0D, accent = 0xFF1D8DD4,
            top10 = true, isNew = false,
            poster = "https://image.tmdb.org/t/p/w500/9kq9p60G0L4kP10vA4F0v86M98k.jpg",
            backdrop = "https://image.tmdb.org/t/p/w1280/9kq9p60G0L4kP10vA4F0v86M98k.jpg",
            link = "https://ffosta-tv.quest/watch.php?vid=3cba4f3a7"
        ),

        // ==========================================
        // 10. افلام كورية
        // ==========================================
        show(
            id = "movie_korean_parasite",
            title = "فيلم طفيلي Parasite",
            year = "2020",
            rating = "18+",
            imdb = 8.6,
            genres = listOf("افلام كورية", "إثارة", "دراما"),
            desc = "عائلة فقيرة تحتال للدخول إلى منزل عائلة ثرية حتى تحدث مفاجأة تقلب كل الموازين.",
            dur = "132 دقيقة",
            c1 = 0xFF1C112A, c2 = 0xFF08050D, accent = 0xFF8B1DD4,
            top10 = true, isNew = false,
            poster = "https://image.tmdb.org/t/p/w500/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg",
            backdrop = "https://image.tmdb.org/t/p/w1280/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg",
            link = "https://ep.alooytv16.xyz/watch/parasite.html"
        ),
        show(
            id = "movie_korean_train_to_busan",
            title = "فيلم قطار إلى بوسان Train to Busan",
            year = "2021",
            rating = "16+",
            imdb = 8.0,
            genres = listOf("افلام كورية", "أكشن", "رعب"),
            desc = "انتشار فيروس زومبي مرعب داخل كوريا وركاب قطار بوسان يقاتلون للبقاء على قيد الحياة.",
            dur = "118 دقيقة",
            c1 = 0xFF2A1117, c2 = 0xFF0D0507, accent = 0xFFD41D48,
            top10 = false, isNew = false,
            poster = "https://image.tmdb.org/t/p/w500/vVpEOvdxVBP2aV166j5Xlvb5C2f.jpg",
            backdrop = "https://image.tmdb.org/t/p/w1280/vVpEOvdxVBP2aV166j5Xlvb5C2f.jpg",
            link = "https://ep.alooytv16.xyz/watch/train-to-busan.html"
        ),

        // ==========================================
        // 13. مسرحيات
        // ==========================================
        show(
            id = "play_madrasat_al_moshaghibeen",
            title = "مسرحية مدرسة المشاغبين",
            year = "1973",
            rating = "العائلة",
            imdb = 8.9,
            genres = listOf("مسرحيات", "كوميدي"),
            desc = "أشهر مسرحية كوميدية عربية تجمع عادل إمام وسعيد صالح وسهير البابلي في فصل دراسي لا يهدأ.",
            dur = "210 دقيقة",
            c1 = 0xFF2A2411, c2 = 0xFF0D0B05, accent = 0xFFD4A31D,
            top10 = true, isNew = false,
            poster = "https://image.tmdb.org/t/p/w500/5E2tW6WMUDux911r6m7haRef0WH.jpg",
            backdrop = "https://image.tmdb.org/t/p/w1280/5E2tW6WMUDux911r6m7haRef0WH.jpg",
            link = "https://ep.alooytv16.xyz/watch/madrasat-al-moshaghibeen.html"
        ),
        show(
            id = "play_el_eyal_kebret",
            title = "مسرحية العيال كبرت",
            year = "1979",
            rating = "العائلة",
            imdb = 8.8,
            genres = listOf("مسرحيات", "كوميدي"),
            desc = "أبناء رمضان السكري يكتشفون نية والدهم الزواج والهروب ويضعون خطة كوميدية لإحباطه.",
            dur = "200 دقيقة",
            c1 = 0xFF142A11, c2 = 0xFF060D05, accent = 0xFF35D41D,
            top10 = false, isNew = false,
            poster = "https://image.tmdb.org/t/p/w500/6E2tW6WMUDux911r6m7haRef0WH.jpg",
            backdrop = "https://image.tmdb.org/t/p/w1280/6E2tW6WMUDux911r6m7haRef0WH.jpg",
            link = "https://ep.alooytv16.xyz/watch/el-eyal-kebret.html"
        ),
        show(
            id = "play_bye_bye_london",
            title = "مسرحية باي باي لندن",
            year = "1981",
            rating = "العائلة",
            imdb = 8.7,
            genres = listOf("مسرحيات", "كوميدي"),
            desc = "المسرحية الكويتية الخالدة بطولة عبدالحسين عبدالرضا وغانم الصالح ومفارقات السفر إلى لندن.",
            dur = "180 دقيقة",
            c1 = 0xFF11242A, c2 = 0xFF050B0D, accent = 0xFF1D8DD4,
            top10 = false, isNew = false,
            poster = "https://image.tmdb.org/t/p/w500/7E2tW6WMUDux911r6m7haRef0WH.jpg",
            backdrop = "https://image.tmdb.org/t/p/w1280/7E2tW6WMUDux911r6m7haRef0WH.jpg",
            link = "https://ep.alooytv16.xyz/watch/bye-bye-london.html"
        )
    ).filterNot { isTashShow(it.title, it.link, it.id) }

    fun byId(id: String): Show? = all.firstOrNull { it.id == id && !isTashShow(it.title, it.link, it.id) }
    
    val featured: Show? get() = all.firstOrNull()

    val categories: List<Category> get() {
        val arabic = all.filter { it.genres.contains("مسلسلات عربية") }
        val turkish = all.filter { it.genres.contains("مسلسلات تركية") || it.genres.contains("تركي") || it.link.contains("3cktv") }
        val turkishSub = turkish.filter { !it.rating.contains("مدبلج") && !it.title.contains("مدبلج") && !it.genres.contains("مدبلج") }
        val turkishDub = turkish.filter { it.rating.contains("مدبلج") || it.title.contains("مدبلج") || it.genres.contains("مدبلج") }
        val movies = all.filter { it.genres.contains("أفلام") || it.genres.contains("افلام عربية") || it.genres.contains("افلام اجنبية") || it.rating == "فيلم" }
        val topShows = all.filter { it.isTop10 }.ifEmpty { all.take(10) }

        return listOf(
            Category("أحدث الحلقات والمسلسلات", all.take(15)),
            Category("مسلسلات عربية ومصرية", arabic),
            Category("أحدث الأفلام الحصرية", movies),
            Category("مسلسلات تركية مترجمة", turkishSub),
            Category("مسلسلات تركية مدبلجة", turkishDub),
            Category("مسلسلات تركية", turkish),
            Category("الأكثر مشاهدة رائج الآن", topShows),
            Category("جميع المسلسلات والأفلام", all)
        ).filter { it.shows.isNotEmpty() }
    }

    val genres = listOf(
        "الكل",
        "تركي",
        "مسلسلات عربية",
        "مسلسلات تركية",
        "مدبلج",
        "مترجم",
        "افلام عربية",
        "افلام اجنبية",
        "دراما",
        "كوميدي",
        "أكشن",
        "رومانسي"
    )
}
