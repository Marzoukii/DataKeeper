package com.marzouki.datasaver.presentation.quota

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.PlanValidity
import com.marzouki.datasaver.ui.components.DataKeeperCard
import com.marzouki.datasaver.ui.components.SettingRow
import com.marzouki.datasaver.ui.components.formatDayMonth
import com.marzouki.datasaver.ui.components.formatDayOrdinal
import com.marzouki.datasaver.ui.theme.numeric
import java.util.Calendar
import java.util.TimeZone

/** Validités proposées en un appui : celles des offres courantes (30 jours, une semaine, 4 jours). */
private val VALIDITY_PRESETS = listOf(30, 7, 4)

/**
 * Cycle du forfait tel que saisi : jour d'achat et validité en jours, ou renouvellement chaque mois
 * à cette date ([monthly]) pour un abonnement.
 */
data class PlanCycleInput(
    /** Minuit, heure locale, du jour d'achat ou de renouvellement. */
    val purchaseMillis: Long = startOfDay(System.currentTimeMillis()),
    val validityInput: String = VALIDITY_PRESETS.first().toString(),
    val monthly: Boolean = false,
) {
    /** Null pour un cycle mensuel ou une saisie invalide (voir [isValid]). */
    val validityDays: Int? get() = if (monthly) null else validityInput.trim().toIntOrNull()?.takeIf { it in 1..MonthPeriod.MAX_VALIDITY_DAYS }

    val isValid: Boolean get() = monthly || validityDays != null

    /** Cycle qui en découle aujourd'hui, pour annoncer sa fin. */
    fun period(nowMillis: Long = System.currentTimeMillis()): MonthPeriod {
        val days = validityDays
        return if (days != null) {
            MonthPeriod.pass(nowMillis, purchaseMillis, days)
        } else {
            MonthPeriod.current(nowMillis, dayOfMonth(purchaseMillis))
        }
    }

    companion object {
        /**
         * Cycle déjà enregistré, à partir du cycle en cours [current] : pour un pass, son dernier
         * renouvellement plutôt que le premier achat, qui peut dater de plusieurs cycles.
         */
        fun of(validity: PlanValidity?, current: MonthPeriod): PlanCycleInput =
            if (validity != null) {
                PlanCycleInput(current.startMillis, validity.days.toString(), monthly = false)
            } else {
                PlanCycleInput(current.startMillis, monthly = true)
            }

        fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        private fun dayOfMonth(millis: Long): Int =
            Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_MONTH)
    }
}

/** Jour d'achat, validité (30 j, 7 j, 4 j, autre ou chaque mois) et date de fin qui en découle. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanCycleFields(
    input: PlanCycleInput,
    onChange: (PlanCycleInput) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val period = input.period()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DataKeeperCard(contentPadding = 0.dp) {
            SettingRow(
                icon = Icons.Rounded.CalendarMonth,
                title = stringResource(R.string.plan_cycle_purchase_title, formatDayMonth(input.purchaseMillis)),
                subtitle = when {
                    !input.isValid -> stringResource(R.string.plan_cycle_purchase_subtitle)
                    input.monthly -> stringResource(
                        R.string.plan_cycle_monthly_summary,
                        formatDayOrdinal(Calendar.getInstance().apply { timeInMillis = input.purchaseMillis }.get(Calendar.DAY_OF_MONTH)),
                        formatDayMonth(period.nextStartMillis),
                    )
                    // Même date que l'opérateur : achat + validité (21 octobre + 30 jours → 20 novembre).
                    else -> stringResource(R.string.plan_cycle_pass_summary, formatDayMonth(period.nextStartMillis))
                },
                onClick = { showDatePicker = true },
            ) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = stringResource(R.string.plan_cycle_purchase_choose),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val chipColors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VALIDITY_PRESETS.forEach { days ->
                FilterChip(
                    selected = !input.monthly && input.validityDays == days,
                    onClick = { onChange(input.copy(validityInput = days.toString(), monthly = false)) },
                    label = {
                        Text(
                            pluralStringResource(R.plurals.plan_cycle_days_chip, days, days),
                            style = MaterialTheme.typography.labelLarge.numeric(),
                        )
                    },
                    shape = CircleShape,
                    colors = chipColors,
                )
            }
            FilterChip(
                selected = input.monthly,
                onClick = { onChange(input.copy(monthly = true)) },
                label = { Text(stringResource(R.string.plan_cycle_monthly_chip), style = MaterialTheme.typography.labelLarge) },
                shape = CircleShape,
                colors = chipColors,
            )
        }
        if (!input.monthly) {
            OutlinedTextField(
                value = input.validityInput,
                onValueChange = { onChange(input.copy(validityInput = it.filter(Char::isDigit).take(3))) },
                label = { Text(stringResource(R.string.plan_cycle_validity_label)) },
                suffix = { Text(stringResource(R.string.plan_cycle_validity_unit)) },
                isError = !input.isValid,
                supportingText = {
                    Text(
                        if (input.isValid) {
                            stringResource(R.string.plan_cycle_validity_hint)
                        } else {
                            stringResource(
                                R.string.plan_cycle_validity_error,
                                pluralStringResource(R.plurals.days, MonthPeriod.MAX_VALIDITY_DAYS, MonthPeriod.MAX_VALIDITY_DAYS),
                            )
                        },
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showDatePicker) {
        PurchaseDatePicker(
            selectedMillis = input.purchaseMillis,
            onSelect = {
                onChange(input.copy(purchaseMillis = it))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

/**
 * Le sélecteur de Material raisonne en jours UTC : on y convertit le jour local, et inversement,
 * pour que le 21 octobre choisi reste le 21 octobre quel que soit le fuseau.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseDatePicker(selectedMillis: Long, onSelect: (Long) -> Unit, onDismiss: () -> Unit) {
    val todayUtc = localDayToUtc(System.currentTimeMillis())
    val earliestUtc = todayUtc - MonthPeriod.MAX_VALIDITY_DAYS * DAY_MILLIS
    val state = rememberDatePickerState(
        initialSelectedDateMillis = localDayToUtc(selectedMillis),
        selectableDates = object : SelectableDates {
            // Un forfait ne s'achète pas dans le futur.
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in earliestUtc..todayUtc
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onSelect(utcDayToLocal(it)) } ?: onDismiss() },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state = state, title = {
            Text(
                stringResource(R.string.plan_cycle_purchase_choose),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
            )
        })
    }
}

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

private fun localDayToUtc(millis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = millis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun utcDayToLocal(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
