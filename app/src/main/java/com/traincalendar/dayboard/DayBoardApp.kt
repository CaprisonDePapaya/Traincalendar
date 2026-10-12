package com.traincalendar.dayboard

import android.app.Application
import com.traincalendar.dayboard.widget.WidgetRefreshWorker

class DayBoardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WidgetRefreshWorker.schedule(this)
    }
}
