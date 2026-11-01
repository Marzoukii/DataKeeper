package com.marzouki.datasaver.presentation.onboarding

import com.marzouki.datasaver.ui.components.DataKeeperCard
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import com.marzouki.datasaver.ui.theme.Brand
import com.marzouki.datasaver.ui.components.heroSurface
import com.marzouki.datasaver.ui.components.glowBackground
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.HorizontalDivider
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

private const val STEP_SIZE = 0
private const val STEP_VALIDITY = 1
private const val STEP_BALANCE = 2
private const val STEP_READY = 3
private const val STEP_COUNT = 3

/**
 * Le forfait en trois étapes, une question par écran : la taille, la durée, puis le solde et le prix
 * (facultatifs). Un écran « Tout est prêt » récapitule avant d'ouvrir le tableau de bord.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanSetupContent(state: PlanSetupState, onIntent: (PlanSetupIntent) -> Unit, modifier: Modifier = Modifier) {
    var step by rememberSaveable { mutableIntStateOf(STEP_SIZE) }
    var showStepError by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = step > STEP_SIZE) { step -= 1 }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            if (step < STEP_READY) {
                StepHeader(
                    step = step,
                    onBack = if (step > STEP_SIZE) ({ step -= 1 }) else null,
                    onSkip = if (step == STEP_BALANCE) ({ step = STEP_READY }) else null,
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (step) {
                    STEP_SIZE -> SizeStep(state, onIntent, showStepError)
                    STEP_VALIDITY -> ValidityStep(state, onIntent)
                    STEP_BALANCE -> BalanceStep(state, onIntent)
                    else -> ReadyStep(state)
                }
            }
            val (label, enabled) = when (step) {
                STEP_SIZE -> stringResource(R.string.setup_next) to true
                STEP_VALIDITY -> stringResource(R.string.setup_next) to state.cycle.isValid
                STEP_BALANCE -> stringResource(R.string.setup_finish) to (!state.remainingError && !state.priceError)
                else -> stringResource(R.string.setup_submit) to !state.isSaving
            }
            Button(
                onClick = {
                    when (step) {
                        STEP_SIZE -> if (state.planError) showStepError = true else { showStepError = false; step = STEP_VALIDITY }
                        STEP_VALIDITY -> step = STEP_BALANCE
                        STEP_BALANCE -> step = STEP_READY
                        else -> onIntent(PlanSetupIntent.Submit)
                    }
                },
                enabled = enabled,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                if (step < STEP_READY) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (step == STEP_BALANCE) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            when (step) {
                STEP_SIZE -> TextButton(
                    onClick = { onIntent(PlanSetupIntent.Skip) },
                    enabled = !state.isSaving,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) { Text(stringResource(R.string.setup_skip), style = MaterialTheme.typography.labelLarge) }
                STEP_READY -> TextButton(
                    onClick = { step = STEP_SIZE },
                    enabled = !state.isSaving,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) { Text(stringResource(R.string.setup_edit), style = MaterialTheme.typography.labelLarge) }
                else -> Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** Retour, « Étape 2 sur 3 », « Passer » éventuel, puis la barre de progression en trois segments. */
