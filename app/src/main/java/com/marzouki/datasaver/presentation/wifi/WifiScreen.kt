package com.marzouki.datasaver.presentation.wifi

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
import com.marzouki.datasaver.ui.components.HeaderIconButton
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
        ) {
            if (state.availability == WifiAvailability.READY) {
                HeaderIconButton(Icons.Rounded.Refresh, stringResource(R.string.wifi_refresh), onClick = { onIntent(WifiIntent.RescanClicked) })
            }
        }

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

/**
 * Réseaux prêts : plan stylisé avec le nombre de réseaux, avertissement, filtres, le plus proche
 * mis en avant dans sa carte, puis les autres en liste avec leur bouton « Connecter ».
 */
@Composable
private fun NetworksSection(state: WifiState, onIntent: (WifiIntent) -> Unit) {
    MapCard(state)
    WarningCard()

    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChipItem(stringResource(R.string.wifi_filter_all, state.networks.size), WifiFilter.ALL, state.filter, onIntent)
        FilterChipItem(stringResource(R.string.wifi_filter_no_ads, state.noAdsCount), WifiFilter.NO_ADS, state.filter, onIntent)
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
        visible.isNotEmpty() -> {
            val nearest = visible.first()
            NearestCard(
                network = nearest,
                connected = nearest.ssid == state.connectedSsid,
                onConnect = { onIntent(WifiIntent.ConnectClicked) },
            )
            val others = visible.drop(1)
            if (others.isNotEmpty()) {
                DataKeeperCard(contentPadding = 0.dp) {
                    others.forEachIndexed { index, network ->
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
    }
}

/** Positions des réseaux sur le plan (fractions de la largeur et de la hauteur), autour de toi au centre. */
private val MapSpots = listOf(0.58f to 0.38f, 0.36f to 0.33f, 0.64f to 0.66f, 0.30f to 0.70f, 0.78f to 0.28f, 0.84f to 0.56f)

/**
 * Plan stylisé : rues claires, rayon pointillé autour de toi (point violet), un point par réseau
 * (sarcelle sans pub, lavande avec portail, gris inconnu). Dessous : nombre de réseaux et heure du scan,
 * relue toutes les 30 s pour que « il y a 1 min » avance.
 */
@Composable
private fun MapCard(state: WifiState) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val ground = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
    val street = MaterialTheme.colorScheme.surface
    val block = MaterialTheme.colorScheme.secondaryContainer
    val ring = DataColors.wifi
    val me = MaterialTheme.colorScheme.primary
    val spotColors = state.networks.take(MapSpots.size).map {
        when (it.portal) {
            WifiPortal.NO_PORTAL -> DataColors.wifi
            WifiPortal.WITH_PORTAL -> DataColors.mobile
            WifiPortal.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    DataKeeperCard(contentPadding = 0.dp) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(ground),
        ) {
            val w = size.width
            val h = size.height
            drawLine(street, Offset(0f, h * 0.24f), Offset(w, h * 0.35f), strokeWidth = 6.dp.toPx())
            drawLine(street, Offset(w * 0.17f, 0f), Offset(w * 0.31f, h), strokeWidth = 10.dp.toPx())
            drawLine(street, Offset(w * 0.70f, 0f), Offset(w * 0.64f, h), strokeWidth = 8.dp.toPx())
            val road = Path().apply {
                moveTo(-10f, h * 0.70f)
                cubicTo(w * 0.25f, h * 0.58f, w * 0.40f, h * 0.84f, w * 0.62f, h * 0.65f)
                cubicTo(w * 0.80f, h * 0.48f, w * 0.92f, h * 0.50f, w + 10f, h * 0.53f)
            }
            drawPath(road, street, style = Stroke(14.dp.toPx(), cap = StrokeCap.Round))
            drawRoundRect(block, Offset(w * 0.75f, h * 0.65f), Size(w * 0.2f, h * 0.28f), CornerRadius(8.dp.toPx()))
            drawRoundRect(block, Offset(w * 0.05f, h * 0.34f), Size(w * 0.12f, h * 0.24f), CornerRadius(8.dp.toPx()))
            val center = Offset(w * 0.47f, h * 0.5f)
            drawCircle(ring.copy(alpha = 0.08f), radius = h * 0.37f, center = center)
            drawCircle(
                ring.copy(alpha = 0.30f),
                radius = h * 0.37f,
                center = center,
                style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx()))),
            )
            spotColors.forEachIndexed { index, color ->
                val (fx, fy) = MapSpots[index]
                drawCircle(color, radius = 7.dp.toPx(), center = Offset(w * fx, h * fy))
            }
            drawCircle(street, radius = 11.dp.toPx(), center = center)
            drawCircle(me, radius = 8.dp.toPx(), center = center)
        }
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (state.isScanning && state.networks.isEmpty()) {
                        stringResource(R.string.wifi_area_title)
                    } else {
                        pluralStringResource(R.plurals.wifi_area_count, state.networks.size, state.networks.size)
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    when {
                        state.isScanning || state.scannedAtMillis == null -> stringResource(R.string.wifi_area_scanning)
                        else -> stringResource(R.string.wifi_area_updated, formatElapsed(state.scannedAtMillis ?: now, now))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.isScanning) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else if (state.noAdsCount > 0) {
                Pill(
                    stringResource(R.string.wifi_no_ads_count, state.noAdsCount),
                    DataColors.successContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun WarningCard() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(
            Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.wifi_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun FilterChipItem(label: String, filter: WifiFilter, selected: WifiFilter, onIntent: (WifiIntent) -> Unit) {
    FilterChip(
        selected = filter == selected,
        onClick = { onIntent(WifiIntent.FilterSelected(filter)) },
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

/** Le réseau le plus proche, mis en avant : liseré sarcelle, nom en grand, étiquette et bouton. */
@Composable
private fun NearestCard(network: OpenWifiNetwork, connected: Boolean, onConnect: () -> Unit) {
    DataKeeperCard(
        modifier = Modifier.border(1.5.dp, DataColors.wifi, MaterialTheme.shapes.large),
        contentPadding = 18.dp,
    ) {
        Text(
            stringResource(R.string.wifi_nearest_label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = DataColors.wifi,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Wifi, contentDescription = null, tint = DataColors.wifi, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(network.ssid, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(
                        R.string.wifi_details,
                        stringResource(R.string.wifi_distance, network.estimatedDistanceMeters),
                        stringResource(network.signal.labelRes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PortalTag(network.portal)
            Spacer(Modifier.weight(1f))
            if (connected) {
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(DataColors.wifi)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.wifi_connected), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondary)
                }
            } else {
                ConnectButton(onConnect)
            }
        }
    }
}

@Composable
private fun ConnectButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        modifier = Modifier.height(36.dp),
    ) {
        Text(stringResource(R.string.wifi_connect), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Pill(text: String, container: Color, content: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = content,
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Réseau de la liste : pastille sarcelle sans pub, lavande avec portail, grise inconnue ; distance, signal et portail. */
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
        when (network.portal) {
            WifiPortal.WITH_PORTAL -> IconTile(Icons.Rounded.Wifi, colors.tertiaryContainer, colors.onTertiaryContainer)
            WifiPortal.NO_PORTAL -> IconTile(Icons.Rounded.Wifi, colors.secondaryContainer, DataColors.wifi)
            WifiPortal.UNKNOWN -> IconTile(Icons.Rounded.Wifi, colors.surfaceVariant, colors.onSurfaceVariant)
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
                ) + " · " + stringResource(network.portal.labelRes),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (connected) {
            Text(stringResource(R.string.wifi_connected), style = MaterialTheme.typography.labelLarge, color = DataColors.wifi)
        } else {
            ConnectButton(onConnect)
        }
    }
}

private val WifiPortal.labelRes: Int
    get() = when (this) {
        WifiPortal.WITH_PORTAL -> R.string.wifi_portal
        WifiPortal.NO_PORTAL -> R.string.wifi_no_portal
        WifiPortal.UNKNOWN -> R.string.wifi_untested
    }

@Composable
private fun PortalTag(portal: WifiPortal) {
    val (container, content, textRes) = when (portal) {
        WifiPortal.WITH_PORTAL -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            R.string.wifi_portal,
        )
        WifiPortal.NO_PORTAL -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            R.string.wifi_no_portal,
        )
        WifiPortal.UNKNOWN -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            R.string.wifi_untested,
        )
    }
    Text(
        stringResource(textRes),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = content,
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
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
                colors = ButtonDefaults.buttonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text(button) }
        }
    }
}

/** Illustration des écrans d'attente du Wi-Fi : une épingle violette sous des ondes sarcelle. */
@Composable
private fun WifiPinIllustration(modifier: Modifier = Modifier) {
    val wave = DataColors.wifi
    val pinColor = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(width = 180.dp, height = 140.dp)) {
        val cx = size.width / 2
        val stroke = 5.dp.toPx()
        listOf(0.95f to 0.45f, 0.68f to 0.75f).forEach { (r, alpha) ->
            val radius = size.width * r / 2
            drawArc(
                color = wave.copy(alpha = alpha),
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
        drawPath(pin, pinColor)
        drawCircle(pinColor, radius = pinR * 0.62f, center = Offset(cx, top + pinR * 0.95f))
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
