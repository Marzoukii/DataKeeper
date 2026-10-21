package com.marzouki.datasaver.presentation.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 32.dp),
            ) {
                Logo()
                Spacer(Modifier.height(32.dp))
                Illustration()
                Spacer(Modifier.height(32.dp))
                Text(
                    stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.onboarding_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CheckPoint(stringResource(R.string.onboarding_point_volumes))
                    CheckPoint(stringResource(R.string.onboarding_point_local))
                    CheckPoint(stringResource(R.string.onboarding_point_revoke))
                }
                Spacer(Modifier.height(24.dp))
                // Dans la zone défilante : avec ses étapes, la carte ne tiendrait pas sur un petit écran.
                if (showRestrictedHelp) {
                    RestrictedSettingsHelp(onOpenAppInfo, Modifier.padding(bottom = 16.dp))
                }
            }
            Button(
                onClick = onOpenSettings,
                shape = CircleShape,
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
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            Icon(
                Icons.Rounded.Wifi,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(12.dp)
                    .offset(y = (-1).dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
    }
}

/** Carte avec quatre barres Wi-Fi et une petite barre mobile, et une bulle Wi-Fi. */
@Composable
private fun Illustration() {
    val description = stringResource(R.string.onboarding_illustration_description)
    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        DataKeeperCard(
            Modifier
                .width(240.dp)
                .height(150.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Bottom,
            ) {
                listOf(0.55f, 0.85f, 0.65f, 1f).forEach { IllustrationBar(DataColors.wifi, it) }
                IllustrationBar(DataColors.mobile, 0.22f)
            }
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = 24.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        }
    }
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
