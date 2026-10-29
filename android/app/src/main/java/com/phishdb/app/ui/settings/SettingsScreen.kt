package com.phishdb.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phishdb.app.R
import com.phishdb.app.data.AppContainer
import com.phishdb.app.sync.SyncState
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer) {
    val syncState by container.syncManager.state.collectAsStateWithLifecycle()
    val stats by remember { container.repository.statsFlow() }.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Phish DB",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Your setlist archive, stored on this phone. New shows are added every Sunday.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = "${stringResource(R.string.last_sync)}: " +
                (stats?.lastSync?.take(16)?.replace('T', ' ') ?: stringResource(R.string.never_synced)),
            style = MaterialTheme.typography.bodyMedium,
        )

        val running = syncState is SyncState.Running
        OutlinedButton(
            onClick = { scope.launch { container.syncManager.syncRecent() } },
            enabled = !running,
        ) {
            Icon(Icons.Outlined.CloudSync, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (running) stringResource(R.string.sync_running) else stringResource(R.string.sync_now))
        }

        when (val s = syncState) {
            is SyncState.Idle -> Unit
            is SyncState.Running -> SyncProgress(s)
            is SyncState.Error -> Text(
                text = "Sync failed: ${s.message}",
                color = MaterialTheme.colorScheme.error,
            )
            is SyncState.Success -> Text(
                text = "Last run: ${s.stats.setlistEntries} entries · ${s.stats.yearsSynced.size} years",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Powered by phish.net API v5.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SyncProgress(s: SyncState.Running) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text(s.message, style = MaterialTheme.typography.bodyMedium)
        }
        if (s.progress != null) {
            LinearProgressIndicator(
                progress = { s.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
