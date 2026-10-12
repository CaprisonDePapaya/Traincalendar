package com.traincalendar.dayboard.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.ContentUris
import android.content.Intent
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traincalendar.dayboard.data.BoardEvent
import com.traincalendar.dayboard.data.Countdown
import com.traincalendar.dayboard.data.countdown
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormat = DateTimeFormatter.ofPattern("EEEE,", Locale.getDefault())
private val dateFormat = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
private val clockFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

fun formatClock(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(clockFormat)

@Composable
fun BoardScreen(
    state: BoardUiState,
    viewModel: BoardViewModel,
    onAdd: () -> Unit,
    onEdit: (BoardEvent) -> Unit,
    showUndo: (message: String, undo: () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    var lineFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var showAllDay by rememberSaveable { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var openEventId by rememberSaveable { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.reload() }
    val requestCalendar = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) }

    val allDay = state.events.filter { it.allDay }
    val timed = state.events
        .filter { !it.allDay }
        .filter { !state.isToday || !it.hasDeparted(state.now) }
        .filter { lineFilter == null || it.lineId == lineFilter }
    val linesToday = state.events.map { it.lineId }.distinct()
        .let { ids -> state.lines.filter { it.id in ids } }

    Column(Modifier.fillMaxSize()) {
        // ---- Header ------------------------------------------------------
        Column(Modifier.fillMaxWidth().background(Board.Black).padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    Modifier
                        .weight(1f)
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> viewModel.selectDate(LocalDate.of(y, m + 1, d)) },
                                state.date.year, state.date.monthValue - 1, state.date.dayOfMonth,
                            ).show()
                        }
                ) {
                    Text(state.date.format(dayFormat), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.date.format(dateFormat), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
                        Text("  ⌄", color = Color.White, fontSize = 18.sp)
                        if (!state.isToday) {
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "TODAY",
                                color = Board.Ink,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .background(Board.Yellow, RoundedCornerShape(3.dp))
                                    .clickable { viewModel.selectDate(LocalDate.now()) }
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                Box(
                    Modifier
                        .size(36.dp)
                        .background(Board.PanelLine, CircleShape)
                        .clickable { showSettings = true }
                        .testTag("settings"),
                    contentAlignment = Alignment.Center,
                ) { Text("⚙", color = Color.White, fontSize = 18.sp) }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                linesToday.forEach { line ->
                    val dim = lineFilter != null && lineFilter != line.id
                    LineBadge(
                        line,
                        28.dp,
                        Modifier
                            .alpha(if (dim) 0.3f else 1f)
                            .clickable { lineFilter = if (lineFilter == line.id) null else line.id },
                    )
                }
                if (allDay.isNotEmpty()) {
                    Text(
                        "◆  ${allDay.size} ALL-DAY",
                        color = if (showAllDay) Board.Ink else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(if (showAllDay) Board.Yellow else Board.PanelLine, RoundedCornerShape(50))
                            .clickable { showAllDay = !showAllDay }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row {
                Text("DEPARTURES", color = Board.Dim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1f).padding(start = 40.dp))
                Text("TIME", color = Board.Dim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(6.dp))
        }

        // ---- Departures --------------------------------------------------
        LazyColumn(
            Modifier
                .fillMaxSize()
                .background(Board.Cream),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (showAllDay && allDay.isNotEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().background(Board.Panel).padding(vertical = 10.dp)) {
                        Text(
                            "◆ ALL-DAY PLANS",
                            color = Board.Yellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                        allDay.forEach { ev ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { openEventId = ev.id }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                LineBadge(state.line(ev.lineId), 22.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(ev.title, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            if (state.loaded && timed.isEmpty()) {
                item {
                    EmptyBoard(
                        title = when {
                            lineFilter != null -> "Nothing on this line."
                            state.isToday && state.events.any { !it.allDay } -> "No more departures today."
                            else -> "Your board is empty."
                        },
                        showCalendarLink = !state.hasCalendarPermission,
                        showSampleLink = !state.sampleMode,
                        onAdd = onAdd,
                        onConnect = requestCalendar,
                        onSample = { viewModel.setSampleMode(true) },
                    )
                }
            }
            val firstLive = timed.firstOrNull { state.isToday && it.isLive(state.now) }?.id
            items(timed, key = { it.id }) { ev ->
                DepartureRow(
                    event = ev,
                    state = state,
                    highlighted = ev.id == firstLive,
                    onClick = { openEventId = ev.id },
                )
            }
        }
    }

    state.events.firstOrNull { it.id == openEventId }?.let { ev ->
        val calendar = state.calendars.firstOrNull { it.id == ev.calendarId }
        EventSheet(
            event = ev,
            line = state.line(ev.lineId),
            calendarName = calendar?.name,
            onDismiss = { openEventId = null },
            actions = EventActions(
                edit = { onEdit(ev) },
                delete = {
                    viewModel.deleteEvent(ev)
                    showUndo("Deleted “${ev.title}”") { viewModel.restoreEvent(ev) }
                },
                hide = {
                    viewModel.hideEvent(ev.id)
                    showUndo("Removed “${ev.title}” from the board") { viewModel.unhideEvent(ev.id) }
                },
                hideCalendar = {
                    calendar?.let { cal ->
                        viewModel.setCalendarShown(cal.id, false)
                        showUndo("Hid “${cal.name}”") { viewModel.setCalendarShown(cal.id, true) }
                    }
                },
                openInCalendar = {
                    ev.calendarEventId?.let { id ->
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id))
                            )
                        }
                    }
                },
                turnOffSample = { viewModel.setSampleMode(false) },
            ),
        )
    }

    if (showSettings) {
        SettingsDialog(
            state = state,
            viewModel = viewModel,
            onDismiss = { showSettings = false },
            onRequestCalendar = requestCalendar,
        )
    }
}

@Composable
private fun DepartureRow(event: BoardEvent, state: BoardUiState, highlighted: Boolean, onClick: () -> Unit) {
    val line = state.line(event.lineId)
    val live = state.isToday && event.isLive(state.now)
    val bg = when {
        highlighted -> Board.Yellow
        live -> Board.LiveCream
        else -> Board.Cream
    }
    val time = if (state.isToday) {
        countdown(event, state.now)
    } else {
        val parts = formatClock(event.start).split(Regex("[\\s\\u00A0\\u202F]+"))
        Countdown(parts[0], parts.getOrElse(1) { "" })
    }

    Column(Modifier.background(bg).clickable(onClick = onClick)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (highlighted) OutlinedLineBadge(line, 34.dp) else LineBadge(line, 34.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (live) {
                    Text(
                        "LIVE",
                        color = Board.Yellow,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .background(Board.Ink, RoundedCornerShape(2.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                    Spacer(Modifier.height(3.dp))
                }
                Text(
                    event.title,
                    color = Board.Ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                )
                event.location?.let {
                    Text(it, color = if (highlighted) Board.Ink else Board.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(formatClock(event.start), color = if (highlighted) Board.Ink else Board.Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.widthIn(min = 56.dp), horizontalAlignment = Alignment.End) {
                Text(time.value, color = Board.Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
                Text(time.unit, color = Board.Ink, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
        if (live) {
            Box(Modifier.fillMaxWidth().height(3.dp)) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(event.progress(state.now))
                        .background(Board.Ink)
                )
            }
        } else {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Board.Divider))
        }
    }
}

@Composable
private fun EmptyBoard(
    title: String,
    showCalendarLink: Boolean,
    showSampleLink: Boolean,
    onAdd: () -> Unit,
    onConnect: () -> Unit,
    onSample: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = Board.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            if (showCalendarLink) {
                "Add your own plans, or bring in the events from your phone's calendar."
            } else {
                "Add a plan and it will show up here."
            },
            color = Board.Muted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "ADD EVENT",
            color = Board.Yellow,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(Board.Ink, RoundedCornerShape(4.dp))
                .clickable(onClick = onAdd)
                .padding(vertical = 15.dp),
        )
        if (showCalendarLink) {
            Spacer(Modifier.height(10.dp))
            Text(
                "SHOW PHONE CALENDAR",
                color = Board.Ink,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Board.Ink, RoundedCornerShape(4.dp))
                    .clickable(onClick = onConnect)
                    .padding(vertical = 15.dp),
            )
        }
        if (showSampleLink) {
            Text(
                "Try the sample day",
                color = Board.Muted,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable(onClick = onSample)
                    .padding(top = 16.dp, bottom = 4.dp, start = 8.dp, end = 8.dp),
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    state: BoardUiState,
    viewModel: BoardViewModel,
    onDismiss: () -> Unit,
    onRequestCalendar: () -> Unit,
) {
    val switchColors = SwitchDefaults.colors(checkedTrackColor = Board.Yellow, checkedThumbColor = Board.Ink)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Board.Panel,
        title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("PHONE CALENDAR", color = Board.Dim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                if (!state.hasCalendarPermission) {
                    Text(
                        "Your board only shows the plans you add. You can also show the events from your phone's calendar.",
                        color = Board.Dim,
                        fontSize = 12.sp,
                    )
                    TextButton(onClick = onRequestCalendar) { Text("Show phone calendar", color = Board.Yellow) }
                } else if (state.calendars.isEmpty()) {
                    Text("No calendars found on this phone.", color = Board.Dim, fontSize = 12.sp)
                } else {
                    Text("Choose which calendars appear on the board.", color = Board.Dim, fontSize = 12.sp)
                    state.calendars.forEach { cal ->
                        Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(Color(cal.color), CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(cal.name, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (cal.account.isNotBlank() && cal.account != cal.name) {
                                    Text(cal.account, color = Board.Dim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Switch(
                                checked = cal.shown,
                                onCheckedChange = { viewModel.setCalendarShown(cal.id, it) },
                                colors = switchColors,
                            )
                        }
                    }
                }
                if (state.hiddenEventCount > 0) {
                    TextButton(onClick = viewModel::unhideAllEvents) {
                        Text(
                            "Bring back ${state.hiddenEventCount} removed calendar event${if (state.hiddenEventCount == 1) "" else "s"}",
                            color = Board.Yellow,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Sample day", color = Color.White, fontSize = 16.sp)
                        Text("Show the demo board from the video.", color = Board.Dim, fontSize = 12.sp)
                    }
                    Switch(checked = state.sampleMode, onCheckedChange = viewModel::setSampleMode, colors = switchColors)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = Board.Yellow) } },
    )
}
