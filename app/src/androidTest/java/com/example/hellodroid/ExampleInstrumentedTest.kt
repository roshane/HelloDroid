package com.example.hellodroid

import android.util.Log
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun test_sanity() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.hellodroid", appContext.packageName)
        with(rule) {
            onNodeWithTag(AppConstants.TAG_CELLULAR).performClick()
            waitForIdle()
            onNodeWithTag(AppConstants.TAG_SEND_BUTTON).performClick()
            waitUntilAtLeastOneExists(
                matcher = hasTestTag(AppConstants.TAG_RESPONSE_DATA_JSON),
                10 * 1000
            )
            onNodeWithTag(AppConstants.TAG_RESPONSE_DATA_JSON).assertExists()
            val responseDataJson = rule.onNodeWithTag(AppConstants.TAG_RESPONSE_DATA_JSON)
                .captureText()
            assertTrue(responseDataJson.isNotEmpty())
            sleep(2000)
            Log.d("TEST", responseDataJson)
        }
    }


    private fun SemanticsNodeInteraction.captureText(): String {
        return fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.EditableText)
            ?.text.orEmpty()

    }
}
