package com.folio.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
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
class PageCurlBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun pageCurlFrameTiming() {
        benchmarkRule.measureRepeated(
            packageName = "com.folio.app",
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            baselineProfileMode = BaselineProfileMode.Require,
            iterations = 10
        ) {
            pressHome()
            startActivityAndWait()
            device.waitForIdle()

            // Open settings via the settings icon button (top-right)
            val settingsIcon = device.wait(
                Until.findObject(By.desc("Settings")),
                5_000
            )
            settingsIcon?.click()
            device.waitForIdle()

            // Navigate back to bookshelf
            device.pressBack()
            device.waitForIdle()

            // Scroll the bookshelf list to exercise list rendering
            val listView = device.wait(
                Until.findObject(By.clazz("androidx.compose.ui.platform.ComposeView")),
                5_000
            )
            listView?.also { view ->
                view.setGestureMargin(200)
                view.fling(Direction.DOWN, 800)
                device.waitForIdle()
                view.fling(Direction.UP, 800)
                device.waitForIdle()
            }
        }
    }
}
