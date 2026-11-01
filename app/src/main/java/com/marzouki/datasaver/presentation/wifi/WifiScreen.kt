package com.marzouki.datasaver.presentation.wifi

import com.marzouki.datasaver.ui.theme.Brand
import com.marzouki.datasaver.ui.components.ScreenHeader
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.Canvas
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.enums.WifiAvailability
import com.marzouki.datasaver.domain.enums.WifiPortal
import com.marzouki.datasaver.domain.enums.WifiSignal
import com.marzouki.datasaver.domain.model.OpenWifiNetwork
import com.marzouki.datasaver.presentation.common.CollectEffects
import com.marzouki.datasaver.presentation.common.openAppDetailsSettings
import com.marzouki.datasaver.ui.components.DataKeeperCard
import com.marzouki.datasaver.ui.components.PreviewSurface
import com.marzouki.datasaver.ui.components.ScreenPreviews
import com.marzouki.datasaver.ui.components.formatElapsed
import com.marzouki.datasaver.ui.theme.DataColors
import com.marzouki.datasaver.ui.theme.Spacing
import kotlinx.coroutines.delay

/** Localisation pour lister les Wi-Fi ; Android 13+ demande en plus « Appareils à proximité ». */
private val WifiPermissions = buildList {
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.NEARBY_WIFI_DEVICES)
}.toTypedArray()

@Composable
fun WifiScreen(modifier: Modifier = Modifier, viewModel: WifiViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    // Après un second refus, Android n'affiche plus la demande : on ouvre alors les infos de l'appli.
    var permissionBlocked by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        permissionBlocked = results.containsValue(false) && activity != null &&
            results.filterValues { !it }.keys.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
        viewModel.onIntent(WifiIntent.Refresh)
    }

    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(WifiIntent.Refresh)
        onPauseOrDispose { }
    }
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            WifiEffect.OpenWifiPanel -> openWifiPanel(context)
            WifiEffect.OpenLocationSettings -> context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }
    WifiContent(
        state = state,
        onIntent = viewModel::onIntent,
        permissionBlocked = permissionBlocked,
        onRequestPermission = {
            if (permissionBlocked) openAppDetailsSettings(context) else permissionLauncher.launch(WifiPermissions)
        },
        modifier = modifier,
    )
}

/** Panneau Wi-Fi par-dessus l'appli (Android 10+), sinon les réglages Wi-Fi. */
private fun openWifiPanel(context: Context) {
    val panel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS
    try {
        context.startActivity(Intent(panel))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }
}

@Composable
private fun WifiContent(
    state: WifiState,
    onIntent: (WifiIntent) -> Unit,
    modifier: Modifier = Modifier,
    permissionBlocked: Boolean = false,
    onRequestPermission: () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.betweenCards),
    ) {
        ScreenHeader(
            overline = stringResource(R.string.wifi_subtitle),
            title = stringResource(R.string.wifi_title),
        )

        when (state.availability) {
            null -> Unit
            WifiAvailability.PERMISSION_MISSING -> NoticeCard(
                title = stringResource(R.string.wifi_permission_title),
                body = stringResource(R.string.wifi_permission_body),
                button = stringResource(if (permissionBlocked) R.string.wifi_permission_settings else R.string.wifi_permission_button),
                onClick = onRequestPermission,
            )
            WifiAvailability.WIFI_OFF -> NoticeCard(
                title = stringResource(R.string.wifi_off_title),
                body = stringResource(R.string.wifi_off_body),
                button = stringResource(R.string.wifi_off_button),
                onClick = { onIntent(WifiIntent.EnableWifiClicked) },
            )
            WifiAvailability.LOCATION_OFF -> NoticeCard(
                title = stringResource(R.string.wifi_location_off_title),
                body = stringResource(R.string.wifi_location_off_body),
                button = stringResource(R.string.wifi_location_off_button),
                onClick = { onIntent(WifiIntent.EnableLocationClicked) },
            )
            WifiAvailability.READY -> NetworksSection(state, onIntent)
        }
    }
}

