package com.phishdb.app.ui.song

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.phishdb.app.data.AppContainer
import com.phishdb.app.data.db.SetlistEntryEntity
import com.phishdb.app.data.repo.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(
    container: AppContainer,
    songName: String,
    onBack: () -> Unit,
    onOpenShow: (String) -> Unit,
) {
    val vm: SongDetailViewModel = viewModel(
        key = songName,
        factory = viewModelFactory {
            initializer { SongDetailViewModel(container.repository, songName) }
        }
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.song?.song ?: songName, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.history.isEmpty() -> Text(
                    "No performances found for \"${state.name}\".",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
                else -> HistoryList(state, onOpenShow)
            }
        }
    }
}

@Composable
private fun HistoryList(
    s: SongUi,
    onOpenShow: (String) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header(s) }
        items(s.history, key = { it.uniqueid }) { entry ->
            HistoryRow(entry) { onOpenShow(entry.showdate) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun Header(s: SongUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(s.song?.song ?: s.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        s.song?.let { song ->
            val sub = buildList {
                song.times_played?.let { add("$it performances") }
                song.debut?.takeIf { it.isNotBlank() }?.let { add("debut $it") }
                song.last_played?.takeIf { it.isNotBlank() }?.let { add("last $it") }
            }.joinToString(" · ")
            if (sub.isNotBlank()) {
                Text(sub, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            "${s.history.size} shown",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    HorizontalDivider()
}

@Composable
private fun HistoryRow(entry: SetlistEntryEntity, onClick: () -> Unit) {
    val duration = formatDuration(entry.tracktime_seconds).ifBlank { entry.tracktime_raw.orEmpty() }
    val loc = listOfNotNull(entry.venue, entry.city, entry.state)
        .filter { it.isNotBlank() }.joinToString(", ")
    val sub = buildList {
        if (loc.isNotBlank()) add(loc)
        if (duration.isNotBlank()) add(duration)
        if (!entry.footnote.isNullOrBlank()) add(entry.footnote)
    }.joinToString(" · ")

    ListItem(
        headlineContent = {
            Text("${entry.showdate} · Set ${entry.set_name} #${entry.position ?: "-"}",
                fontWeight = FontWeight.SemiBold)
        },
        supportingContent = { if (sub.isNotBlank()) Text(sub) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}
