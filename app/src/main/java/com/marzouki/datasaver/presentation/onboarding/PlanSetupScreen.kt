package com.marzouki.datasaver.presentation.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.SavedPlan
import com.marzouki.datasaver.presentation.quota.PlanCycleFields
import com.marzouki.datasaver.ui.components.PreviewSurface
import com.marzouki.datasaver.ui.components.ScreenPreviews
import com.marzouki.datasaver.ui.components.formatDinars
import com.marzouki.datasaver.ui.components.toReadableSize
import com.marzouki.datasaver.ui.theme.numeric

private val PRESETS_GB = listOf(5, 10, 20, 25)

/**
 * Écran 1 : configuration du forfait avant le premier tableau de bord, pour que ses chiffres
 * partent du vrai forfait (ex. 25 Go dont 19 restants) et non d'un cycle au hasard.
 */
@Composable
fun PlanSetupScreen(modifier: Modifier = Modifier, viewModel: PlanSetupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // L'alerte de quota et le rappel de renouvellement sont des notifications : on demande la
    // permission avant d'enregistrer, car l'écran disparaît dès que le forfait est connu.
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.onIntent(PlanSetupIntent.Submit)
        }
    PlanSetupContent(
        state,
        onIntent = { intent ->
            val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (intent == PlanSetupIntent.Submit && state.canSubmit && needsPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onIntent(intent)
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanSetupContent(state: PlanSetupState, onIntent: (PlanSetupIntent) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .padding(24.dp),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    if (state.operatorName != null) {
                        stringResource(R.string.setup_title_operator, state.operatorName)
                    } else {
                        stringResource(R.string.setup_title)
                    },
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
                )
                Text(
                    stringResource(if (state.operatorChanged) R.string.setup_body_operator_changed else R.string.setup_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val chipColors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                // 0. Forfaits déjà utilisés chez cet opérateur : un appui remplit tout.
                if (state.savedPlans.isNotEmpty()) {
                    Text(
                        state.operatorName?.let { stringResource(R.string.setup_saved_plans_label, it) }
                            ?: stringResource(R.string.setup_saved_plans_label_generic),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.savedPlans.forEach { plan ->
                            SavedPlanChip(
                                plan = plan,
                                selected = state.planBytes == plan.limitBytes &&
                                    state.planPriceMillimes == plan.priceMillimes &&
                                    state.cycle.validityDays == plan.validityDays,
                                onClick = { onIntent(PlanSetupIntent.SavedPlanSelected(plan)) },
                                onForget = { onIntent(PlanSetupIntent.SavedPlanForgotten(plan)) },
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.setup_saved_plans_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // 1. Forfait.
                Text(stringResource(R.string.setup_plan_label), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESETS_GB.forEach { gb ->
                        FilterChip(
                            selected = state.planInput == gb.toString(),
                            onClick = { onIntent(PlanSetupIntent.PresetSelected(gb)) },
                            label = { Text(stringResource(R.string.quota_preset, gb), style = MaterialTheme.typography.labelLarge.numeric()) },
                            shape = CircleShape,
                            colors = chipColors,
                        )
                    }
                }
                GigabyteField(
                    value = state.planInput,
                    onValueChange = { onIntent(PlanSetupIntent.PlanChanged(it)) },
                    label = stringResource(R.string.setup_plan_input),
                    placeholder = stringResource(R.string.setup_plan_placeholder),
                    supporting = stringResource(R.string.quota_input_error).takeIf { state.showErrors && state.planError },
                    isError = state.showErrors && state.planError,
                )

                // 2. Data restante chez l'opérateur.
                Text(stringResource(R.string.setup_remaining_label), style = MaterialTheme.typography.titleMedium)
                val planBytes = state.planBytes
                GigabyteField(
                    value = state.remainingInput,
                    onValueChange = { onIntent(PlanSetupIntent.RemainingChanged(it)) },
                    label = stringResource(R.string.quota_balance_input_label),
                    placeholder = stringResource(R.string.setup_remaining_placeholder),
                    supporting = if (state.remainingError && planBytes != null) {
                        stringResource(R.string.quota_balance_input_error, planBytes.toReadableSize())
                    } else {
                        stringResource(R.string.setup_remaining_hint)
                    },
                    isError = state.remainingError,
                )

                // 3. Durée : pass de 30 jours, de 4 jours, ou abonnement qui repart chaque mois.
                Text(stringResource(R.string.setup_validity_label), style = MaterialTheme.typography.titleMedium)
                PlanCycleFields(state.cycle, onChange = { onIntent(PlanSetupIntent.CycleChanged(it)) })

                // 4. Prix du forfait, ramené au Go pour chiffrer les économies.
                Text(stringResource(R.string.setup_price_label), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.priceInput,
                    onValueChange = { onIntent(PlanSetupIntent.PriceChanged(it)) },
                    label = { Text(stringResource(R.string.setup_price_input_label)) },
                    placeholder = { Text(stringResource(R.string.setup_price_placeholder)) },
                    suffix = { Text(stringResource(R.string.unit_dinar)) },
                    supportingText = {
                        val pricePerGb = state.pricePerGb
                        Text(
                            when {
                                state.priceError -> stringResource(R.string.setup_price_error)
                                pricePerGb != null -> stringResource(
                                    R.string.setup_price_per_gb,
                                    formatDinars(pricePerGb.millimes.toLong()),
                                )
                                else -> stringResource(R.string.setup_price_hint)
                            },
                        )
                    },
                    isError = state.priceError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onIntent(PlanSetupIntent.Submit) },
                enabled = !state.isSaving,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) { Text(stringResource(R.string.setup_submit), style = MaterialTheme.typography.labelLarge) }
            TextButton(
                onClick = { onIntent(PlanSetupIntent.Skip) },
                enabled = !state.isSaving,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) { Text(stringResource(R.string.setup_skip), style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/** « 25 Go · 28,5 DT · 30 j », avec une croix pour l'oublier. */
@Composable
private fun SavedPlanChip(plan: SavedPlan, selected: Boolean, onClick: () -> Unit, onForget: () -> Unit) {
    val parts = buildList {
        add(plan.limitBytes.toReadableSize())
        plan.priceMillimes?.let { add(formatDinars(it)) }
        add(
            plan.validityDays?.let { pluralStringResource(R.plurals.plan_cycle_validity_short, it, it) }
                ?: stringResource(R.string.plan_cycle_monthly_chip),
        )
    }
    InputChip(
        selected = selected,
        onClick = onClick,
        label = { Text(parts.joinToString(" · "), style = MaterialTheme.typography.labelLarge.numeric()) },
        trailingIcon = {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.setup_saved_plan_forget),
                modifier = Modifier
                    .size(InputChipDefaults.IconSize)
                    .clickable(onClick = onForget),
            )
        },
        shape = CircleShape,
        colors = InputChipDefaults.inputChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@Composable
private fun GigabyteField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    supporting: String?,
    isError: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        suffix = { Text(stringResource(R.string.unit_gb)) },
        supportingText = supporting?.let { { Text(it) } },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}

@ScreenPreviews
@Composable
private fun PlanSetupPreview() {
    PreviewSurface(padded = false) {
        PlanSetupContent(
            PlanSetupState(
                planInput = "25", remainingInput = "19", priceInput = "28,5", operatorName = "Ooredoo",
                savedPlans = listOf(
                    SavedPlan("60503", 25_000_000_000L, 28_500, 30),
                    SavedPlan("60503", 220_000_000L, 950, 4),
                ),
            ),
            onIntent = {},
        )
    }
}
