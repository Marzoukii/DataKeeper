package com.marzouki.datasaver.presentation.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.HomeSummary
import com.marzouki.datasaver.domain.usecase.GetHomeSummaryUseCase
import com.marzouki.datasaver.presentation.AppRoutes
import com.marzouki.datasaver.presentation.widget.openTabIntent
import com.marzouki.datasaver.ui.components.toReadableSize
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Base des tuiles des réglages rapides : relit le résumé à chaque ouverture du volet
 * et ouvre l'application sur [route] au toucher.
 */
abstract class HomeTileService : TileService() {

    @Inject
    lateinit var getHomeSummary: GetHomeSummaryUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var refreshJob: Job? = null

    protected abstract val route: String

    /** Remplit la tuile (libellé, sous-titre, état) à partir du résumé. */
    protected abstract fun Tile.bind(summary: HomeSummary)

    override fun onStartListening() {
        super.onStartListening()
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val summary = getHomeSummary()
            qsTile?.apply {
                bind(summary)
                updateTile()
            }
        }
    }

    override fun onStopListening() {
        refreshJob?.cancel()
        super.onStopListening()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val intent = openTabIntent(this, route)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, route.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    /** Le sous-titre n'existe qu'à partir d'Android 10 : avant, il rejoint le libellé. */
    protected fun Tile.setTexts(title: String, subtitle: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            label = title
            this.subtitle = subtitle
        } else {
            label = "$title · $subtitle"
        }
        contentDescription = "$title, $subtitle"
    }
}

/** Tuile « DataKeeper » : data mobile restante sur le quota. */
@AndroidEntryPoint
class QuotaTileService : HomeTileService() {

    override val route = AppRoutes.QUOTA

    override fun Tile.bind(summary: HomeSummary) {
        val status = summary.quotaStatus
        val subtitle = when {
            !summary.hasUsageAccess -> getString(R.string.tile_no_access)
            summary.operatorChanged -> getString(R.string.tile_new_sim)
            status == null -> getString(R.string.widget_no_quota)
            status.isExceeded -> getString(R.string.quota_exceeded_chip, status.exceededBytes.toReadableSize())
            else -> getString(R.string.quota_remaining_chip, status.remainingBytes.toReadableSize())
        }
        setTexts(getString(R.string.app_name), subtitle)
        state = if (status != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }
}

/** Tuile « Hors ligne » : téléchargements qui attendent le Wi-Fi. */
@AndroidEntryPoint
class DownloadsTileService : HomeTileService() {

    override val route = AppRoutes.DOWNLOADS

    override fun Tile.bind(summary: HomeSummary) {
        val subtitle = if (summary.pendingDownloads == 0) {
            getString(R.string.tile_downloads_none)
        } else {
            resources.getQuantityString(R.plurals.tile_downloads_pending, summary.pendingDownloads, summary.pendingDownloads)
        }
        setTexts(getString(R.string.tab_downloads), subtitle)
        state = if (summary.pendingDownloads > 0) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }
}
