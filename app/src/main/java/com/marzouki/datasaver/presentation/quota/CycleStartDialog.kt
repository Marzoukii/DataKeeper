package com.marzouki.datasaver.presentation.quota

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.ui.components.PreviewSurface
import com.marzouki.datasaver.ui.components.ThemePreviews
import com.marzouki.datasaver.ui.components.formatDayMonth
import com.marzouki.datasaver.ui.components.formatMonthYear
import com.marzouki.datasaver.ui.theme.numeric
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val WEEKDAY_INITIALS = listOf("L", "M", "M", "J", "V", "S", "D")

/** Mois affichables autour du mois courant : assez pour retrouver la date de début du forfait. */
private const val MIN_MONTH_OFFSET = -2
private const val MAX_MONTH_OFFSET = 1

/**
 * Calendrier réel du mois (jours de la semaine, aujourd'hui) où l'on touche la date de début du
 * cycle. La plage du cycle qui en découle est surlignée avant d'enregistrer.
 */
@Composable
internal fun CycleStartDialog(
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    nowMillis: Long = remember { System.currentTimeMillis() },
) {
    var pendingDay by rememberSaveable { mutableIntStateOf(selected) }
    // Ouvre sur le mois où a commencé le cycle en cours.
    var monthOffset by rememberSaveable {
        mutableIntStateOf(monthsBetween(nowMillis, MonthPeriod.current(nowMillis, selected).startMillis))
    }
    val period = MonthPeriod.current(nowMillis, pendingDay)
    val lastDayMillis = Calendar.getInstance().apply {
        timeInMillis = period.nextStartMillis
        add(Calendar.DAY_OF_MONTH, -1)
    }.timeInMillis
    val month = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, monthOffset)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quota_cycle_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.quota_cycle_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                MonthHeader(
                    title = formatMonthYear(month.timeInMillis),
                    canGoBack = monthOffset > MIN_MONTH_OFFSET,
                    canGoForward = monthOffset < MAX_MONTH_OFFSET,
                    onBack = { monthOffset-- },
                    onForward = { monthOffset++ },
                )
                MonthGrid(
                    month = month,
                    todayKey = dayKey(nowMillis),
                    rangeStartKey = dayKey(period.startMillis),
                    rangeEndKey = dayKey(lastDayMillis),
                    resetKey = dayKey(period.nextStartMillis),
                    onDayClick = { day ->
                        pendingDay = day
                        // Le jour touché peut ouvrir un cycle commencé le mois d'avant : on y va pour le montrer.
                        monthOffset = monthsBetween(nowMillis, MonthPeriod.current(nowMillis, day).startMillis)
                    },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(
                        R.string.quota_cycle_range,
                        formatDayMonth(period.startMillis),
                        formatDayMonth(lastDayMillis),
                    ),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(
                        R.string.quota_cycle_reset,
                        pluralStringResource(R.plurals.days, period.totalDays, period.totalDays),
                        formatDayMonth(period.nextStartMillis),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.quota_cycle_max_day),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSelect(pendingDay) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun MonthHeader(
    title: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, enabled = canGoBack) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.quota_cycle_previous_month))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onForward, enabled = canGoForward) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.quota_cycle_next_month))
        }
    }
}

@Composable
private fun MonthGrid(
    month: Calendar,
    todayKey: Int,
    rangeStartKey: Int,
    rangeEndKey: Int,
    resetKey: Int,
    onDayClick: (Int) -> Unit,
) {
    val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Semaine du lundi au dimanche : Calendar.MONDAY (2) en colonne 0.
    val leadingBlanks = (month.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val cells: List<Int?> = List(leadingBlanks) { null } + (1..daysInMonth)
    val year = month.get(Calendar.YEAR)
    val monthIndex = month.get(Calendar.MONTH)

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row {
            WEEKDAY_INITIALS.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp)
                        .clearAndSetSemantics { },
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row {
                week.forEachIndexed { column, day ->
                    if (day == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val key = dayKey(year, monthIndex, day)
                        DayCell(
                            day = day,
                            dateMillis = Calendar.getInstance().apply { set(year, monthIndex, day) }.timeInMillis,
                            inRange = key in rangeStartKey..rangeEndKey,
                            isStart = key == rangeStartKey,
                            roundStart = key == rangeStartKey || column == 0 || day == 1,
                            roundEnd = key == rangeEndKey || column == 6 || day == daysInMonth,
                            isToday = key == todayKey,
                            isReset = key == resetKey,
                            enabled = day <= MonthPeriod.MAX_CYCLE_START_DAY,
                            onClick = { onDayClick(day) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    dateMillis: Long,
    inRange: Boolean,
    isStart: Boolean,
    roundStart: Boolean,
    roundEnd: Boolean,
    isToday: Boolean,
    isReset: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val band = RoundedCornerShape(
        topStartPercent = if (roundStart) 50 else 0,
        bottomStartPercent = if (roundStart) 50 else 0,
        topEndPercent = if (roundEnd) 50 else 0,
        bottomEndPercent = if (roundEnd) 50 else 0,
    )
    val description = buildList {
        add(SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE).format(Date(dateMillis)))
        if (isStart) add(stringResource(R.string.quota_cycle_day_start))
        if (isReset) add(stringResource(R.string.quota_cycle_day_reset))
        if (isToday) add(stringResource(R.string.quota_cycle_day_today))
    }.joinToString(", ")

    Box(
        modifier
            .height(40.dp)
            .background(if (inRange) colors.primaryContainer else Color.Transparent, band)
            .clip(band)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                contentDescription = description
                selected = isStart
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(if (isStart) Modifier.background(colors.primary) else Modifier)
                .then(if (isReset && !isStart) Modifier.border(1.5.dp, colors.primary, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.toString(),
                style = MaterialTheme.typography.labelLarge.numeric(),
                fontWeight = if (isToday) FontWeight.Bold else null,
                color = when {
                    isStart -> colors.onPrimary
                    !enabled -> colors.onSurface.copy(alpha = 0.38f)
                    inRange -> colors.onPrimaryContainer
                    else -> colors.onSurface
                },
            )
            if (isToday) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isStart) colors.onPrimary else colors.primary),
                )
            }
        }
    }
}

/** Date comparable sous la forme aaaammjj, sans souci de fuseau ni d'heure d'été. */
private fun dayKey(year: Int, month: Int, day: Int): Int = year * 10_000 + (month + 1) * 100 + day

private fun dayKey(millis: Long): Int {
    val calendar = Calendar.getInstance().apply { timeInMillis = millis }
    return dayKey(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
}

private fun monthsBetween(fromMillis: Long, toMillis: Long): Int {
    val from = Calendar.getInstance().apply { timeInMillis = fromMillis }
    val to = Calendar.getInstance().apply { timeInMillis = toMillis }
    return (to.get(Calendar.YEAR) - from.get(Calendar.YEAR)) * 12 + to.get(Calendar.MONTH) - from.get(Calendar.MONTH)
}

@ThemePreviews
@Composable
private fun CycleStartDialogPreview() {
    PreviewSurface { CycleStartDialog(selected = 25, onSelect = {}, onDismiss = {}) }
}
