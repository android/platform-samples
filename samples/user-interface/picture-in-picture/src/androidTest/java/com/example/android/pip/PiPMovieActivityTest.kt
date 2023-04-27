/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.pip

import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.example.android.pip.widget.MovieView
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.AllOf.allOf
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PiPMovieActivityTest {

    @Rule @JvmField
    val rule = ActivityScenarioRule(PiPMovieActivity::class.java)

    @Test
    fun movie_playingOnPip() {
        // The movie should be playing on start
        onView(withId(R.id.movie))
                .check(matches(allOf(isDisplayed(), isPlaying())))
                .perform(showControls())
        // Click on the button to enter Picture-in-Picture mode
        onView(withId(R.id.minimize)).perform(click())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        // The Activity is paused. We cannot use Espresso to test paused activities.
        rule.scenario.onActivity { activity ->
            // We are now in Picture-in-Picture mode
            assertThat(activity.isInPictureInPictureMode).isTrue()
            val view = activity.findViewById<MovieView>(R.id.movie)
            assertNotNull(view)
            // The video should still be playing
            assertThat(view.isPlaying).isTrue()

            // The media session state should be playing.
            assertMediaStateIs(PlaybackStateCompat.STATE_PLAYING)
        }
    }

    @Test
    fun movie_pauseAndResume() {
        // The movie should be playing on start
        onView(withId(R.id.movie))
                .check(matches(allOf(isDisplayed(), isPlaying())))
                .perform(showControls())
        // Pause
        onView(withId(R.id.toggle)).perform(click())
        onView(withId(R.id.movie)).check(matches(not(isPlaying())))
        // The media session state should be paused.
        assertMediaStateIs(PlaybackStateCompat.STATE_PAUSED)
        // Resume
        onView(withId(R.id.toggle)).perform(click())
        onView(withId(R.id.movie)).check(matches(isPlaying()))
        // The media session state should be playing.
        assertMediaStateIs(PlaybackStateCompat.STATE_PLAYING)
    }

    private fun assertMediaStateIs(@PlaybackStateCompat.State expectedState: Int) {
        rule.scenario.onActivity { activity ->
            val state = activity.mediaController.playbackState
            assertNotNull(state)
            assertThat(state!!.state).isEqualTo(expectedState)
        }
    }

    private fun isPlaying(): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun matchesSafely(view: View): Boolean {
                return (view as MovieView).isPlaying
            }

            override fun describeTo(description: Description) {
                description.appendText("MovieView is playing")
            }
        }
    }

    private fun showControls(): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(MovieView::class.java)
            }

            override fun getDescription(): String {
                return "Show controls of MovieView"
            }

            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadUntilIdle()
                (view as MovieView).showControls()
                uiController.loopMainThreadUntilIdle()
            }
        }
    }
}
