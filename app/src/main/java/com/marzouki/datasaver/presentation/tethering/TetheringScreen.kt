package com.marzouki.datasaver.presentation.tethering

import com.marzouki.datasaver.ui.components.BackHeader
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material.icons.rounded.WifiTetheringOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.TetheringAlert
import com.marzouki.datasaver.domain.model.TetheringSession
import com.marzouki.datasaver.domain.model.TetheringUsage
import com.marzouki.datasaver.presentation.common.CollectEffects
import com.marzouki.datasaver.ui.components.DataKeeperCard
import com.marzouki.datasaver.ui.components.PreviewSurface
import com.marzouki.datasaver.ui.components.ScreenPreviews
import com.marzouki.datasaver.ui.components.SettingRow
import com.marzouki.datasaver.ui.components.ThinBar
import com.marzouki.datasaver.ui.components.formatDuration
import com.marzouki.datasaver.ui.components.formatTimeOrDay
import com.marzouki.datasaver.ui.components.toReadableSize
import com.marzouki.datasaver.ui.components.toReadableSizeParts
import com.marzouki.datasaver.ui.theme.DataColors
import com.marzouki.datasaver.ui.theme.Spacing
import com.marzouki.datasaver.ui.theme.numeric

/** Conso du partage de connexion : état du point d'accès, total de la session, rythme et alerte. */
@Composable
fun TetheringScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TetheringViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(TetheringIntent.Refresh)
        onPauseOrDispose { }
    }
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            TetheringEffect.RequestNotificationPermission ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            TetheringEffect.OpenHotspotSettings -> openHotspotSettings(context)
        }
    }
    TetheringContent(state, viewModel::onIntent, onBack, modifier)
}

/**
 * Réglages du point d'accès, où Android liste les appareils connectés. Leur adresse varie selon
 * la version et le constructeur : on essaie la page du point d'accès (AOSP, puis Samsung), puis
 * celle du partage, puis les réglages « Connexions ».
 */
private fun openHotspotSettings(context: Context) {
    val candidates = listOf(
        Intent().setClassName(SETTINGS_PACKAGE, "$SETTINGS_PACKAGE.Settings\$WifiTetherSettingsActivity"),
        // One UI n'exporte pas la page AOSP : sans ces deux-là on tombe sur « Point d'accès et modem ».
        Intent().setClassName(SETTINGS_PACKAGE, "$SETTINGS_PACKAGE.Settings\$WifiApSettingsActivity"),
        Intent().setClassName(SETTINGS_PACKAGE, "com.samsung.android.settings.wifi.mobileap.WifiApSettings"),
        Intent().setClassName(SETTINGS_PACKAGE, "$SETTINGS_PACKAGE.Settings\$TetherSettingsActivity"),
        Intent(Settings.ACTION_WIRELESS_SETTINGS),
    )
    for (intent in candidates) {
        try {
            context.startActivity(intent)
            return
        } catch (_: ActivityNotFoundException) {
            // Page absente sur ce téléphone : on essaie la suivante.
        } catch (_: SecurityException) {
            // Page non exportée par ce constructeur : on essaie la suivante.
        }
    }
}

private const val SETTINGS_PACKAGE = "com.android.settings"

@Composable
private fun SectionLabel(@StringRes textRes: Int) {
    val locale = LocalConfiguration.current.locales[0]
    Text(
        stringResource(textRes).uppercase(locale),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = Spacing.titleExtra, top = 8.dp),
    )
}

