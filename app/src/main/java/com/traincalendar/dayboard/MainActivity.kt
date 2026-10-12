package com.traincalendar.dayboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.traincalendar.dayboard.ui.BoardViewModel
import com.traincalendar.dayboard.ui.DayBoardRoot

class MainActivity : ComponentActivity() {
    private val viewModel: BoardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
        )
        super.onCreate(savedInstanceState)
        setContent { DayBoardRoot(viewModel) }
    }

    override fun onResume() {
        super.onResume()
        // Calendar contents or permissions may have changed while we were away.
        viewModel.reload()
    }
}
