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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val addDateFormat = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
private val addTimeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private val DURATIONS = listOf(15, 30, 45, 60, 90, 120)

@Composable
fun AddScreen(state: BoardUiState, viewModel: BoardViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var date by remember { mutableStateOf(state.date) }
    var time by remember {
        // Default to the next half hour.
        val now = LocalTime.now()
        mutableStateOf(now.withSecond(0).withNano(0).withMinute(if (now.minute < 30) 30 else 0).let {
            if (now.minute >= 30) it.plusHours(1) else it
        })
    }
    var duration by rememberSaveable { mutableStateOf(30) }
    var allDay by rememberSaveable { mutableStateOf(false) }
    var lineId by rememberSaveable { mutableStateOf(state.lines.firstOrNull()?.id ?: "") }

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
        Text("Add Event", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("What's happening?") },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Where (platform)") },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("WHEN")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(date.format(addDateFormat), selected = true) {
                DatePickerDialog(
                    context,
                    { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                    date.year, date.monthValue - 1, date.dayOfMonth,
                ).show()
            }
            if (!allDay) {
                Chip(time.format(addTimeFormat), selected = true) {
                    TimePickerDialog(
                        context,
                        { _, h, min -> time = LocalTime.of(h, min) },
                        time.hour, time.minute, DateFormat.is24HourFormat(context),
                    ).show()
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("All day", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = allDay,
                onCheckedChange = { allDay = it },
                colors = SwitchDefaults.colors(checkedTrackColor = Board.Yellow, checkedThumbColor = Board.Ink),
            )
        }

        if (!allDay) {
            Spacer(Modifier.height(12.dp))
            SectionLabel("DURATION")
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DURATIONS.forEach { d ->
                    Chip(if (d < 60) "$d MIN" else "%d:%02d HR".format(d / 60, d % 60), selected = d == duration) {
                        duration = d
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("LINE")
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.lines.forEach { line ->
                Column(
                    Modifier
                        .alpha(if (line.id == lineId) 1f else 0.4f)
                        .clickable { lineId = line.id },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LineBadge(line, 36.dp)
                    Text(line.name, color = Color.White, fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        val canSave = title.isNotBlank() && lineId.isNotBlank()
        Text(
            "ADD TO BOARD",
            color = if (canSave) Board.Ink else Board.Dim,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (canSave) Board.Yellow else Board.PanelLine, RoundedCornerShape(4.dp))
                .clickable(enabled = canSave) {
                    val zone = ZoneId.systemDefault()
                    val (start, end) = if (allDay) {
                        date.atStartOfDay(zone).toInstant().toEpochMilli() to
                            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                    } else {
                        val s = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
                        s to s + duration * 60_000L
                    }
                    viewModel.addEvent(title.trim(), location.trim().ifBlank { null }, start, end, allDay, lineId)
                    if (date != state.date) viewModel.selectDate(date)
                    title = ""
                    location = ""
                    onDone()
                }
                .padding(vertical = 16.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Plans added here live on your board and widget. Tap one on the board to remove it.",
            color = Board.Dim,
            fontSize = 12.sp,
        )
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
