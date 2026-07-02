package com.folio.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val baselineRule = BaselineProfileRule()

    @Test
    fun generate() {
        baselineRule.collect(
            packageName = "com.folio.app",
            includeInStartupProfile = true
        ) {
            // 1. Cold start → Bookshelf
            startActivityAndWait()
            device.waitForIdle()

            // 2. Scroll the bookshelf to warm up list rendering
            val composeView = device.wait(
                Until.findObject(By.clazz("androidx.compose.ui.platform.ComposeView")),
                5_000
            )
            composeView?.also { view ->
                view.setGestureMargin(200)
                view.fling(Direction.DOWN, 600)
                device.waitForIdle()
                view.fling(Direction.UP, 600)
                device.waitForIdle()
            }

            // 3. Open settings to warm up settings screen
            val settingsIcon = device.wait(
                Until.findObject(By.desc("Settings")),
                2_000
            )
            settingsIcon?.click()
            device.waitForIdle()

            // Navigate back
            device.pressBack()
            device.waitForIdle()
        }
    }
}
