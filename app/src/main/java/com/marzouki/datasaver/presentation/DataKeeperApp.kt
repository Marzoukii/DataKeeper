package com.marzouki.datasaver.presentation

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.marzouki.datasaver.R
import com.marzouki.datasaver.presentation.common.CollectEffects
import com.marzouki.datasaver.presentation.common.openAppDetailsSettings
import com.marzouki.datasaver.presentation.common.openUsageAccessSettings
import com.marzouki.datasaver.presentation.downloads.DownloadsScreen
import com.marzouki.datasaver.presentation.onboarding.OnboardingEffect
import com.marzouki.datasaver.presentation.onboarding.OnboardingIntent
import com.marzouki.datasaver.presentation.onboarding.OnboardingViewModel
import com.marzouki.datasaver.presentation.onboarding.UsageAccessScreen
import com.marzouki.datasaver.presentation.quota.QuotaScreen
import com.marzouki.datasaver.presentation.usage.UsageScreen

private enum class Destination(val route: String, @param:StringRes val labelRes: Int, val icon: ImageVector) {
    USAGE(AppRoutes.USAGE, R.string.tab_usage, Icons.Rounded.BarChart),
    QUOTA(AppRoutes.QUOTA, R.string.tab_quota, Icons.Rounded.Speed),
    DOWNLOADS(AppRoutes.DOWNLOADS, R.string.tab_downloads, Icons.Rounded.Download),
}

/**
 * Racine : écran d'accès aux données d'usage tant que la permission manque, sinon l'appli à onglets.
 * [requestedTab] (route d'[AppRoutes]) ouvre un onglet précis, à la demande d'un widget ou d'une tuile.
 */
@Composable
fun DataKeeperApp(
    requestedTab: String? = null,
    onTabRequestHandled: () -> Unit = {},
    onboardingViewModel: OnboardingViewModel = hiltViewModel(),
) {
    val onboarding by onboardingViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        onboardingViewModel.onIntent(OnboardingIntent.Refresh)
        onPauseOrDispose { }
    }
    CollectEffects(onboardingViewModel.effects) { effect ->
        when (effect) {
            OnboardingEffect.OpenUsageAccessSettings -> openUsageAccessSettings(context)
            OnboardingEffect.OpenAppDetailsSettings -> openAppDetailsSettings(context)
        }
    }

    Crossfade(targetState = onboarding.showUsageAccessScreen, label = "root") { showUsageAccess ->
        if (showUsageAccess) {
            UsageAccessScreen(
                onOpenSettings = { onboardingViewModel.onIntent(OnboardingIntent.OpenSettingsClicked) },
                onLater = { onboardingViewModel.onIntent(OnboardingIntent.LaterClicked) },
                // Les « paramètres restreints » n'existent qu'à partir d'Android 13.
                showRestrictedHelp = onboarding.settingsOpened && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                onOpenAppInfo = { onboardingViewModel.onIntent(OnboardingIntent.OpenAppInfoClicked) },
            )
        } else {
            MainScaffold(requestedTab, onTabRequestHandled)
        }
    }
}

@Composable
private fun MainScaffold(requestedTab: String?, onTabRequestHandled: () -> Unit) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(requestedTab) {
        Destination.entries.firstOrNull { it.route == requestedTab }?.let { navController.navigateToTab(it) }
        if (requestedTab != null) onTabRequestHandled()
    }
    val current = Destination.entries.firstOrNull { it.route == backStackEntry?.destination?.route } ?: Destination.USAGE

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == current,
                        onClick = { navController.navigateToTab(destination) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
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
            composable(Destination.USAGE.route) {
                UsageScreen(onOpenQuota = { navController.navigateToTab(Destination.QUOTA) })
            }
            composable(Destination.QUOTA.route) {
                QuotaScreen(snackbarHostState, onOpenDownloads = { navController.navigateToTab(Destination.DOWNLOADS) })
            }
            composable(Destination.DOWNLOADS.route) { DownloadsScreen(snackbarHostState) }
        }
    }
}

private fun NavHostController.navigateToTab(destination: Destination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
