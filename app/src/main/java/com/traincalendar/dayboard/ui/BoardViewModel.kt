package com.traincalendar.dayboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.traincalendar.dayboard.data.BoardEvent
import com.traincalendar.dayboard.data.BoardRepository
import com.traincalendar.dayboard.data.CalendarInfo
import com.traincalendar.dayboard.data.DEFAULT_LINES
import com.traincalendar.dayboard.data.EventSource
import com.traincalendar.dayboard.data.Line
import com.traincalendar.dayboard.widget.DayBoardWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class BoardUiState(
    val date: LocalDate = LocalDate.now(),
    val now: Long = System.currentTimeMillis(),
    val lines: List<Line> = emptyList(),
    val events: List<BoardEvent> = emptyList(),
    val sampleMode: Boolean = true,
    val hasCalendarPermission: Boolean = false,
    val calendars: List<CalendarInfo> = emptyList(),
    val hiddenEventCount: Int = 0,
    val loaded: Boolean = false,
) {
    val isToday get() = date == LocalDate.now()
    fun line(id: String): Line =
        lines.firstOrNull { it.id == id } ?: lines.firstOrNull() ?: DEFAULT_LINES.first()
}

class BoardViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BoardRepository(app)
    private val _state = MutableStateFlow(BoardUiState())
    val state: StateFlow<BoardUiState> = _state
    private var loadSeq = 0

    init {
        reload()
        // The board clock: countdowns tick every 15 seconds, and the day's
        // events are re-read every few minutes to pick up calendar changes.
        viewModelScope.launch {
            var ticks = 0
            while (true) {
                delay(15_000)
                _state.update { it.copy(now = System.currentTimeMillis()) }
                if (++ticks % 20 == 0) reload()
            }
        }
    }

    fun reload() {
        val seq = ++loadSeq
        viewModelScope.launch {
            val date = _state.value.date
            val loaded = withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                BoardUiState(
                    date = date,
                    now = now,
                    lines = repo.lines(),
                    events = repo.eventsFor(date, now),
                    sampleMode = repo.sampleMode,
                    hasCalendarPermission = repo.hasCalendarPermission(),
                    calendars = repo.calendars(),
                    hiddenEventCount = repo.hiddenEvents.size,
                    loaded = true,
                )
            }
            // Only the newest load wins, so a slow, stale one can't undo an edit.
            if (seq == loadSeq) _state.value = loaded
        }
    }

    private fun changed() {
        reload()
        viewModelScope.launch { DayBoardWidget.refresh(getApplication<Application>()) }
    }

    fun selectDate(date: LocalDate) {
        _state.update { it.copy(date = date, events = emptyList(), loaded = false) }
        reload()
    }

    fun setSampleMode(enabled: Boolean) {
        repo.sampleMode = enabled
        changed()
    }

    fun saveLine(line: Line) {
        val lines = repo.lines()
        val updated = if (lines.any { it.id == line.id }) {
            lines.map { if (it.id == line.id) line else it }
        } else {
            lines + line
        }
        repo.saveLines(updated)
        changed()
    }

    fun deleteLine(id: String) {
        val remaining = repo.lines().filterNot { it.id == id }
        if (remaining.isEmpty()) return
        repo.saveLines(remaining)
        changed()
    }

    /** Adds a new event ([id] null) or updates one of the user's own events. */
    fun saveEvent(id: String?, draft: EventDraft) {
        val (start, end) = draft.toMillis()
        repo.saveLocalEvent(
            BoardEvent(
                id = id ?: repo.newLocalId(),
                title = draft.title.trim(),
                location = draft.location.trim().ifBlank { null },
                start = start,
                end = end,
                allDay = draft.allDay,
                lineId = draft.lineId,
                source = EventSource.LOCAL,
            )
        )
        // Follow the event to its day so it's visible right after saving.
        if (draft.date != _state.value.date) {
            _state.update { it.copy(date = draft.date, events = emptyList(), loaded = false) }
        }
        changed()
    }

    fun deleteEvent(event: BoardEvent) {
        repo.deleteLocalEvent(event.id)
        changed()
    }

    /** Puts a deleted event back (the snackbar's Undo). */
    fun restoreEvent(event: BoardEvent) {
        repo.saveLocalEvent(event)
        changed()
    }

    /** Takes a phone-calendar event off the board; the calendar keeps it. */
    fun hideEvent(id: String) {
        repo.hiddenEvents = repo.hiddenEvents + id
        changed()
    }

    fun unhideEvent(id: String) {
        repo.hiddenEvents = repo.hiddenEvents - id
        changed()
    }

    fun unhideAllEvents() {
        repo.hiddenEvents = emptySet()
        changed()
    }

    fun setCalendarShown(calendarId: Long, shown: Boolean) {
        repo.hiddenCalendars = if (shown) repo.hiddenCalendars - calendarId else repo.hiddenCalendars + calendarId
        changed()
    }
}
