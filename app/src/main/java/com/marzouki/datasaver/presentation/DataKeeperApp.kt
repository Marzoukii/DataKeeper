package com.marzouki.datasaver.presentation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.marzouki.datasaver.R
import com.marzouki.datasaver.presentation.downloads.DownloadsScreen
import com.marzouki.datasaver.presentation.quota.QuotaScreen
import com.marzouki.datasaver.presentation.usage.UsageScreen

private enum class Destination(val route: String, @param:StringRes val labelRes: Int, @param:DrawableRes val iconRes: Int) {
    USAGE("usage", R.string.tab_usage, R.drawable.ic_data_usage),
    QUOTA("quota", R.string.tab_quota, R.drawable.ic_cell),
    DOWNLOADS("downloads", R.string.tab_downloads, R.drawable.ic_download),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataKeeperApp() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = Destination.entries.firstOrNull { it.route == backStackEntry?.destination?.route } ?: Destination.USAGE

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(current.labelRes)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == current,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.USAGE.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.USAGE.route) { UsageScreen() }
            composable(Destination.QUOTA.route) { QuotaScreen(snackbarHostState) }
            composable(Destination.DOWNLOADS.route) { DownloadsScreen(snackbarHostState) }
        }
    }
}
