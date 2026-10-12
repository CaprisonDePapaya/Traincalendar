package com.traincalendar.dayboard.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.traincalendar.dayboard.MainActivity
import com.traincalendar.dayboard.data.BoardEvent
import com.traincalendar.dayboard.data.BoardRepository
import com.traincalendar.dayboard.data.Line
import com.traincalendar.dayboard.data.badgeTextIsDark
import com.traincalendar.dayboard.data.minutesUntil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Amber = Color(0xFFFFC23D)
private val Panel = Color(0xFF0B0B0B)
private val Dim = Color(0xFF8A8A8A)

private class WidgetData(
    val date: LocalDate,
    val now: Long,
    val timed: List<BoardEvent>,
    val allDay: List<BoardEvent>,
    val lines: Map<String, Line>,
    val sample: Boolean,
)

/**
 * The home-screen board: a dot-matrix style arrivals display in amber
 * monospace, like the signs on a subway platform.
 */
class DayBoardWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = withContext(Dispatchers.IO) {
            val repo = BoardRepository(context)
            val now = System.currentTimeMillis()
            val today = LocalDate.now()
            val events = repo.eventsFor(today, now)
            WidgetData(
                date = today,
                now = now,
                timed = events.filter { !it.allDay && !it.hasDeparted(now) },
                allDay = events.filter { it.allDay },
                lines = repo.lines().associateBy { it.id },
                sample = repo.sampleMode,
            )
        }
        provideContent { WidgetBoard(data) }
    }

    companion object {
        suspend fun refresh(context: Context) {
            runCatching { DayBoardWidget().updateAll(context) }
        }
    }
}

class DayBoardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DayBoardWidget()
}

private val headerFormat = DateTimeFormatter.ofPattern("EEE MMM d", Locale.getDefault())

private fun mono(color: Color, size: Int, bold: Boolean = true) = TextStyle(
    color = ColorProvider(color),
    fontSize = size.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    fontFamily = FontFamily.Monospace,
)

@Composable
private fun WidgetBoard(data: WidgetData) {
    val size: DpSize = LocalSize.current
    val rowHeight = 17.dp
    val wide = size.width >= 200.dp

    // Fit as many rows as the widget is tall: header ~34dp, footer ~20dp.
    val showAllDay = data.allDay.isNotEmpty() && size.height >= 200.dp
    val allDayRows = if (showAllDay) minOf(data.allDay.size, 2) else 0
    val reserved = 34.dp + 22.dp + if (showAllDay) (20.dp + rowHeight * allDayRows) else 0.dp
    val maxRows = ((size.height - reserved).value / rowHeight.value).toInt().coerceIn(1, 10)

    Column(
        GlanceModifier
            .fillMaxSize()
            .background(Panel)
            .cornerRadius(18.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(data.date.format(headerFormat).uppercase(), style = mono(Color.White, 14))
        Spacer(GlanceModifier.height(4.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            Text("LN  EVENT", style = mono(Dim, 8), modifier = GlanceModifier.defaultWeight())
            Text("MIN", style = mono(Dim, 8))
        }
        Spacer(GlanceModifier.height(2.dp))

        // Glance containers hold at most 10 children, so rows get their own column.
        Column(GlanceModifier.fillMaxWidth()) {
            if (data.timed.isEmpty()) {
                Text("NO MORE DEPARTURES TODAY", style = mono(Amber, 10), modifier = GlanceModifier.padding(vertical = 6.dp))
            }
            data.timed.take(maxRows).forEach { ev ->
                val line = data.lines[ev.lineId] ?: data.lines.values.first()
                val time = if (ev.isLive(data.now)) "LIVE" else minutesUntil(ev, data.now).toString()
                BoardLine(line, ev.title, time, rowHeight)
            }
        }

        if (showAllDay) {
            Column(GlanceModifier.fillMaxWidth().padding(top = 6.dp)) {
                Row(GlanceModifier.fillMaxWidth()) {
                    Text("LN  ALL DAY", style = mono(Dim, 8), modifier = GlanceModifier.defaultWeight())
                    val extra = data.allDay.size - allDayRows
                    if (extra > 0) Text("+$extra", style = mono(Dim, 8))
                }
                data.allDay.take(allDayRows).forEach { ev ->
                    val line = data.lines[ev.lineId] ?: data.lines.values.first()
                    BoardLine(line, ev.title, "", rowHeight)
                }
            }
        }

        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "DAY BOARD ◆ ${data.timed.size}",
                style = mono(Color.White, 8),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (data.sample && wide) Text("Sample day", style = mono(Dim, 8, bold = false))
        }
    }
}

@Composable
private fun BoardLine(line: Line, title: String, time: String, height: androidx.compose.ui.unit.Dp) {
    Row(
        GlanceModifier.fillMaxWidth().height(height),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            GlanceModifier
                .size(12.dp)
                .background(Color(line.color))
                .cornerRadius(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                line.letter.take(1),
                style = mono(if (badgeTextIsDark(line.color)) Color.Black else Color.White, 7),
            )
        }
        Spacer(GlanceModifier.width(6.dp))
        Text(
            title.uppercase(),
            style = mono(Amber, 10),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        if (time.isNotEmpty()) {
            Spacer(GlanceModifier.width(6.dp))
            Text(time, style = mono(Amber, 10))
        }
    }
}
