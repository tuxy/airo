package com.tuxy.airo

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class AiroTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun appOpens() {
        rule.setContent { MainScreen() }
    }
}