@Composable
private fun NetworksSection(state: WifiState, onIntent: (WifiIntent) -> Unit) {
    AreaCard(state.scannedAtMillis, state.isScanning, onRescan = { onIntent(WifiIntent.RescanClicked) })
    WarningCard()

    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChipItem(stringResource(R.string.wifi_filter_all, state.networks.size), WifiFilter.ALL, state.filter, onIntent)
        FilterChipItem(stringResource(R.string.wifi_filter_no_ads, state.noAdsCount), WifiFilter.NO_ADS, state.filter, onIntent)
        FilterChipItem(stringResource(R.string.wifi_filter_nearest), WifiFilter.NEAREST, state.filter, onIntent)
    }

    val visible = state.visibleNetworks
    when {
        state.networks.isEmpty() && !state.isScanning -> DataKeeperCard {
            Text(stringResource(R.string.wifi_empty_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.wifi_empty_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        visible.isEmpty() && state.networks.isNotEmpty() -> DataKeeperCard {
            Text(
                stringResource(R.string.wifi_empty_filter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        visible.isNotEmpty() -> DataKeeperCard(contentPadding = 0.dp) {
            visible.forEachIndexed { index, network ->
                if (index > 0) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
                NetworkRow(
                    network = network,
                    connected = network.ssid == state.connectedSsid,
                    onConnect = { onIntent(WifiIntent.ConnectClicked) },
                )
            }
        }
    }
}

/** Zone scannée et heure du scan, relue toutes les 30 s pour que « il y a 1 min » avance. */
@Composable
private fun AreaCard(scannedAtMillis: Long?, isScanning: Boolean, onRescan: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    DataKeeperCard(contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Rounded.LocationOn, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.wifi_area_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    when {
                        isScanning || scannedAtMillis == null -> stringResource(R.string.wifi_area_scanning)
                        else -> stringResource(R.string.wifi_area_updated, formatElapsed(scannedAtMillis, now))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedIconButton(onClick = onRescan, enabled = !isScanning) {
                if (isScanning) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.wifi_refresh))
                }
            }
        }
    }
}

@Composable
private fun WarningCard() {
    DataKeeperCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentPadding = 12.dp) {
        Row {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.wifi_warning), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FilterChipItem(label: String, filter: WifiFilter, selected: WifiFilter, onIntent: (WifiIntent) -> Unit) {
    FilterChip(
        selected = filter == selected,
        onClick = { onIntent(WifiIntent.FilterSelected(filter)) },
        label = { Text(label) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

/** Réseau : icône orange s'il passe par un portail, bleue sinon ; distance, signal et ce qu'on sait du portail. */
@Composable
private fun NetworkRow(network: OpenWifiNetwork, connected: Boolean, onConnect: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (network.portal == WifiPortal.WITH_PORTAL) {
            IconTile(Icons.Rounded.Wifi, colors.tertiaryContainer, DataColors.mobile)
        } else {
            IconTile(Icons.Rounded.Wifi, colors.secondaryContainer, DataColors.wifi)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                network.ssid,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(
                    R.string.wifi_details,
                    stringResource(R.string.wifi_distance, network.estimatedDistanceMeters),
                    stringResource(network.signal.labelRes),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            PortalTag(network.portal)
        }
        Spacer(Modifier.width(8.dp))
        if (connected) {
            Text(stringResource(R.string.wifi_connected), style = MaterialTheme.typography.labelLarge, color = DataColors.wifi)
        } else {
            OutlinedButton(onClick = onConnect, shape = CircleShape) {
                Text(stringResource(R.string.wifi_connect))
            }
        }
    }
}

@Composable
private fun PortalTag(portal: WifiPortal) {
    val (container, content, textRes) = when (portal) {
        WifiPortal.WITH_PORTAL -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            R.string.wifi_portal,
        )
        WifiPortal.NO_PORTAL -> Triple(DataColors.successContainer, DataColors.success, R.string.wifi_no_portal)
        WifiPortal.UNKNOWN -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            R.string.wifi_untested,
        )
    }
    Text(
        stringResource(textRes),
        style = MaterialTheme.typography.labelSmall,
        color = content,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(container)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun IconTile(icon: ImageVector, container: Color, tint: Color) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Ce qui manque pour scanner (permission, Wi-Fi, localisation) et le bouton pour y remédier. */
@Composable
private fun NoticeCard(title: String, body: String, button: String, onClick: () -> Unit) {
    DataKeeperCard(shape = MaterialTheme.shapes.extraLarge) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WifiPinIllustration(Modifier.padding(vertical = 8.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Brand.Accent, contentColor = Brand.OnAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text(button) }
        }
    }
}

/** Illustration des écrans d'attente du Wi-Fi : une épingle bleu nuit sous des ondes menthe. */
@Composable
private fun WifiPinIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 180.dp, height = 140.dp)) {
        val cx = size.width / 2
        val stroke = 5.dp.toPx()
        listOf(0.95f to 0.45f, 0.68f to 0.75f).forEach { (r, alpha) ->
            val radius = size.width * r / 2
            drawArc(
                color = Brand.Wifi.copy(alpha = alpha),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - radius, size.height * 0.55f - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        val pinR = size.height * 0.24f
        val top = size.height * 0.22f
        val pin = Path().apply {
            moveTo(cx, size.height * 0.98f)
            cubicTo(cx - pinR * 1.6f, top + pinR * 1.5f, cx - pinR * 1.3f, top, cx, top)
            cubicTo(cx + pinR * 1.3f, top, cx + pinR * 1.6f, top + pinR * 1.5f, cx, size.height * 0.98f)
            close()
        }
        drawPath(pin, Brand.Accent)
        drawCircle(Brand.Accent, radius = pinR * 0.62f, center = Offset(cx, top + pinR * 0.95f))
    }
}

private val WifiSignal.labelRes: Int
    get() = when (this) {
        WifiSignal.STRONG -> R.string.wifi_signal_strong
        WifiSignal.MEDIUM -> R.string.wifi_signal_medium
        WifiSignal.WEAK -> R.string.wifi_signal_weak
    }

@ScreenPreviews
@Composable
private fun WifiContentPreview() {
    PreviewSurface(padded = false) {
        WifiContent(
            state = WifiState(
                availability = WifiAvailability.READY,
                networks = listOf(
                    OpenWifiNetwork("Café du Port", rssi = -62, frequencyMhz = 2437, portal = WifiPortal.WITH_PORTAL),
                    OpenWifiNetwork("Station TGM", rssi = -70, frequencyMhz = 2412, portal = WifiPortal.WITH_PORTAL),
                    OpenWifiNetwork("Hôtel Médina", rssi = -76, frequencyMhz = 5180, portal = WifiPortal.NO_PORTAL),
                    OpenWifiNetwork("Librairie Centrale", rssi = -80, frequencyMhz = 2462),
                ),
                scannedAtMillis = System.currentTimeMillis() - 60_000,
            ),
            onIntent = {},
        )
    }
}

@ScreenPreviews
@Composable
private fun WifiPermissionPreview() {
    PreviewSurface(padded = false) {
        WifiContent(WifiState(availability = WifiAvailability.PERMISSION_MISSING), onIntent = {})
    }
}
