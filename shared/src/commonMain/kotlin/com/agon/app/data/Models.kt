package com.agon.app.data

data class Category(
    val title: String,
    val shows: List<Show>
)

data class Episode(
    val id: String,
    val title: String,
    val episodeNum: String,
    val poster: String,
    val link: String,
    val ribbon: String? = null,
    val seasonNumber: Int = 1
)

data class CastMember(
    val name: String,
    val character: String? = null,
    val profilePath: String? = null
)

data class Show(
    val id: String,
    val title: String,
    val year: String,
    val rating: String,      // e.g. "+16"
    val imdb: Double,
    val genres: List<String>,
    val description: String,
    val poster: String,       // portrait
    val backdrop: String,     // landscape
    val duration: String,
    val isTop10: Boolean = false,
    val isNew: Boolean = false,
    val cast: List<String> = emptyList(),
    val castMembers: List<CastMember> = emptyList(),
    val link: String = "",
    val isMovie: Boolean = false,
    val episodes: List<Episode> = emptyList(),
    val ribbon: String? = null,
    val tmdbId: Int? = null
)

data class VideoServer(
    val name: String,
    val url: String
)

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val features: String,
    val downloadUrl: String,
    val isMandatory: Boolean = true
)

data class HomeScrapedShow(
    val title: String,
    val link: String,
    val ribbon: String? = null,
    val poster: String? = null
)

data class ShortItem(
    val id: String,
    val title: String,
    val description: String,
    val videoUrl: String,
    val posterUrl: String = "",
    val uploaderName: String = "حكاياتنا شورتس",
    val uploaderAvatar: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val audioTitle: String = "الصوت الأصلي",
    val linkedShowId: String? = null,
    val comments: List<ShortComment> = emptyList()
)

data class ShortComment(
    val id: String,
    val user: String,
    val avatar: String,
    val text: String,
    val timeAgo: String = "الآن",
    val linkedShowId: String? = null
)
