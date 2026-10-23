package com.marzouki.datasaver.ui.components

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.enums.AppKind
import com.marzouki.datasaver.domain.model.AppDataUsage
import com.marzouki.datasaver.ui.theme.DataColors
import com.marzouki.datasaver.ui.theme.numeric

private val IconSize = 36.dp

/** Ligne d'application : icône, nom, data mobile et barre fine proportionnelle à [fraction]. */
@Composable
fun AppUsageRow(
    icon: ImageBitmap?,
    name: String,
    bytes: Long,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(icon, name)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(bytes.toReadableSize(), style = MaterialTheme.typography.labelLarge.numeric())
            }
            ThinBar(fraction, DataColors.mobile)
        }
    }
}

@Composable
private fun AppIcon(icon: ImageBitmap?, name: String) {
    val shape = RoundedCornerShape(12.dp)
    if (icon != null) {
        Image(icon, contentDescription = null, modifier = Modifier.size(IconSize).clip(shape))
    } else {
        // Repli : initiale de l'appli sur fond mobile.
        Box(
            Modifier
                .size(IconSize)
                .clip(shape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.firstOrNull()?.uppercase().orEmpty(),
                style = MaterialTheme.typography.titleSmall.numeric(),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

/** Icône de l'application via le [PackageManager], ou null si elle est introuvable. */
@Composable
fun rememberAppIcon(packageName: String?): ImageBitmap? {
    val packageManager = LocalContext.current.packageManager
    return remember(packageName) {
        packageName?.let {
            try {
                packageManager.getApplicationIcon(it).toBitmap(width = 96, height = 96).asImageBitmap()
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }
    }
}

/** Nom affiché d'une entrée du classement, y compris pour les catégories système. */
@Composable
fun AppDataUsage.displayName(): String = when (kind) {
    AppKind.APP -> label ?: packageName ?: stringResource(R.string.usage_app_unknown)
    AppKind.SYSTEM -> stringResource(R.string.usage_app_system)
    AppKind.TETHERING -> stringResource(R.string.usage_app_tethering)
    AppKind.REMOVED -> stringResource(R.string.usage_app_removed)
}

@ThemePreviews
@Composable
private fun AppUsageRowPreview() {
    PreviewSurface {
        DataKeeperCard {
            val total = PreviewData.usage.mobileBytes.toFloat()
            PreviewData.topApps.forEach { app ->
                AppUsageRow(icon = null, name = app.displayName(), bytes = app.mobileBytes, fraction = app.mobileBytes / total)
            }
        }
    }
}
