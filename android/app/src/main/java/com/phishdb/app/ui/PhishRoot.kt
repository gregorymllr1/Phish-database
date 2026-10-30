package com.phishdb.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.phishdb.app.data.AppContainer
import com.phishdb.app.ui.nav.Routes
import com.phishdb.app.ui.search.SearchScreen
import com.phishdb.app.ui.settings.SettingsScreen
import com.phishdb.app.ui.show.ShowDetailScreen
import com.phishdb.app.ui.song.SongDetailScreen
import com.phishdb.app.ui.stats.StatsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhishRoot(container: AppContainer) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            if (current in topLevelRoutes) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = backStack?.destination?.hierarchy
                            ?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.SEARCH,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.SEARCH) {
                SearchScreen(
                    container = container,
                    onOpenShow = { date -> nav.navigate(Routes.show(date)) },
                    onOpenSong = { name -> nav.navigate(Routes.song(name)) },
                )
            }
            composable(Routes.STATS) {
                StatsScreen(container = container)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(container = container)
            }
            composable(Routes.SHOW) { entry ->
                val date = entry.arguments?.getString("date").orEmpty()
                ShowDetailScreen(
                    container = container,
                    showDate = date,
                    onBack = { nav.popBackStack() },
                    onOpenSong = { name -> nav.navigate(Routes.song(name)) },
                )
            }
            composable(Routes.SONG) { entry ->
                val raw = entry.arguments?.getString("name").orEmpty()
                val name = java.net.URLDecoder.decode(raw, "UTF-8")
                SongDetailScreen(
                    container = container,
                    songName = name,
                    onBack = { nav.popBackStack() },
                    onOpenShow = { date -> nav.navigate(Routes.show(date)) },
                )
            }
        }
    }
}

private val topLevelRoutes = setOf(Routes.SEARCH, Routes.STATS, Routes.SETTINGS)

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(Routes.SEARCH, "Search", Icons.Outlined.Search),
    BottomTab(Routes.STATS, "Stats", Icons.Outlined.BarChart),
    BottomTab(Routes.SETTINGS, "Settings", Icons.Outlined.Settings),
)
