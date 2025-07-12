package com.tuxy.airo

import android.content.Context
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelectable
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

class AiroAddFlowTest {

    @get:Rule
    val rule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun appOpens() {
        rule.setContent { MainScreen() }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testAddFlight() {
        val flightNumber = "VJ83"
        val date = "Monday, July 14, 2025"

        rule.setContent { MainScreen() }
        rule.onRoot(useUnmergedTree = true)

        selectDefaultApi() // Select default API before performing action

        val addFlightButton = rule.onNodeWithTag("add_flight")
        addFlightButton.performClick()

        // NewFlightScreen.kt

        val textField = rule.onNodeWithText(context.getString(R.string.flight_number))
        textField.performTextInput(flightNumber)
        textField.performKeyInput {
             pressKey(Key.Enter)
        }

        // DatePickerScreen.kt

        rule.onNodeWithText(date).performClick()

        val secondAddFlightButton = rule.onNodeWithTag("add_flight")
        secondAddFlightButton.performClick()

        rule.waitUntilAtLeastOneExists(
            matcher = hasText(context.getString(R.string.my_flights)),
            timeoutMillis = 40000
        )

        // Back to MainFlightScreen.kt
        rule.onNodeWithText(context.getString(R.string.flight)).assertExists()
    }

    private fun selectDefaultApi() {
        val settingsButton = rule.onNodeWithTag("settings_button")
        settingsButton.performClick()


        val selectableButton = rule.onNodeWithText(context.getString(R.string.default_server))
        selectableButton.assertIsSelectable() // Will fail if settings_button fails to enter proper screen
        selectableButton.performClick()
        selectableButton.assertIsSelected()

        val apply = rule.onNodeWithTag("apply_settings")
        apply.performClick() // Will fail if wrong button selected or something went wrong
    }
}