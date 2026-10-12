package com.traincalendar.dayboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traincalendar.dayboard.data.BoardEvent
import com.traincalendar.dayboard.data.EventSource
import com.traincalendar.dayboard.data.Line
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val sheetDateFormat = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

/** What the user can do with an event they tapped on the board. */
class EventActions(
    val edit: () -> Unit,
    val delete: () -> Unit,
    val hide: () -> Unit,
    val hideCalendar: () -> Unit,
    val openInCalendar: () -> Unit,
    val turnOffSample: () -> Unit,
)

@Composable
fun EventSheet(
    event: BoardEvent,
    line: Line,
    calendarName: String?,
    actions: EventActions,
    onDismiss: () -> Unit,
) {
    // Every action also closes the sheet.
    fun run(action: () -> Unit): () -> Unit = { onDismiss(); action() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Board.Panel,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LineBadge(line, 30.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    event.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        text = {
            Column {
                Text(whenText(event), color = Color.White, fontSize = 15.sp)
                event.location?.let { Text(it, color = Board.Dim, fontSize = 14.sp) }
                Text("${line.name} line", color = Board.Dim, fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                when (event.source) {
                    EventSource.LOCAL -> {
                        SheetButton("EDIT", primary = true, onClick = run(actions.edit))
                        SheetButton("DELETE", danger = true, onClick = run(actions.delete))
                    }
                    EventSource.CALENDAR -> {
                        Note("From your phone calendar${calendarName?.let { " · $it" } ?: ""}.")
                        SheetButton("REMOVE FROM BOARD", primary = true, onClick = run(actions.hide))
                        Note("It stays in your calendar. To change or delete it there, open it in your calendar app.")
                        SheetButton("OPEN IN CALENDAR", onClick = run(actions.openInCalendar))
                        if (calendarName != null) {
                            SheetButton("HIDE ALL OF “$calendarName”", onClick = run(actions.hideCalendar))
                        }
                    }
                    EventSource.SAMPLE -> {
                        Note("This is a demo event from the sample day.")
                        SheetButton("TURN OFF SAMPLE DAY", primary = true, onClick = run(actions.turnOffSample))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = Board.Yellow) } },
    )
}

private fun whenText(event: BoardEvent): String {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(event.start).atZone(zone)
    val day = start.format(sheetDateFormat)
    return if (event.allDay) "$day · All day" else "$day · ${formatClock(event.start)} – ${formatClock(event.end)}"
}

@Composable
private fun Note(text: String) {
    Text(text, color = Board.Dim, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun SheetButton(label: String, primary: Boolean = false, danger: Boolean = false, onClick: () -> Unit) {
    val (bg, fg) = when {
        primary -> Board.Yellow to Board.Ink
        else -> Color.Transparent to (if (danger) Board.Red else Color.White)
    }
    Text(
        label,
        color = fg,
        fontWeight = FontWeight.Black,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(4.dp))
            .border(1.5.dp, if (primary) bg else fg, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp, horizontal = 8.dp),
    )
}
