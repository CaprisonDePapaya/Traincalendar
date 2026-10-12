package com.traincalendar.dayboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.traincalendar.dayboard.data.BoardEvent
import com.traincalendar.dayboard.data.BoardRepository
import com.traincalendar.dayboard.data.CalendarInfo
import com.traincalendar.dayboard.data.DEFAULT_LINES
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
) {
    val isToday get() = date == LocalDate.now()
    fun line(id: String): Line =
        lines.firstOrNull { it.id == id } ?: lines.firstOrNull() ?: DEFAULT_LINES.first()
}

class BoardViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BoardRepository(app)
    private val _state = MutableStateFlow(BoardUiState())
    val state: StateFlow<BoardUiState> = _state

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
                )
            }
            // Ignore a stale load if the user switched days in the meantime.
            if (_state.value.date == date) _state.value = loaded
        }
    }

    private fun changed() {
        reload()
        viewModelScope.launch { DayBoardWidget.refresh(getApplication()) }
    }

    fun selectDate(date: LocalDate) {
        _state.update { it.copy(date = date, events = emptyList()) }
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

    fun addEvent(title: String, location: String?, start: Long, end: Long, allDay: Boolean, lineId: String) {
        repo.addLocalEvent(title, location, start, end, allDay, lineId)
        changed()
    }

    fun deleteEvent(id: String) {
        repo.deleteLocalEvent(id)
        changed()
    }
}
