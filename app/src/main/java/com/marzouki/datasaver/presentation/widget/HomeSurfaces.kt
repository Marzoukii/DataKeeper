package com.marzouki.datasaver.presentation.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.service.quicksettings.TileService
import androidx.glance.appwidget.updateAll
import com.marzouki.datasaver.MainActivity
import com.marzouki.datasaver.domain.usecase.GetHomeSummaryUseCase
import com.marzouki.datasaver.presentation.AppRoutes
import com.marzouki.datasaver.presentation.tile.DownloadsTileService
import com.marzouki.datasaver.presentation.tile.QuotaTileService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Les widgets Glance ne sont pas injectables : ils passent par ce point d'entrée Hilt. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface HomeSurfacesEntryPoint {
    fun getHomeSummary(): GetHomeSummaryUseCase
}

internal fun homeSummaryUseCase(context: Context): GetHomeSummaryUseCase =
    EntryPointAccessors.fromApplication(context.applicationContext, HomeSurfacesEntryPoint::class.java).getHomeSummary()

/** Redessine les widgets et demande aux tuiles de se rafraîchir à leur prochaine ouverture. */
suspend fun refreshHomeSurfaces(context: Context) {
    QuotaWidget().updateAll(context)
    SavingsWidget().updateAll(context)
    QuotaBarWidget().updateAll(context)
    TileService.requestListeningState(context, ComponentName(context, QuotaTileService::class.java))
    TileService.requestListeningState(context, ComponentName(context, DownloadsTileService::class.java))
}

/** Intent ouvrant l'application sur l'onglet [route] (voir [AppRoutes]). */
fun openTabIntent(context: Context, route: String): Intent =
    Intent(context, MainActivity::class.java)
        .putExtra(AppRoutes.EXTRA_TAB, route)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

internal val Context.isNightMode: Boolean
    get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
