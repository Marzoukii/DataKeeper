package com.marzouki.datasaver.presentation.onboarding

import androidx.compose.ui.draw.shadow
import com.marzouki.datasaver.ui.theme.numeric
import com.marzouki.datasaver.ui.theme.Brand
import com.marzouki.datasaver.ui.components.heroSurface
import com.marzouki.datasaver.ui.components.glowBackground
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marzouki.datasaver.R
import com.marzouki.datasaver.ui.components.DataKeeperCard
import com.marzouki.datasaver.ui.components.PreviewSurface
import com.marzouki.datasaver.ui.components.ScreenPreviews
import com.marzouki.datasaver.ui.theme.DataColors

/**
 * Écran 0 : demande l'accès aux données d'usage, sans barre de navigation.
 * [showRestrictedHelp] explique comment lever le blocage des « paramètres restreints ».
 */
@Composable
fun UsageAccessScreen(
    onOpenSettings: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
    showRestrictedHelp: Boolean = false,
    onOpenAppInfo: () -> Unit = {},
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .glowBackground()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fadingBottomEdge(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp, bottom = 24.dp),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(32.dp), ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.10f))
                        .clip(RoundedCornerShape(32.dp))
                        .heroSurface()
                        .padding(20.dp),
                ) {
                    Logo()
                    Spacer(Modifier.height(20.dp))
                    Illustration()
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.onboarding_title),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.onboarding_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(16.dp))
                DataKeeperCard(contentPadding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        CheckPoint(stringResource(R.string.onboarding_point_volumes))
                        CheckPoint(stringResource(R.string.onboarding_point_local))
                        CheckPoint(stringResource(R.string.onboarding_point_revoke))
                    }
                }
                // Dans la zone défilante : avec ses étapes, la carte ne tiendrait pas sur un petit écran.
                if (showRestrictedHelp) {
                    Spacer(Modifier.height(24.dp))
                    RestrictedSettingsHelp(onOpenAppInfo)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onOpenSettings,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Brand.Accent, contentColor = Brand.OnAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) { Text(stringResource(R.string.onboarding_open_settings), style = MaterialTheme.typography.labelLarge) }
            TextButton(
                onClick = onLater,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) { Text(stringResource(R.string.onboarding_later), style = MaterialTheme.typography.labelLarge) }
            Text(
                stringResource(R.string.onboarding_settings_path),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

/** Aide affichée quand Android a grisé l'interrupteur (« Contrôlé par les paramètres restreints »). */
@Composable
private fun RestrictedSettingsHelp(onOpenAppInfo: () -> Unit, modifier: Modifier = Modifier) {
    DataKeeperCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentPadding = 16.dp,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.onboarding_restricted_title), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.onboarding_restricted_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        R.string.onboarding_restricted_step_toggle,
                        R.string.onboarding_restricted_step_allow,
                        R.string.onboarding_restricted_step_retry,
                    ).forEachIndexed { index, step -> RestrictedStep(index + 1, stringResource(step)) }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.onboarding_restricted_missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onOpenAppInfo,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
                ) { Text(stringResource(R.string.onboarding_restricted_button), style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

@Composable
private fun RestrictedStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            "$number.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(18.dp),
        )
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Logo() {
    val description = stringResource(R.string.onboarding_logo_description)
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Trois lignes du carnet : le jour passé, aujourd'hui en safran, ce qu'il reste en menthe.
        Column(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brand.Accent)
                .semantics { contentDescription = description }
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            LogoBar(Color.White, 1f)
            LogoBar(Brand.Accent, 1f)
            LogoBar(Brand.Wifi, 0.58f)
        }
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun LogoBar(color: Color, fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth(fraction)
            .height(4.dp)
            .clip(CircleShape)
            .background(color),
    )
}

/**
 * Fond la fin de la zone défilante dans [color] : un texte qui continue sous les boutons se devine
 * au lieu d'être coupé net.
 */
private fun Modifier.fadingBottomEdge(color: Color, height: Dp = 32.dp): Modifier = drawWithContent {
    drawContent()
    val fade = height.toPx()
    drawRect(
        Brush.verticalGradient(listOf(Color.Transparent, color), startY = size.height - fade, endY = size.height),
        topLeft = Offset(0f, size.height - fade),
        size = Size(size.width, fade),
    )
}

private val IllustrationCardWidth = 240.dp
private val IllustrationBubbleSize = 56.dp

/** Un petit carnet : quatre jours, surtout en Wi-Fi, le troisième en bande safran (aujourd'hui). */
@Composable
private fun Illustration() {
    val description = stringResource(R.string.onboarding_illustration_description)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        listOf(Triple("26", 0.70f, 0.18f), Triple("27", 0.86f, 0.14f), Triple("28", 0.52f, 0.10f), Triple("29", 0.62f, 0.22f))
            .forEachIndexed { index, (day, wifi, mobile) ->
                val today = index == 2
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (today) Brand.AccentTint else Color.Transparent)
                        .padding(horizontal = if (today) 8.dp else 0.dp, vertical = if (today) 6.dp else 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        day,
                        style = MaterialTheme.typography.titleMedium.numeric(),
                        color = if (today) Brand.OnAccentTint else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(36.dp),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        IllustrationLine(if (today) Color(0xFF0B6E66) else Brand.Wifi, wifi)
                        IllustrationLine(if (today) Color(0xFF6A4FD8) else Color(0xFFB9A6F5), mobile)
                    }
                }
            }
    }
}

@Composable
private fun IllustrationLine(color: Color, fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth(fraction)
            .height(7.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun IllustrationBar(color: Color, fraction: Float) {
    Box(
        Modifier
            .width(22.dp)
            .fillMaxHeight(fraction)
            .clip(RoundedCornerShape(6.dp))
            .background(color),
    )
}

@Composable
private fun CheckPoint(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
    }
}

@ScreenPreviews
@Composable
private fun UsageAccessScreenPreview() {
    PreviewSurface(padded = false) { UsageAccessScreen(onOpenSettings = {}, onLater = {}) }
}

@ScreenPreviews
@Composable
private fun UsageAccessScreenRestrictedPreview() {
    PreviewSurface(padded = false) {
        UsageAccessScreen(onOpenSettings = {}, onLater = {}, showRestrictedHelp = true, onOpenAppInfo = {})
    }
}
