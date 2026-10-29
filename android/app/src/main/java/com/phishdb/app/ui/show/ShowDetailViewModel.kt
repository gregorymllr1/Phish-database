package com.phishdb.app.ui.show

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phishdb.app.data.repo.PhishRepository
import com.phishdb.app.data.repo.ShowDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShowDetailUi(
    val loading: Boolean = true,
    val detail: ShowDetail? = null,
    val error: String? = null,
)

class ShowDetailViewModel(
    private val repo: PhishRepository,
    private val date: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ShowDetailUi())
    val state: StateFlow<ShowDetailUi> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = ShowDetailUi(loading = true)
            runCatching { repo.getShowByDate(date) }
                .onSuccess { _state.value = ShowDetailUi(loading = false, detail = it) }
                .onFailure { _state.value = ShowDetailUi(loading = false, error = it.message) }
        }
    }
}
