package com.marzouki.datasaver.presentation.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.marzouki.datasaver.R
import com.marzouki.datasaver.ui.components.DataKeeperCard
import com.marzouki.datasaver.ui.components.PreviewSurface
import com.marzouki.datasaver.ui.components.ThemePreviews
import kotlinx.coroutines.flow.Flow

/** Collecte les effets MVI uniquement lorsque l'écran est au moins STARTED. */
@Composable
fun <E> CollectEffects(effects: Flow<E>, onEffect: suspend (E) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEffect by rememberUpdatedState(onEffect)
    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            effects.collect { currentOnEffect(it) }
        }
    }
}

/** Ouvre l'écran système "Accès aux données d'utilisation", sur l'application si possible. */
fun openUsageAccessSettings(context: Context) {
    val targeted = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        .setData(Uri.fromParts("package", context.packageName, null))
    try {
        context.startActivity(targeted)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
}

/** Ouvre la page "Infos sur l'appli" de DataKeeper (menu ⋮ › Autoriser les paramètres restreints). */
fun openAppDetailsSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", context.packageName, null)),
    )
}

@Composable
fun UsageAccessRequiredCard(onGrantClick: () -> Unit, modifier: Modifier = Modifier) {
    DataKeeperCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.usage_access_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.usage_access_body), style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = onGrantClick,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) { Text(stringResource(R.string.usage_access_button)) }
        }
    }
}

@ThemePreviews
@Composable
private fun UsageAccessRequiredCardPreview() {
    PreviewSurface { UsageAccessRequiredCard(onGrantClick = {}) }
}
