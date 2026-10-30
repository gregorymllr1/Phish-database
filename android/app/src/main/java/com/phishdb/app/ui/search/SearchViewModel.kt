package com.phishdb.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phishdb.app.data.repo.PhishRepository
import com.phishdb.app.data.repo.ResultType
import com.phishdb.app.data.repo.SearchFilters
import com.phishdb.app.data.repo.SearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val filters: SearchFilters = SearchFilters(),
    val results: List<SearchResult> = emptyList(),
    val loading: Boolean = false,
    val filterSheetOpen: Boolean = false,
    val submitted: Boolean = false,
)

class SearchViewModel(private val repo: PhishRepository) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(query = q)
        schedule()
    }

    fun onFiltersChange(filters: SearchFilters) {
        _state.value = _state.value.copy(filters = filters)
        schedule()
    }

    fun openFilters() { _state.value = _state.value.copy(filterSheetOpen = true) }
    fun closeFilters() { _state.value = _state.value.copy(filterSheetOpen = false) }

    fun clearAll() {
        _state.value = SearchUiState()
    }

    private fun schedule() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250)
            runSearch()
        }
    }

    fun runSearch() {
        val s = _state.value
        val combined = s.filters.copy(query = s.query.ifBlank { null }, limit = s.filters.limit.coerceAtLeast(50))
        if (!combined.hasAny()) {
            _state.value = s.copy(results = emptyList(), loading = false, submitted = false)
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, submitted = true)
            val list = runCatching { repo.searchAll(combined) }.getOrDefault(emptyList())
            _state.value = _state.value.copy(results = list, loading = false)
        }
    }

    fun setResultTypeFilter(type: ResultType?) {
        onFiltersChange(_state.value.filters.copy(resultType = type))
    }
}
