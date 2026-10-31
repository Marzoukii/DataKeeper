package com.marzouki.datasaver.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.marzouki.datasaver.ui.theme.Brand

/**
 * En-tête d'écran : bloc bleu nuit à lueurs, ligne de contexte sous le titre.
 * Prévu pour une colonne à marge de 16 dp : le bloc occupe la largeur de la colonne.
 */
@Composable
fun ScreenHeader(
    overline: String,
    title: String,
    modifier: Modifier = Modifier,
    overlineColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(28.dp))
            .heroSurface()
            .padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(overline, style = MaterialTheme.typography.bodyMedium, color = overlineColor)
        }
        action?.invoke()
    }
}

/** En-tête des sous-écrans (réglages, partage de connexion) : bloc bleu nuit avec la flèche retour. */
@Composable
fun BackHeader(
    title: String,
    backDescription: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(28.dp))
            .heroSurface()
            .padding(start = 8.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = backDescription, tint = MaterialTheme.colorScheme.onSurface)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Bouton icône rond de l'en-tête, en verre clair sur le bleu nuit. */
@Composable
fun HeaderIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier.padding(start = 8.dp),
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@ThemePreviews
@Composable
private fun ScreenHeaderPreview() {
    PreviewSurface {
        ScreenHeader("Forfait Ooredoo · jour 20 sur 30", "Ma consommation") {
            HeaderIconButton(Icons.Rounded.Settings, "Réglages", onClick = {})
        }
    }
}
