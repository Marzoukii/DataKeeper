package com.marzouki.datasaver.presentation.settings

import com.marzouki.datasaver.ui.components.BackHeader
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.enums.AppLanguageChoice
import com.marzouki.datasaver.domain.enums.ThemeMode
import com.marzouki.datasaver.ui.components.DataKeeperCard
import com.marzouki.datasaver.ui.components.PreviewSurface
import com.marzouki.datasaver.ui.components.ScreenPreviews
import com.marzouki.datasaver.ui.theme.Spacing

/** Ordre de la maquette : clair, sombre, puis « Système ». */
private val ThemeChoices = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)

/** Langues proposées ; « Langue du téléphone » reste le réglage par défaut, sans ligne à lui. */
private val LanguageChoices = listOf(AppLanguageChoice.FRENCH, AppLanguageChoice.ENGLISH, AppLanguageChoice.ARABIC)

/** Réglages de l'appli : thème, langue et version. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val version = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    SettingsContent(state, version, viewModel::onIntent, onBack, modifier)
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    version: String,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Sans choix dans l'appli, on coche la langue réellement affichée (le français pour une langue non traduite).
    val locale = LocalConfiguration.current.locales[0]
    val displayedLanguage = state.language.takeIf { it != AppLanguageChoice.SYSTEM }
        ?: AppLanguageChoice.fromTag(locale.toLanguageTag()).takeIf { it != AppLanguageChoice.SYSTEM }
        ?: AppLanguageChoice.FRENCH

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.betweenCards),
    ) {
        BackHeader(
            title = stringResource(R.string.settings_title),
            backDescription = stringResource(R.string.settings_back),
            onBack = onBack,
        )

        SectionLabel(R.string.settings_appearance)
        Row(
            Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeChoices.forEach { mode ->
                ThemeCard(
                    mode = mode,
                    selected = mode == state.themeMode,
                    onClick = { onIntent(SettingsIntent.ThemeSelected(mode)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        SectionLabel(R.string.language_title)
        DataKeeperCard(contentPadding = 0.dp, modifier = Modifier.selectableGroup()) {
            LanguageChoices.forEachIndexed { index, language ->
                if (index > 0) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
                LanguageRow(
                    language = language,
                    selected = language == displayedLanguage,
                    onClick = { onIntent(SettingsIntent.LanguageSelected(language)) },
                )
            }
        }

        SectionLabel(R.string.settings_about)
        DataKeeperCard(contentPadding = 0.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.settings_version), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(version, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionLabel(@StringRes textRes: Int) {
    val locale = LocalConfiguration.current.locales[0]
    Text(
        stringResource(textRes),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = Spacing.titleExtra, top = 8.dp),
    )
}

/** Carte de thème : icône ronde et nom ; la carte choisie est bleutée avec une bordure primaire. */
@Composable
private fun ThemeCard(mode: ThemeMode, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.heightIn(min = 96.dp),
        shape = MaterialTheme.shapes.large,
        color = if (selected) colors.primaryContainer else colors.surface,
        contentColor = colors.onSurface,
        border = if (selected) BorderStroke(2.dp, colors.primary) else BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(
            Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (selected) colors.surface else colors.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    mode.icon,
                    contentDescription = null,
                    tint = if (selected) colors.primary else colors.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                stringResource(mode.labelRes),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/** Ligne de langue : code dans un badge, nom dans la langue de l'appli, puis dans sa propre langue. */
@Composable
private fun LanguageRow(language: AppLanguageChoice, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                language.tag.orEmpty().uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(language.nameRes), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(language.nativeNameRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        SelectionMark(selected)
    }
}

/** Rond plein et coché pour la langue choisie, simple cercle sinon. */
@Composable
private fun SelectionMark(selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(24.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(colors.primary)
                } else {
                    Modifier.border(1.5.dp, colors.outline, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }

private val ThemeMode.icon: ImageVector
    get() = when (this) {
        ThemeMode.SYSTEM -> Icons.Rounded.Contrast
        ThemeMode.LIGHT -> Icons.Rounded.LightMode
        ThemeMode.DARK -> Icons.Rounded.DarkMode
    }

/** Nom de la langue dans la langue de l'appli : « Anglais », « English », « الإنجليزية ». */
private val AppLanguageChoice.nameRes: Int
    get() = when (this) {
        AppLanguageChoice.SYSTEM, AppLanguageChoice.FRENCH -> R.string.language_french_name
        AppLanguageChoice.ENGLISH -> R.string.language_english_name
        AppLanguageChoice.ARABIC -> R.string.language_arabic_name
    }

/** Chaque langue s'écrit aussi dans sa propre langue (« English », « العربية »), pour qu'on la reconnaisse. */
private val AppLanguageChoice.nativeNameRes: Int
    get() = when (this) {
        AppLanguageChoice.SYSTEM, AppLanguageChoice.FRENCH -> R.string.language_french
        AppLanguageChoice.ENGLISH -> R.string.language_english
        AppLanguageChoice.ARABIC -> R.string.language_arabic
    }

@ScreenPreviews
@Composable
private fun SettingsContentPreview() {
    PreviewSurface(padded = false) {
        SettingsContent(SettingsState(ThemeMode.SYSTEM, AppLanguageChoice.FRENCH), version = "1.0", onIntent = {}, onBack = {})
    }
}
