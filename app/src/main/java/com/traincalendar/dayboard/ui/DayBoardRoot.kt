package com.traincalendar.dayboard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@Composable
fun DayBoardRoot(viewModel: BoardViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.BOARD) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val showUndo: (String, () -> Unit) -> Unit = { message, undo ->
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(message, actionLabel = "UNDO", duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) undo()
        }
    }
    val editing = state.events.firstOrNull { it.id == editingId }
    BackHandler(enabled = editingId != null) { editingId = null }
    BackHandler(enabled = editingId == null && tab != Tab.BOARD) { tab = Tab.BOARD }

    DayBoardTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(Board.Black)
                .statusBarsPadding()
        ) {
            if (state.sampleMode && editing == null) {
                SampleDayBar(onExit = { viewModel.setSampleMode(false) })
            }
            Box(Modifier.weight(1f)) {
                if (editing != null) {
                    EventForm(
                        heading = "Edit Event",
                        initial = editing.toDraft(),
                        lines = state.lines,
                        saveLabel = "SAVE CHANGES",
                        onSave = { draft ->
                            viewModel.saveEvent(editing.id, draft)
                            editingId = null
                        },
                        onCancel = { editingId = null },
                        onDelete = {
                            viewModel.deleteEvent(editing)
                            editingId = null
                            showUndo("Deleted “${editing.title}”") { viewModel.restoreEvent(editing) }
                        },
                    )
                } else {
                    when (tab) {
                        Tab.BOARD -> BoardScreen(
                            state = state,
                            viewModel = viewModel,
                            onAdd = { tab = Tab.ADD },
                            onEdit = { editingId = it.id },
                            showUndo = showUndo,
                        )
                        Tab.LINES -> LinesScreen(state, viewModel)
                        Tab.ADD -> AddScreen(state, viewModel, onDone = { tab = Tab.BOARD })
                    }
                }
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter)) { data ->
                    Snackbar(
                        data,
                        containerColor = Board.Yellow,
                        contentColor = Board.Ink,
                        actionColor = Board.Ink,
                    )
                }
            }
            if (editing == null) {
                BottomBar(tab) { tab = it }
            } else {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun SampleDayBar(onExit: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "SAMPLE DAY",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                "✕",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable(onClick = onExit)
                    .padding(horizontal = 4.dp),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(2.dp)
                .background(Color.White)
        )
    }
}

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Board.Black)
            .navigationBarsPadding()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Tab.entries.forEach { t ->
            val color = if (t == selected) Board.Yellow else Board.Dim
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onSelect(t) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .width(14.dp)
                        .height(2.dp)
                        .background(if (t == selected) Board.Yellow else Board.Black)
                )
                Spacer(Modifier.height(4.dp))
                TabIcon(t, color)
                Spacer(Modifier.height(4.dp))
                Text(t.label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}
