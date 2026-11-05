package com.agon.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agon.app.data.matches.MatchDay
import com.agon.app.data.matches.MatchItem
import com.agon.app.data.matches.MatchStatus
import com.agon.app.data.matches.MatchesRepository
import com.agon.app.data.matches.SportsNewsItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MatchesViewModel : ViewModel() {

    private val _matches = MutableStateFlow<List<MatchItem>>(MatchesRepository.getInitialMatches())
    val matches: StateFlow<List<MatchItem>> = _matches.asStateFlow()

    private val _news = MutableStateFlow<List<SportsNewsItem>>(MatchesRepository.getInitialNews())
    val news: StateFlow<List<SportsNewsItem>> = _news.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedDay = MutableStateFlow(MatchDay.TODAY)
    val selectedDay: StateFlow<MatchDay> = _selectedDay.asStateFlow()

    private val _selectedLeagueFilter = MutableStateFlow("الكل")
    val selectedLeagueFilter: StateFlow<String> = _selectedLeagueFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Live matches count
    val liveMatchesCount: StateFlow<Int> = _matches.combine(_selectedDay) { list, _ ->
        list.count { it.status == MatchStatus.LIVE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    // Filtered list based on day, league filter, and search
    val filteredMatches: StateFlow<List<MatchItem>> = combine(
        _matches,
        _selectedDay,
        _selectedLeagueFilter,
        _searchQuery
    ) { allMatches, day, filter, query ->
        allMatches.filter { match ->
            // Filter by day
            val matchesDay = match.day == day
            
            // Filter by league / category
            val matchesFilter = when (filter) {
                "الكل" -> true
                "مباشر الآن" -> match.status == MatchStatus.LIVE
                "الدوري الإسباني" -> match.tournament.contains("الإسباني")
                "الدوري الإنجليزي" -> match.tournament.contains("الإنجليزي") || match.tournament.contains("الكاراباو")
                "الدوري الأوروبي" -> match.tournament.contains("الأوروبي")
                "الدوري المصري" -> match.tournament.contains("المصري")
                else -> match.tournament.contains(filter)
            }

            // Filter by search query
            val matchesQuery = query.isBlank() ||
                    match.team1Name.contains(query, ignoreCase = true) ||
                    match.team2Name.contains(query, ignoreCase = true) ||
                    match.tournament.contains(query, ignoreCase = true) ||
                    match.commentator.contains(query, ignoreCase = true)

            matchesDay && matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _matches.value)

    init {
        refresh()
    }

    fun selectDay(day: MatchDay) {
        _selectedDay.value = day
    }

    fun setLeagueFilter(filter: String) {
        _selectedLeagueFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val liveList = MatchesRepository.fetchLiveMatches()
                if (liveList.isNotEmpty()) {
                    _matches.value = liveList
                }
                val liveNews = MatchesRepository.fetchLiveNews()
                if (liveNews.isNotEmpty()) {
                    _news.value = liveNews
                }
            } catch (e: Exception) {
                // Keep initial data
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getMatchById(id: String): MatchItem? {
        return _matches.value.find { it.id == id } ?: MatchesRepository.getInitialMatches().find { it.id == id }
    }
}
