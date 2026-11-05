package com.agon.app.data.matches

enum class MatchStatus(val label: String, val colorHex: Long) {
    LIVE("مباشر الآن", 0xFFE50914),
    NOT_STARTED("لم تبدأ بعد", 0xFF1976D2),
    FINISHED("انتهت", 0xFF546E7A)
}

enum class MatchDay(val title: String) {
    YESTERDAY("مباريات الأمس"),
    TODAY("مباريات اليوم"),
    TOMORROW("مباريات الغد")
}

data class MatchServer(
    val id: String,
    val name: String,
    val embedUrl: String,
    val qualityBadge: String = "HD"
)

data class MatchItem(
    val id: String,
    val team1Name: String,
    val team1Logo: String,
    val team2Name: String,
    val team2Logo: String,
    val score1: String = "0",
    val score2: String = "0",
    val matchTime: String,
    val status: MatchStatus,
    val tournament: String,
    val channel: String,
    val commentator: String,
    val streamPageUrl: String,
    val embedUrl: String,
    val servers: List<MatchServer> = emptyList(),
    val day: MatchDay = MatchDay.TODAY,
    val liveMinute: String? = null
)

data class SportsNewsItem(
    val id: String,
    val title: String,
    val imageUrl: String,
    val url: String,
    val category: String = "أخبار الرياضة",
    val content: String = "",
    val date: String = "اليوم"
)
