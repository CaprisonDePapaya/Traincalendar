package com.traincalendar.dayboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traincalendar.dayboard.data.LINE_PALETTE
import com.traincalendar.dayboard.data.Line
import com.traincalendar.dayboard.data.LineShape
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val subFormat = DateTimeFormatter.ofPattern("EEE MMM d", Locale.getDefault())

@Composable
fun LinesScreen(state: BoardUiState, viewModel: BoardViewModel) {
    var editing by remember { mutableStateOf<Line?>(null) }
    var isNew by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(Board.Black),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Text("Lines", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                state.date.format(subFormat).uppercase(),
                color = Board.Dim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(12.dp))
        }
        items(state.lines, key = { it.id }) { line ->
            val count = state.events.count { it.lineId == line.id }
            Column(Modifier.clickable { editing = line; isNew = false }) {
                Row(
                    Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LineBadge(line, 30.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(line.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("$count today", color = Board.Dim, fontSize = 12.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("›", color = Board.Dim, fontSize = 20.sp)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Board.PanelLine))
            }
        }
        item {
            Spacer(Modifier.height(20.dp))
            Text(
                "ADD LINE",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color.White, RoundedCornerShape(4.dp))
                    .clickable {
                        val used = state.lines.map { it.letter }.toSet()
                        val letter = ('A'..'Z').map { it.toString() }.firstOrNull { it !in used } ?: "Z"
                        editing = Line(
                            id = UUID.randomUUID().toString(),
                            name = "",
                            letter = letter,
                            color = LINE_PALETTE[state.lines.size % LINE_PALETTE.size],
                        )
                        isNew = true
                    }
                    .padding(vertical = 14.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (state.hasCalendarPermission) {
                    "Tip: open a line to choose which of your calendars ride on it."
                } else {
                    "Events from a calendar named like a line (e.g. \"Work\") ride that line automatically."
                },
                color = Board.Dim,
                fontSize = 12.sp,
            )
        }
    }

    editing?.let { line ->
        EditLineDialog(
            initial = line,
            isNew = isNew,
            state = state,
            onDismiss = { editing = null },
            onSave = { viewModel.saveLine(it); editing = null },
            onDelete = if (!isNew && state.lines.size > 1) {
                { viewModel.deleteLine(line.id); editing = null }
            } else null,
        )
    }
}

@Composable
private fun EditLineDialog(
    initial: Line,
    isNew: Boolean,
    state: BoardUiState,
    onDismiss: () -> Unit,
    onSave: (Line) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var line by remember { mutableStateOf(initial) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Board.Yellow,
        focusedLabelColor = Board.Yellow,
        cursorColor = Board.Yellow,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Board.Panel,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LineBadge(line, 30.dp)
                Spacer(Modifier.width(12.dp))
                Text(if (isNew) "New Line" else "Edit Line", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = line.name,
                    onValueChange = { line = line.copy(name = it) },
                    label = { Text("Name") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = line.letter,
                    onValueChange = { line = line.copy(letter = it.take(2).uppercase()) },
                    label = { Text("Letter") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Text("COLOR", color = Board.Dim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                LINE_PALETTE.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                        row.forEach { c ->
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .background(Color(c), CircleShape)
                                    .border(
                                        if (line.color == c) 3.dp else 0.dp,
                                        if (line.color == c) Color.White else Color.Transparent,
                                        CircleShape,
                                    )
                                    .clickable { line = line.copy(color = c) }
                            )
                        }
                    }
                }
                Text("SHAPE", color = Board.Dim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LineShape.entries.forEach { shape ->
                        Box(
                            Modifier
                                .border(
                                    2.dp,
                                    if (line.shape == shape) Board.Yellow else Color.Transparent,
                                    RoundedCornerShape(6.dp),
                                )
                                .clickable { line = line.copy(shape = shape) }
                                .padding(6.dp)
                        ) { LineBadge(line.copy(shape = shape), 30.dp) }
                    }
                }
                if (state.calendars.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text("CALENDARS ON THIS LINE", color = Board.Dim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    state.calendars.forEach { cal ->
                        val checked = cal.id in line.calendarIds
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    line = line.copy(
                                        calendarIds = if (checked) line.calendarIds - cal.id else line.calendarIds + cal.id
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = Board.Yellow, checkmarkColor = Board.Ink),
                                modifier = Modifier.padding(12.dp),
                            )
                            Column {
                                Text(cal.name, color = Color.White, fontSize = 14.sp)
                                if (cal.account.isNotBlank()) Text(cal.account, color = Board.Dim, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = line.name.isNotBlank() && line.letter.isNotBlank(),
                onClick = { onSave(line.copy(name = line.name.trim())) },
            ) { Text("Save", color = Board.Yellow) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = Color(0xFFEE352E)) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
            }
        },
    )
}