@Composable
private fun TetheringContent(
    state: TetheringState,
    onIntent: (TetheringIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showThresholdDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.betweenCards),
    ) {
        BackHeader(
            title = stringResource(R.string.tethering_title),
            subtitle = stringResource(R.string.tethering_subtitle),
            backDescription = stringResource(R.string.settings_back),
            onBack = onBack,
        )

        when (state.active) {
            null -> Unit
            true -> ActiveCard(state)
            false -> InactiveCard()
        }
        state.usage?.let { UsageCard(it, state) }
        AlertCard(
            alert = state.alert,
            onToggle = { onIntent(TetheringIntent.AlertToggled(it)) },
            onChangeThreshold = { showThresholdDialog = true },
        )
        SectionLabel(R.string.tethering_devices_section)
        // Android réserve la liste des appareils à ses propres réglages : on y renvoie.
        DataKeeperCard(contentPadding = 0.dp, onClick = { onIntent(TetheringIntent.DevicesClicked) }) {
            SettingRow(
                icon = Icons.Rounded.Devices,
                title = stringResource(R.string.tethering_devices_title),
                subtitle = stringResource(R.string.tethering_devices_body),
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showThresholdDialog) {
        ThresholdDialog(
            selected = state.alert.thresholdBytes,
            onSelect = {
                showThresholdDialog = false
                onIntent(TetheringIntent.ThresholdSelected(it))
            },
            onDismiss = { showThresholdDialog = false },
        )
    }
}

/**
 * Bandeau orange : le partage tourne, donc il pioche dans le forfait. Détail : opérateur, réseau,
 * heure de début (« Ooredoo · 5G · Depuis 10:40 »).
 */
@Composable
private fun ActiveCard(state: TetheringState) {
    val usage = state.usage
    DataKeeperCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DataColors.mobile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.WifiTethering,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.tethering_active),
                    style = MaterialTheme.typography.titleSmall,
                    color = DataColors.mobile,
                )
                val since = usage?.let {
                    val start = formatTimeOrDay(it.session.startMillis, it.nowMillis)
                    if (it.session.startKnown) {
                        stringResource(R.string.tethering_since, start)
                    } else {
                        stringResource(R.string.tethering_since_unknown, start)
                    }
                }
                val details = listOfNotNull(
                    state.operatorName,
                    state.networkGeneration?.label,
                    since,
                )
                if (details.isNotEmpty()) {
                    Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.tethering_active_badge),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DataColors.mobile)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun InactiveCard() {
    DataKeeperCard(contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.WifiTetheringOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.tethering_inactive), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.tethering_inactive_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Total de la session en grand, rythme, et progression vers le prochain palier. */
@Composable
private fun UsageCard(usage: TetheringUsage, state: TetheringState) {
    val size = usage.usedBytes.toReadableSizeParts()
    val target = usage.nextTargetBytes(state.alert)
    DataKeeperCard {
        Text(
            stringResource(R.string.tethering_used_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(size.value, style = MaterialTheme.typography.displaySmall.numeric(), color = DataColors.mobile)
            Spacer(Modifier.width(6.dp))
            Text(
                size.unit,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        val rate = state.bytesPerHour
        Text(
            if (rate != null) {
                stringResource(R.string.tethering_rate, rate.toReadableSize())
            } else {
                stringResource(R.string.tethering_rate_pending)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        ThinBar(usage.usedBytes.toFloat() / target, DataColors.mobile, height = 8.dp)
        usage.millisToReach(target, rate)?.let { remaining ->
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.tethering_eta, formatDuration(remaining), target.toReadableSize()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Interrupteur de l'alerte ; toucher le reste de la ligne change le seuil. */
@Composable
private fun AlertCard(alert: TetheringAlert, onToggle: (Boolean) -> Unit, onChangeThreshold: () -> Unit) {
    DataKeeperCard(contentPadding = 0.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(onClick = onChangeThreshold)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.tethering_alert_title, alert.thresholdBytes.toReadableSize()),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(R.string.tethering_alert_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = alert.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun ThresholdDialog(selected: Long, onSelect: (Long) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tethering_threshold_title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                TetheringAlert.THRESHOLDS.forEach { threshold ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = threshold == selected,
                                onClick = { onSelect(threshold) },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = threshold == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(threshold.toReadableSize(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@ScreenPreviews
@Composable
private fun TetheringActivePreview() {
    val now = System.currentTimeMillis()
    val session = TetheringSession(startMillis = now - 7_500_000, startKnown = true, baselineBytes = 0)
    PreviewSurface(padded = false) {
        TetheringContent(
            state = TetheringState(
                active = true,
                usage = TetheringUsage(session, usedBytes = 1_800_000_000, nowMillis = now),
                recentBytesPerHour = 240_000_000,
                alert = TetheringAlert(enabled = true, thresholdBytes = 2 * TetheringAlert.GB),
                operatorName = "Ooredoo",
            ),
            onIntent = {},
            onBack = {},
        )
    }
}

@ScreenPreviews
@Composable
private fun TetheringInactivePreview() {
    PreviewSurface(padded = false) {
        TetheringContent(TetheringState(active = false), onIntent = {}, onBack = {})
    }
}
