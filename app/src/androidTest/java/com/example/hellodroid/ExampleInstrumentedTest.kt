package com.example.hellodroid

import androidx.compose.ui.test.ExperimentalTestApi
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
        val sendButton = rule.onNodeWithTag(AppConstants.TAG_SEND_BUTTON)
        sendButton.performClick()
        rule.waitUntilAtLeastOneExists(matcher = hasTestTag(AppConstants.TAG_RESPONSE_DATA_JSON))
        Thread.sleep(5000)
        val responseText = rule.onNodeWithTag(AppConstants.TAG_RESPONSE_DATA_JSON)
        rule.onNodeWithTag(AppConstants.TAG_RESPONSE_DATA_JSON).assertExists()
    }
}