package com.example

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BatchTabContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testBatchTabContent_EmptyQueue() {
        composeTestRule.setContent {
            MyApplicationTheme {
                BatchTabContent(
                    batchQueue = emptyList(),
                    autoMatchDeviceFrameRatio = true,
                    onAutoMatchDeviceFrameRatioChange = {},
                    onPickScreenshots = {},
                    onRemoveScreenshot = {},
                    onClearQueue = {},
                    onProcessBatch = {}
                )
            }
        }

        // Verify key UI elements are displayed
        composeTestRule.onNodeWithText("Batch Process").assertIsDisplayed()
        composeTestRule.onNodeWithText("Queue is empty").assertIsDisplayed()
        composeTestRule.onNodeWithTag("batch_select_button").assertIsDisplayed()

        // Verify action buttons are hidden when queue is empty
        composeTestRule.onNodeWithTag("batch_clear_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("batch_process_button").assertDoesNotExist()
    }

    @Test
    fun testBatchTabContent_WithItems() {
        val mockUri1 = Uri.parse("content://media/external/images/media/1")
        val mockUri2 = Uri.parse("content://media/external/images/media/2")
        val queue = listOf(mockUri1, mockUri2)

        composeTestRule.setContent {
            MyApplicationTheme {
                BatchTabContent(
                    batchQueue = queue,
                    autoMatchDeviceFrameRatio = true,
                    onAutoMatchDeviceFrameRatioChange = {},
                    onPickScreenshots = {},
                    onRemoveScreenshot = {},
                    onClearQueue = {},
                    onProcessBatch = {}
                )
            }
        }

        // Verify Queue count in the header
        composeTestRule.onNodeWithText("Screenshots Queue (2)").assertIsDisplayed()

        // Verify action buttons are displayed when queue contains items
        composeTestRule.onNodeWithTag("batch_clear_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("batch_process_button").assertIsDisplayed()
    }
}
