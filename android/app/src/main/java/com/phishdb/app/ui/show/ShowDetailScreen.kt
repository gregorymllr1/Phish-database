package com.phishdb.app.ui.show

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
fun ShowDetailScreen(
    container: AppContainer,
    showDate: String,
    onBack: () -> Unit,
    onOpenSong: (String) -> Unit,
) {
    val vm: ShowDetailViewModel = viewModel(
        key = showDate,
        factory = viewModelFactory {
            initializer { ShowDetailViewModel(container.repository, showDate) }
        }
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = state.detail?.show?.showdate ?: showDate,
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
                state.error != null -> Text(
                    "Couldn't load show: ${state.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
                state.detail == null -> Text(
                    "No show stored for $showDate.\nSync in Settings to populate the database.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
                else -> ShowContent(state.detail!!, onOpenSong)
            }
        }
    }
}

@Composable
private fun ShowContent(
    detail: com.phishdb.app.data.repo.ShowDetail,
    onOpenSong: (String) -> Unit,
) {
    val show = detail.show
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.padding(20.dp)) {
                Text(show.venue.orEmpty(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                val loc = listOfNotNull(show.city, show.state, show.country)
                    .filter { it.isNotBlank() }.joinToString(", ")
                if (loc.isNotBlank()) {
                    Text(loc, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!show.tour_name.isNullOrBlank()) {
                    Spacer(Modifier.width(0.dp))
                    Text("Tour: ${show.tour_name}", style = MaterialTheme.typography.bodyMedium)
                }
                if (!show.soundcheck.isNullOrBlank()) {
                    Text("Soundcheck: ${show.soundcheck}", style = MaterialTheme.typography.bodyMedium)
                }
                if (!show.setlist_notes.isNullOrBlank()) {
                    Text(show.setlist_notes, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider()
        }

        detail.sets.forEach { set ->
            item {
                Text(
                    text = set.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
            items(set.entries, key = { it.uniqueid }) { entry ->
                EntryRow(entry = entry, onOpenSong = { onOpenSong(entry.song) })
            }
        }
    }
}

@Composable
private fun EntryRow(entry: SetlistEntryEntity, onOpenSong: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSong)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = (entry.position?.toString() ?: "-").padStart(2),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
        Column(Modifier.fillMaxWidth()) {
            val duration = formatDuration(entry.tracktime_seconds).ifBlank { entry.tracktime_raw.orEmpty() }
            Text(
                text = buildString {
                    append(entry.song)
                    if (!entry.trans_mark.isNullOrBlank()) append(" ${entry.trans_mark}")
                    if (entry.isjamchart == 1) append(" ✦")
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            val meta = buildList {
                if (duration.isNotBlank()) add(duration)
                if (!entry.footnote.isNullOrBlank()) add(entry.footnote)
            }
            if (meta.isNotEmpty()) {
                Text(
                    text = meta.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!entry.jamchart_description.isNullOrBlank()) {
                Text(
                    text = entry.jamchart_description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
