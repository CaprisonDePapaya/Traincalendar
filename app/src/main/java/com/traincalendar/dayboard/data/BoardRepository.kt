package com.traincalendar.dayboard.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

/**
 * Single source of truth for lines, settings and the events on a given day.
 * Lines, settings and events added in-app live in SharedPreferences; the
 * rest comes from the device calendar (or the built-in sample day).
 */
class BoardRepository(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("day_board", Context.MODE_PRIVATE)

    var sampleMode: Boolean
        get() = prefs.getBoolean(KEY_SAMPLE, true)
        set(value) = prefs.edit().putBoolean(KEY_SAMPLE, value).apply()

    fun hasCalendarPermission(): Boolean =
        ContextCompat.checkSelfPermission(app, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    // ---- Lines ----------------------------------------------------------

    fun lines(): List<Line> {
        val raw = prefs.getString(KEY_LINES, null) ?: return DEFAULT_LINES
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val cals = o.optJSONArray("calendars") ?: JSONArray()
                Line(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    letter = o.getString("letter"),
                    color = o.getLong("color"),
                    shape = LineShape.valueOf(o.optString("shape", LineShape.CIRCLE.name)),
                    calendarIds = (0 until cals.length()).map { cals.getLong(it) }.toSet(),
                )
            }
        }.getOrDefault(DEFAULT_LINES)
    }

    fun saveLines(lines: List<Line>) {
        val arr = JSONArray()
        lines.forEach { l ->
            arr.put(
                JSONObject()
                    .put("id", l.id)
                    .put("name", l.name)
                    .put("letter", l.letter)
                    .put("color", l.color)
                    .put("shape", l.shape.name)
                    .put("calendars", JSONArray(l.calendarIds.toList()))
            )
        }
        prefs.edit().putString(KEY_LINES, arr.toString()).apply()
    }

    // ---- Events added in the app ----------------------------------------

    private fun localEvents(): List<BoardEvent> {
        val raw = prefs.getString(KEY_LOCAL, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                BoardEvent(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    location = o.optString("location").ifBlank { null },
                    start = o.getLong("start"),
                    end = o.getLong("end"),
                    allDay = o.optBoolean("allDay"),
                    lineId = o.getString("line"),
                    source = EventSource.LOCAL,
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun saveLocalEvents(events: List<BoardEvent>) {
        val arr = JSONArray()
        events.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("title", e.title)
                    .put("location", e.location ?: "")
                    .put("start", e.start)
                    .put("end", e.end)
                    .put("allDay", e.allDay)
                    .put("line", e.lineId)
            )
        }
        prefs.edit().putString(KEY_LOCAL, arr.toString()).apply()
    }

    fun addLocalEvent(
        title: String,
        location: String?,
        start: Long,
        end: Long,
        allDay: Boolean,
        lineId: String,
    ) {
        val event = BoardEvent(
            id = "local-" + UUID.randomUUID(),
            title = title,
            location = location,
            start = start,
            end = end,
            allDay = allDay,
            lineId = lineId,
            source = EventSource.LOCAL,
        )
        saveLocalEvents(localEvents() + event)
    }

    fun deleteLocalEvent(id: String) = saveLocalEvents(localEvents().filterNot { it.id == id })

    // ---- Device calendar ------------------------------------------------

    fun calendars(): List<CalendarInfo> {
        if (!hasCalendarPermission()) return emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
        )
        val result = mutableListOf<CalendarInfo>()
        runCatching {
            app.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI, projection,
                "${CalendarContract.Calendars.VISIBLE} = 1", null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    result += CalendarInfo(
                        id = c.getLong(0),
                        name = c.getString(1) ?: "Calendar",
                        account = c.getString(2) ?: "",
                        color = c.getInt(3),
                    )
                }
            }
        }
        return result
    }

    private fun calendarEvents(date: LocalDate, lines: List<Line>): List<BoardEvent> {
        if (!hasCalendarPermission()) return emptyList()
        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // All-day instances are stored at UTC midnight, so widen the query
        // window by a day on each side and filter precisely below.
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, dayStart - DAY_MS)
        ContentUris.appendId(builder, dayEnd + DAY_MS)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
        )
        val result = mutableListOf<BoardEvent>()
        runCatching {
            app.contentResolver.query(
                builder.build(), projection, null, null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val begin = c.getLong(3)
                    val end = c.getLong(4)
                    val allDay = c.getInt(5) == 1
                    val onDay = if (allDay) {
                        val first = Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).toLocalDate()
                        val last = Instant.ofEpochMilli(end - 1).atZone(ZoneOffset.UTC).toLocalDate()
                        !date.isBefore(first) && !date.isAfter(last)
                    } else {
                        begin < dayEnd && end > dayStart
                    }
                    if (!onDay) continue
                    val eventId = c.getLong(0)
                    result += BoardEvent(
                        id = "cal-$eventId-$begin",
                        title = c.getString(1)?.ifBlank { null } ?: "(No title)",
                        location = c.getString(2)?.ifBlank { null },
                        start = begin,
                        end = end,
                        allDay = allDay,
                        lineId = lineForCalendar(c.getLong(6), c.getString(7), lines),
                        source = EventSource.CALENDAR,
                        calendarEventId = eventId,
                    )
                }
            }
        }
        return result
    }

    private fun lineForCalendar(calendarId: Long, calendarName: String?, lines: List<Line>): String =
        (lines.firstOrNull { calendarId in it.calendarIds }
            ?: lines.firstOrNull { it.name.equals(calendarName, ignoreCase = true) }
            ?: lines.first()).id

    // ---- The board ------------------------------------------------------

    /** Every event for [date], sorted by start time. */
    fun eventsFor(date: LocalDate, now: Long = System.currentTimeMillis()): List<BoardEvent> {
        val lines = lines()
        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val base = if (sampleMode) sampleDay(date, now) else calendarEvents(date, lines)
        val local = localEvents().filter { it.start < dayEnd && it.end > dayStart }
        val known = lines.map { it.id }.toSet()
        return (base + local)
            .map { if (it.lineId in known) it else it.copy(lineId = lines.first().id) }
            .sortedWith(compareBy({ it.start }, { it.title }))
    }

    /**
     * A demo day anchored to the current time of day, so there is always
     * something live and a few trains on the way.
     */
    private fun sampleDay(date: LocalDate, now: Long): List<BoardEvent> {
        val zone = ZoneId.systemDefault()
        val nowTime = Instant.ofEpochMilli(now).atZone(zone).toLocalTime()
        val anchor = date.atTime(nowTime).atZone(zone).toInstant().toEpochMilli()
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        fun ev(id: String, title: String, loc: String, startMin: Int, durMin: Int, line: String) =
            BoardEvent(
                id = "sample-$id", title = title, location = loc,
                start = anchor + startMin * 60_000L,
                end = anchor + (startMin + durMin) * 60_000L,
                allDay = false, lineId = line, source = EventSource.SAMPLE,
            )
        fun allDay(id: String, title: String, line: String) = BoardEvent(
            id = "sample-$id", title = title, location = null,
            start = dayStart, end = dayStart + DAY_MS, allDay = true,
            lineId = line, source = EventSource.SAMPLE,
        )
        val endOfDay = date.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
        return listOf(
            ev("coffee", "Coffee with Team", "Kitchen 2F", -20, 30, "personal"),
            ev("launch", "Cross-Functional Launch Readiness and Final Production Review", "Studio 4A · Video Call", -14, 37, "work"),
            ev("standup", "Morning Standup", "Conf Room B", 18, 15, "work"),
            ev("deep", "Deep Work", "Home office", 52, 90, "focus"),
            ev("lunch", "Lunch with Mara", "The Canteen 3F", 118, 60, "social"),
            ev("cut", "Send Final Cut", "Edit Bay 2", 195, 30, "deadlines"),
            ev("dentist", "Dentist Appointment", "Dr Kim · 310 5th Ave", 288, 45, "health"),
            ev("train", "Train Home", "Platform 6", 450, 40, "travel"),
            allDay("hold1", "Hold: posting buffer — no organic posts", "deadlines"),
            allDay("hold2", "Hold: posting buffer — no other sponsors", "deadlines"),
        ).filter { it.allDay || it.start < endOfDay }
    }

    companion object {
        private const val KEY_SAMPLE = "sample_mode"
        private const val KEY_LINES = "lines"
        private const val KEY_LOCAL = "local_events"
        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
