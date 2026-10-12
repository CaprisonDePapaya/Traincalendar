package com.traincalendar.dayboard

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/**
 * End-to-end flows on a real device: the board starts as the user's own,
 * and every event on it can be added, edited, deleted and restored.
 */
@RunWith(AndroidJUnit4::class)
class BoardFlowTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val prefs
        get() = context.getSharedPreferences("day_board", Context.MODE_PRIVATE)

    @Before
    fun freshInstall() {
        prefs.edit().clear().commit()
    }

    @Test
    fun addEditDeleteAndUndo() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // A fresh board has no demo events in the way.
            waitForText("Your board is empty.")
            compose.onNodeWithText("Coffee with Team").assertDoesNotExist()
            shot("01-empty-board")

            compose.onNodeWithText("ADD EVENT").performClick()
            compose.onNodeWithTag("event-title").performTextInput("Gym")
            shot("02-add-form")
            compose.onNodeWithTag("event-save").performClick()
            waitForText("Gym")
            shot("03-board-with-event")

            compose.onNodeWithText("Gym").performClick()
            compose.onNodeWithText("EDIT").assertIsDisplayed()
            shot("04-event-sheet")
            compose.onNodeWithText("EDIT").performClick()
            compose.onNodeWithTag("event-title").performTextReplacement("Gym session")
            compose.onNodeWithText("SAVE CHANGES").performClick()
            waitForText("Gym session")

            compose.onNodeWithText("Gym session").performClick()
            compose.onNodeWithText("DELETE").performClick()
            waitForText("Your board is empty.")
            compose.onNodeWithText("UNDO").performClick()
            waitForText("Gym session")

            compose.onNodeWithText("Gym session").performClick()
            compose.onNodeWithText("DELETE").performClick()
            waitForText("Your board is empty.")
            compose.onNodeWithText("Gym session").assertDoesNotExist()
        }
    }

    @Test
    fun sampleDayCanBeTurnedOffFromAnEvent() {
        prefs.edit().putBoolean("sample_day_on", true).commit()
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForText("Coffee with Team")
            compose.onNodeWithText("Coffee with Team").performClick()
            compose.onNodeWithText("TURN OFF SAMPLE DAY").assertIsDisplayed()
            shot("05-sample-event-sheet")
            compose.onNodeWithText("TURN OFF SAMPLE DAY").performClick()
            waitForText("Your board is empty.")
            compose.onNodeWithText("Coffee with Team").assertDoesNotExist()
        }
    }

    @Test
    fun settingsAndLines() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForText("Your board is empty.")
            compose.onNodeWithTag("settings").performClick()
            compose.onNodeWithText("Show phone calendar").assertIsDisplayed()
            shot("06-settings")
            compose.onNodeWithText("Done").performClick()

            compose.onNodeWithText("LINES").performClick()
            waitForText("Travel")
            shot("07-lines")
            compose.onNodeWithTag("delete-line-travel").performClick()
            compose.onNodeWithText("Delete").performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithText("Travel").fetchSemanticsNodes().isEmpty()
            }
        }
    }

    private fun waitForText(text: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Logs a small JPEG of the screen in base64 chunks, so CI can rebuild it
     * from logcat without needing access to the device's storage.
     */
    private fun shot(name: String) {
        runCatching {
            compose.waitForIdle()
            Thread.sleep(600)
            val raw = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
            val screen = if (raw.config == Bitmap.Config.HARDWARE) raw.copy(Bitmap.Config.ARGB_8888, false) else raw
            val width = 300
            val scaled = Bitmap.createScaledBitmap(screen, width, screen.height * width / screen.width, true)
            val bytes = ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.JPEG, 55, it) }.toByteArray()
            val chunks = Base64.encodeToString(bytes, Base64.NO_WRAP).chunked(3000)
            chunks.forEachIndexed { i, chunk -> Log.i("DayBoardShot", "$name ${i + 1}/${chunks.size} $chunk") }
        }.onFailure { Log.w("DayBoardShot", "screenshot $name failed", it) }
    }
}
