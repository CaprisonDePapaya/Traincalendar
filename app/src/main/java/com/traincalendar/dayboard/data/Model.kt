package com.traincalendar.dayboard.data

import kotlin.math.ceil

enum class LineShape { CIRCLE, DIAMOND }

/** A "line" is a category of plans, drawn as a subway route badge. */
data class Line(
    val id: String,
    val name: String,
    val letter: String,
    val color: Long,
    val shape: LineShape = LineShape.CIRCLE,
    /** Device calendars whose events ride this line. */
    val calendarIds: Set<Long> = emptySet(),
)

enum class EventSource { SAMPLE, LOCAL, CALENDAR }

data class BoardEvent(
    val id: String,
    val title: String,
    val location: String?,
    val start: Long,
    val end: Long,
    val allDay: Boolean,
    val lineId: String,
    val source: EventSource,
    /** Calendar event id, for opening it in the calendar app. */
    val calendarEventId: Long? = null,
) {
    fun isLive(now: Long) = !allDay && now in start until end
    fun hasDeparted(now: Long) = !allDay && now >= end
    fun progress(now: Long): Float =
        if (end <= start) 0f else ((now - start).toFloat() / (end - start)).coerceIn(0f, 1f)
}

data class CalendarInfo(val id: Long, val name: String, val account: String, val color: Int)

/** What the TIME column shows: a big value and a small unit underneath. */
data class Countdown(val value: String, val unit: String)

fun countdown(event: BoardEvent, now: Long): Countdown {
    val live = event.isLive(now)
    val target = if (live) event.end else event.start
    val minutes = ceil((target - now) / 60_000.0).toLong().coerceAtLeast(0)
    val suffix = if (live) " LEFT" else ""
    return if (minutes < 60) {
        Countdown(minutes.toString(), "MIN$suffix")
    } else {
        Countdown("%d:%02d".format(minutes / 60, minutes % 60), "HR$suffix")
    }
}

/** Plain minutes until departure, used by the compact widget board. */
fun minutesUntil(event: BoardEvent, now: Long): Long =
    ceil((event.start - now) / 60_000.0).toLong().coerceAtLeast(0)

val LINE_PALETTE: List<Long> = listOf(
    0xFF0039A6, // blue
    0xFFFF6319, // orange
    0xFFEE352E, // red
    0xFFFCCC0A, // yellow
    0xFF00933C, // green
    0xFFB933AD, // magenta
    0xFF00ADD0, // cyan
    0xFF6CBE45, // lime
    0xFF996633, // brown
    0xFFA7A9AC, // grey
)

/** Dark text on light badges (yellow, lime, grey), white text otherwise. */
fun badgeTextIsDark(color: Long): Boolean {
    val r = (color shr 16 and 0xFF) / 255.0
    val g = (color shr 8 and 0xFF) / 255.0
    val b = (color and 0xFF) / 255.0
    return 0.299 * r + 0.587 * g + 0.114 * b > 0.6
}

val DEFAULT_LINES = listOf(
    Line("work", "Work", "A", 0xFF0039A6),
    Line("focus", "Focus", "B", 0xFFFF6319),
    Line("deadlines", "Deadlines", "C", 0xFFEE352E, LineShape.DIAMOND),
    Line("personal", "Personal", "D", 0xFFFCCC0A),
    Line("health", "Health", "E", 0xFF00933C),
    Line("social", "Social", "F", 0xFFB933AD),
    Line("travel", "Travel", "G", 0xFF00ADD0),
)
