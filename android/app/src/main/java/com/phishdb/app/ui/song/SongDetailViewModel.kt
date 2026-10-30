package com.phishdb.app.ui.song

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phishdb.app.data.db.SetlistEntryEntity
import com.phishdb.app.data.db.SongEntity
import com.phishdb.app.data.repo.PhishRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SongUi(
    val loading: Boolean = true,
    val name: String = "",
    val song: SongEntity? = null,
    val history: List<SetlistEntryEntity> = emptyList(),
    val error: String? = null,
)

class SongDetailViewModel(
    private val repo: PhishRepository,
    private val name: String,
) : ViewModel() {

    private val _state = MutableStateFlow(SongUi(name = name))
    val state: StateFlow<SongUi> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = SongUi(loading = true, name = name)
            runCatching {
                val song = repo.songByName(name)
                val history = repo.songHistory(name, limit = 200)
                song to history
            }.onSuccess { (song, history) ->
                _state.value = SongUi(loading = false, name = name, song = song, history = history)
            }.onFailure {
                _state.value = SongUi(loading = false, name = name, error = it.message)
            }
        }
    }
}
