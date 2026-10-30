package com.phishdb.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.phishdb.app.R
import com.phishdb.app.data.AppContainer
import com.phishdb.app.data.repo.ResultType
import com.phishdb.app.data.repo.SearchFilters
import com.phishdb.app.data.repo.SearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    container: AppContainer,
    onOpenShow: (String) -> Unit,
    onOpenSong: (String) -> Unit,
) {
    val vm: SearchViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SearchViewModel(container.repository) }
        }
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = state.query,
            onQueryChange = vm::onQueryChange,
            onOpenFilters = vm::openFilters,
            onClear = { vm.clearAll() },
            hasFilters = state.filters.hasStructuredFilters(),
        )
        TypeChips(
            selected = state.filters.resultType,
            onSelect = vm::setResultTypeFilter,
        )
        HorizontalDivider()

        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

        when {
            !state.submitted && !state.loading ->
                EmptyHint(text = "Type to search shows, songs, venues, cities. Add filters for exact matches.")
            state.results.isEmpty() && !state.loading ->
                EmptyHint(text = stringResource(R.string.no_results))
            else -> ResultsList(
                results = state.results,
                onOpenShow = onOpenShow,
                onOpenSong = onOpenSong,
            )
        }
    }

    if (state.filterSheetOpen) {
        FilterSheet(
            initial = state.filters,
            onDismiss = vm::closeFilters,
            onApply = {
                vm.onFiltersChange(it)
                vm.closeFilters()
            },
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onClear: () -> Unit,
    hasFilters: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onOpenFilters) {
            Icon(
                Icons.Outlined.Tune,
                contentDescription = stringResource(R.string.filters),
                tint = if (hasFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TypeChips(
    selected: ResultType?,
    onSelect: (ResultType?) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("All") },
        )
        FilterChip(
            selected = selected == ResultType.SONG_PERFORMANCE,
            onClick = { onSelect(ResultType.SONG_PERFORMANCE) },
            label = { Text("Performances") },
        )
        FilterChip(
            selected = selected == ResultType.SHOW,
            onClick = { onSelect(ResultType.SHOW) },
            label = { Text("Shows") },
        )
        FilterChip(
            selected = selected == ResultType.SONG,
            onClick = { onSelect(ResultType.SONG) },
            label = { Text("Songs") },
        )
    }
}

@Composable
private fun ResultsList(
    results: List<SearchResult>,
    onOpenShow: (String) -> Unit,
    onOpenSong: (String) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
        items(items = results, key = { r -> r::class.simpleName + ":" + r.entityId }) { r ->
            ResultRow(r, onOpenShow, onOpenSong)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun ResultRow(
    r: SearchResult,
    onOpenShow: (String) -> Unit,
    onOpenSong: (String) -> Unit,
) {
    val icon: ImageVector = when (r) {
        is SearchResult.Show -> Icons.Outlined.Album
        is SearchResult.Performance -> Icons.Outlined.MusicNote
        is SearchResult.Song -> Icons.Outlined.MusicNote
    }
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = {
            Text(r.title, maxLines = 2, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            if (r.subtitle.isNotBlank()) Text(r.subtitle, maxLines = 2)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when (r) {
                    is SearchResult.Show -> onOpenShow(r.showdate)
                    is SearchResult.Performance -> onOpenShow(r.showdate)
                    is SearchResult.Song -> onOpenSong(r.title)
                }
            },
    )
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    initial: SearchFilters,
    onDismiss: () -> Unit,
    onApply: (SearchFilters) -> Unit,
) {
    val editable = remember(initial) { FilterSheetState.fromFilters(initial) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.filters), style = MaterialTheme.typography.titleMedium)

            FieldRow(label = R.string.song, value = editable.song) { editable.song = it }
            FieldRow(label = R.string.tour, value = editable.tour) { editable.tour = it }
            FieldRow(label = R.string.venue, value = editable.venue) { editable.venue = it }
            FieldRow(label = R.string.city, value = editable.city) { editable.city = it }
            FieldRow(label = R.string.state, value = editable.state) { editable.state = it }
            FieldRow(label = R.string.date, value = editable.date) { editable.date = it }
            FieldRow(label = R.string.date_from, value = editable.dateFrom) { editable.dateFrom = it }
            FieldRow(label = R.string.date_to, value = editable.dateTo) { editable.dateTo = it }
            FieldRow(label = R.string.year, value = editable.year) { editable.year = it.filter { ch -> ch.isDigit() } }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { editable.clear(); onApply(SearchFilters(query = initial.query)) }) {
                    Text(stringResource(R.string.clear))
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { onApply(editable.toFilters(initial.query)) }) {
                    Text(stringResource(R.string.apply))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FieldRow(label: Int, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

private class FilterSheetState {
    var song: String by mutableStateOf("")
    var tour: String by mutableStateOf("")
    var venue: String by mutableStateOf("")
    var city: String by mutableStateOf("")
    var state: String by mutableStateOf("")
    var date: String by mutableStateOf("")
    var dateFrom: String by mutableStateOf("")
    var dateTo: String by mutableStateOf("")
    var year: String by mutableStateOf("")

    fun clear() {
        song = ""; tour = ""; venue = ""; city = ""; state = ""
        date = ""; dateFrom = ""; dateTo = ""; year = ""
    }

    fun toFilters(query: String?): SearchFilters = SearchFilters(
        query = query,
        song = song.ifBlank { null },
        tour = tour.ifBlank { null },
        venue = venue.ifBlank { null },
        city = city.ifBlank { null },
        state = state.ifBlank { null },
        date = date.ifBlank { null },
        dateFrom = dateFrom.ifBlank { null },
        dateTo = dateTo.ifBlank { null },
        year = year.ifBlank { null }?.toIntOrNull(),
    )

    companion object {
        fun fromFilters(f: SearchFilters) = FilterSheetState().apply {
            song = f.song.orEmpty()
            tour = f.tour.orEmpty()
            venue = f.venue.orEmpty()
            city = f.city.orEmpty()
            state = f.state.orEmpty()
            date = f.date.orEmpty()
            dateFrom = f.dateFrom.orEmpty()
            dateTo = f.dateTo.orEmpty()
            year = f.year?.toString().orEmpty()
        }
    }
}
