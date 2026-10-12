package com.traincalendar.dayboard.ui

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DayBoardRoot(viewModel: BoardViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.BOARD) }

    DayBoardTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(Board.Black)
                .statusBarsPadding()
        ) {
            if (state.sampleMode) {
                SampleDayBar(onExit = { viewModel.setSampleMode(false) })
            }
            Box(Modifier.weight(1f)) {
                when (tab) {
                    Tab.BOARD -> BoardScreen(state, viewModel)
                    Tab.LINES -> LinesScreen(state, viewModel)
                    Tab.ADD -> AddScreen(state, viewModel, onDone = { tab = Tab.BOARD })
                }
            }
            BottomBar(tab) { tab = it }
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
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                "✕",
                color = androidx.compose.ui.graphics.Color.White,
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
                .background(androidx.compose.ui.graphics.Color.White)
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
