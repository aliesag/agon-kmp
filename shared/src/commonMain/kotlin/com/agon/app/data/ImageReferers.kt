package com.agon.app.data

object ImageReferers {
    /** Android original requires Referer on image GETs or the host returns a block page. */
    fun refererFor(url: String): String {
        val host = url.substringAfter("://", url).substringBefore("/").lowercase()
        return when {
            host.contains("3cktv") -> "https://e.3cktv.com/"
            host.contains("ffosta-tv") || host.contains("fosta") -> "https://ffosta-tv.quest/"
            host.contains("alooytv") -> "https://ep.alooytv16.xyz/"
            host.contains("yalla.moda") -> "https://yalla.moda/"
            host.contains("image.tmdb.org") -> "https://www.themoviedb.org/"
            else -> "https://e.3cktv.com/"
        }
    }

    fun userAgent(): String =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
}
