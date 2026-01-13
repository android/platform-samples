package com.example.android.pip

import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PiPSampleActivityTest {

    /**
     * Verifies that the timer starts and updates the time.
     */
    @Test
    fun startTimer_updatesTime() {
        val scenario = launch(PiPSampleActivity::class.java)

        scenario.onActivity { activity ->
            val startBtn = activity.findViewById<View>(R.id.start_or_pause)
            val timeText = activity.findViewById<TextView>(R.id.time)

            // Initial state
            assertThat(timeText.text.toString()).isEqualTo("00:00:00")

            // Start timer
            startBtn.performClick()
        }

        // Wait for async update
        Thread.sleep(1500)

        scenario.onActivity { activity ->
            val timeText = activity.findViewById<TextView>(R.id.time)

            // Time should now be updated
            assertThat(timeText.text.toString()).isNotEqualTo("00:00:00")
        }
    }

    /**
     * Verifies that the Clear button resets the timer.
     */
    @Test
    fun clearTimer_resetsTime() {
        val scenario = launch(PiPSampleActivity::class.java)

        scenario.onActivity { activity ->
            val startBtn = activity.findViewById<View>(R.id.start_or_pause)
            val clearBtn = activity.findViewById<View>(R.id.clear)
            val timeText = activity.findViewById<TextView>(R.id.time)

            startBtn.performClick()
            clearBtn.performClick()

            // Time should reset
            assertThat(timeText.text.toString()).isEqualTo("00:00:00")
        }
    }
}
