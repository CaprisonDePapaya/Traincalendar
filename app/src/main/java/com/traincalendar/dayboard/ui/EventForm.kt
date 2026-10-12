package com.traincalendar.dayboard.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traincalendar.dayboard.data.BoardEvent
import com.traincalendar.dayboard.data.DEFAULT_LINES
import com.traincalendar.dayboard.data.Line
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** The editable fields of one of the user's own events. */
data class EventDraft(
    val title: String = "",
    val location: String = "",
    val date: LocalDate,
    val time: LocalTime,
    val durationMin: Int = 30,
    val allDay: Boolean = false,
    val lineId: String,
) {
    fun toMillis(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        return if (allDay) {
            date.atStartOfDay(zone).toInstant().toEpochMilli() to
                date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        } else {
            val start = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
            start to start + durationMin * 60_000L
        }
    }
}

/** A blank event: next half hour today, or 9 AM on another day. */
fun newDraft(state: BoardUiState): EventDraft {
    val lineId = state.lines.firstOrNull()?.id ?: DEFAULT_LINES.first().id
    if (!state.isToday) return EventDraft(date = state.date, time = LocalTime.of(9, 0), lineId = lineId)
    val now = LocalDateTime.now()
    val next = now.truncatedTo(ChronoUnit.HOURS).plusMinutes(if (now.minute < 30) 30L else 60L)
    return EventDraft(date = next.toLocalDate(), time = next.toLocalTime(), lineId = lineId)
}

fun BoardEvent.toDraft(): EventDraft {
    val begins = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault())
    return EventDraft(
        title = title,
        location = location.orEmpty(),
        date = begins.toLocalDate(),
        time = begins.toLocalTime().truncatedTo(ChronoUnit.MINUTES),
        durationMin = if (allDay) 30 else ((end - start) / 60_000).toInt().coerceAtLeast(5),
        allDay = allDay,
        lineId = lineId,
    )
}

private val formDateFormat = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
private val formTimeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private val PRESET_DURATIONS = listOf(15, 30, 45, 60, 90, 120)

private fun durationLabel(min: Int) = if (min < 60) "$min MIN" else "%d:%02d HR".format(min / 60, min % 60)

@Composable
fun AddScreen(state: BoardUiState, viewModel: BoardViewModel, onDone: () -> Unit) {
    val initial = remember { newDraft(state) }
    EventForm(
        heading = "Add Event",
        initial = initial,
        lines = state.lines,
        saveLabel = "ADD TO BOARD",
        onSave = { viewModel.saveEvent(null, it); onDone() },
    )
}

/**
 * The form for adding or editing one of the user's own events. [onCancel]
 * and [onDelete] add a Cancel link and a Delete button when editing.
 */
@Composable
fun EventForm(
    heading: String,
    initial: EventDraft,
    lines: List<Line>,
    saveLabel: String,
    onSave: (EventDraft) -> Unit,
    onCancel: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var draft by remember(initial) { mutableStateOf(initial) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Board.Yellow,
        focusedLabelColor = Board.Yellow,
        cursorColor = Board.Yellow,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Board.Black)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(heading, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (onCancel != null) {
                Text(
                    "CANCEL",
                    color = Board.Dim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clickable(onClick = onCancel)
                        .padding(8.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = draft.title,
            onValueChange = { draft = draft.copy(title = it) },
            label = { Text("What's happening?") },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth().testTag("event-title"),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = draft.location,
            onValueChange = { draft = draft.copy(location = it) },
            label = { Text("Where (platform)") },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("WHEN")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(draft.date.format(formDateFormat), selected = true) {
                DatePickerDialog(
                    context,
                    { _, y, m, d -> draft = draft.copy(date = LocalDate.of(y, m + 1, d)) },
                    draft.date.year, draft.date.monthValue - 1, draft.date.dayOfMonth,
                ).show()
            }
            if (!draft.allDay) {
                Chip(draft.time.format(formTimeFormat), selected = true) {
                    TimePickerDialog(
                        context,
                        { _, h, min -> draft = draft.copy(time = LocalTime.of(h, min)) },
                        draft.time.hour, draft.time.minute, DateFormat.is24HourFormat(context),
                    ).show()
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("All day", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = draft.allDay,
                onCheckedChange = { draft = draft.copy(allDay = it) },
                colors = SwitchDefaults.colors(checkedTrackColor = Board.Yellow, checkedThumbColor = Board.Ink),
            )
        }

        if (!draft.allDay) {
            Spacer(Modifier.height(12.dp))
            SectionLabel("HOW LONG")
            val ends = draft.time.plusMinutes(draft.durationMin.toLong())
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (PRESET_DURATIONS + draft.durationMin).distinct().sorted().forEach { d ->
                    Chip(durationLabel(d), selected = d == draft.durationMin) {
                        draft = draft.copy(durationMin = d)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Chip("ENDS ${ends.format(formTimeFormat)}", selected = true) {
                TimePickerDialog(
                    context,
                    { _, h, min ->
                        var minutes = Duration.between(draft.time, LocalTime.of(h, min)).toMinutes()
                        if (minutes <= 0) minutes += 24 * 60 // ends after midnight
                        draft = draft.copy(durationMin = minutes.toInt())
                    },
                    ends.hour, ends.minute, DateFormat.is24HourFormat(context),
                ).show()
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("LINE")
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            lines.forEach { line ->
                Column(
                    Modifier
                        .alpha(if (line.id == draft.lineId) 1f else 0.4f)
                        .clickable { draft = draft.copy(lineId = line.id) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LineBadge(line, 36.dp)
                    Text(line.name, color = Color.White, fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        val canSave = draft.title.isNotBlank()
        Text(
            saveLabel,
            color = if (canSave) Board.Ink else Board.Dim,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (canSave) Board.Yellow else Board.PanelLine, RoundedCornerShape(4.dp))
                .clickable(enabled = canSave) { onSave(draft) }
                .padding(vertical = 16.dp)
                .testTag("event-save"),
        )
        if (onDelete != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                "DELETE EVENT",
                color = Board.Red,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Board.Red, RoundedCornerShape(4.dp))
                    .clickable(onClick = onDelete)
                    .padding(vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Board.Dim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Color.White else Board.Dim,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .border(1.5.dp, if (selected) Color.White else Board.PanelLine, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}