@Composable
private fun StepHeader(step: Int, onBack: (() -> Unit)?, onSkip: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(72.dp)) {
                if (onBack != null) {
                    FilledIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.settings_back)) }
                }
            }
            Text(
                stringResource(R.string.setup_step_of, step + 1, STEP_COUNT),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.width(72.dp), contentAlignment = Alignment.CenterEnd) {
                if (onSkip != null) {
                    TextButton(onClick = onSkip) { Text(stringResource(R.string.setup_skip_step)) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(STEP_COUNT) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
    }
}

@Composable
private fun StepTitle(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp, lineHeight = 34.sp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SizeStep(state: PlanSetupState, onIntent: (PlanSetupIntent) -> Unit, showError: Boolean) {
    StepTitle(
        stringResource(R.string.setup_q_size),
        state.operatorName?.let { stringResource(R.string.setup_q_size_body_operator, it) } ?: stringResource(R.string.setup_q_size_body),
    )
    if (state.operatorChanged) {
        val change = state.operatorChange
        Text(
            if (change != null) {
                stringResource(
                    R.string.setup_operator_changed_at,
                    change.operatorName,
                    java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(change.atMillis)),
                )
            } else {
                stringResource(R.string.setup_body_operator_changed)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (state.savedPlans.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                state.operatorName?.let { stringResource(R.string.setup_saved_plans_label, it) }
                    ?: stringResource(R.string.setup_saved_plans_label_generic),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }
    }
    // Grosses tuiles : les tailles courantes, puis « Autre » pour saisir la sienne.
    val tiles: List<Int?> = PLAN_PRESETS_GB + listOf(null)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { gb ->
                    val selected = if (gb == null) state.showsCustomPlan else !state.showsCustomPlan && state.planInput == gb.toString()
                    SizeTile(
                        value = gb?.toString(),
                        selected = selected,
                        onClick = {
                            if (gb == null) onIntent(PlanSetupIntent.CustomPlanSelected) else onIntent(PlanSetupIntent.PresetSelected(gb))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    val planError = (showError || state.showErrors) && state.planError
    if (state.showsCustomPlan) {
        val planBytes = state.planBytes
        GigabyteField(
            value = state.planInput,
            onValueChange = { onIntent(PlanSetupIntent.PlanChanged(it)) },
            label = stringResource(R.string.setup_plan_input),
            placeholder = stringResource(R.string.setup_plan_placeholder),
            supporting = when {
                planError -> stringResource(R.string.quota_input_error)
                planBytes != null -> stringResource(R.string.setup_plan_custom_hint, planBytes.toReadableSize())
                else -> stringResource(R.string.setup_plan_custom_empty)
            },
            isError = planError,
        )
    } else if (planError) {
        Text(
            stringResource(R.string.setup_plan_choose_error),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/** Tuile de taille : le nombre en grand et « Go » dessous ; pleine et rouge corail une fois choisie. */
@Composable
private fun SizeTile(value: String?, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val container = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        selected = selected,
        shape = RoundedCornerShape(18.dp),
        color = container,
        contentColor = content,
        shadowElevation = if (selected) 6.dp else 2.dp,
        modifier = modifier.height(84.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (value != null) {
                Text(value, style = MaterialTheme.typography.headlineMedium.numeric(), fontWeight = FontWeight.Medium)
                Text(
                    stringResource(R.string.unit_gb),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) content.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = if (selected) content else MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.setup_plan_custom_chip),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) content.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ValidityStep(state: PlanSetupState, onIntent: (PlanSetupIntent) -> Unit) {
    StepTitle(stringResource(R.string.setup_q_validity), stringResource(R.string.setup_q_validity_body))
    DataKeeperCard(contentPadding = 16.dp) {
        PlanCycleFields(state.cycle, onChange = { onIntent(PlanSetupIntent.CycleChanged(it)) })
    }
}

@Composable
private fun BalanceStep(state: PlanSetupState, onIntent: (PlanSetupIntent) -> Unit) {
    StepTitle(stringResource(R.string.setup_q_balance), stringResource(R.string.setup_q_balance_body))
    val planBytes = state.planBytes
    DataKeeperCard(contentPadding = 16.dp) {
        GigabyteField(
            value = state.remainingInput,
            onValueChange = { onIntent(PlanSetupIntent.RemainingChanged(it)) },
            label = stringResource(R.string.setup_remaining_label),
            placeholder = stringResource(R.string.setup_remaining_placeholder),
            supporting = if (state.remainingError && planBytes != null) {
                stringResource(R.string.quota_balance_input_error, planBytes.toReadableSize())
            } else {
                stringResource(R.string.setup_remaining_hint)
            },
            isError = state.remainingError,
        )
    }
    DataKeeperCard(contentPadding = 16.dp) {
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
                        pricePerGb != null -> stringResource(R.string.setup_price_per_gb, formatDinars(pricePerGb.millimes.toLong()))
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
}

/** « Tout est prêt ! » : coche entourée de confettis et récapitulatif du forfait. */
@Composable
private fun ReadyStep(state: PlanSetupState) {
    val accent = MaterialTheme.colorScheme.primary
    val tint = MaterialTheme.colorScheme.primaryContainer
    val onAccent = MaterialTheme.colorScheme.onPrimary
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Canvas(Modifier.size(width = 200.dp, height = 150.dp).padding(top = 8.dp)) {
            val c = Offset(size.width / 2, size.height * 0.55f)
            drawCircle(tint, radius = size.height * 0.42f, center = c)
            drawCircle(accent, radius = size.height * 0.30f, center = c)
            val check = Path().apply {
                moveTo(c.x - size.height * 0.13f, c.y)
                lineTo(c.x - size.height * 0.03f, c.y + size.height * 0.10f)
                lineTo(c.x + size.height * 0.16f, c.y - size.height * 0.10f)
            }
            drawPath(check, onAccent, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            listOf(
                Triple(0.12f, 0.22f, Brand.Mobile), Triple(0.86f, 0.15f, Brand.Wifi), Triple(0.88f, 0.78f, Brand.Mobile),
                Triple(0.14f, 0.80f, Brand.Wifi), Triple(0.74f, 0.05f, accent), Triple(0.28f, 0.06f, accent),
            ).forEach { (x, y, color) -> drawCircle(color, radius = 4.5.dp.toPx(), center = Offset(size.width * x, size.height * y)) }
        }
        Text(stringResource(R.string.setup_ready_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.setup_ready_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        DataKeeperCard(contentPadding = 0.dp) {
            val validity = state.cycle.validityDays
            SummaryRow(
                stringResource(R.string.setup_ready_plan),
                listOfNotNull(state.operatorName, state.planBytes?.toReadableSize()).joinToString(" "),
                first = true,
            )
            SummaryRow(
                stringResource(R.string.setup_ready_validity),
                validity?.let { pluralStringResource(R.plurals.days, it, it) } ?: stringResource(R.string.plan_cycle_monthly_chip),
            )
            SummaryRow(
                stringResource(R.string.setup_ready_remaining),
                state.remainingBytes?.toReadableSize() ?: (state.planBytes?.toReadableSize() ?: "—"),
                highlight = true,
            )
            SummaryRow(
                stringResource(R.string.setup_ready_price),
                state.planPriceMillimes?.let { price ->
                    val perGb = state.pricePerGb?.let { " · " + stringResource(R.string.setup_price_per_gb, formatDinars(it.millimes.toLong())) }.orEmpty()
                    formatDinars(price) + perGb
                } ?: "—",
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, first: Boolean = false, highlight: Boolean = false) {
    if (!first) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.numeric(),
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
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
                planInput = "30", remainingInput = "22,7", priceInput = "28,5", operatorName = "Ooredoo",
                savedPlans = listOf(
                    SavedPlan("60503", 25_000_000_000L, 28_500, 30),
                    SavedPlan("60503", 220_000_000L, 950, 4),
                ),
            ),
            onIntent = {},
        )
    }
}
