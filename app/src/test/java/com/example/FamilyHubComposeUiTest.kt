package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.components.EditorialEmptyState
import com.example.ui.components.MemberAvatar
import com.example.ui.theme.FamilyHubTheme
import com.example.ui.theme.ThemeMode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class FamilyHubComposeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `member avatar renders initials fallback when photoPath is null`() {
        composeRule.setContent {
            FamilyHubTheme(themeMode = ThemeMode.LIGHT) {
                MemberAvatar(fullName = "Elena Vance", photoPath = null, memberId = 1L)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("EV").assertIsDisplayed()
    }

    @Test
    fun `editorial empty state displays title and invokes CTA action`() {
        var clicked = false
        composeRule.setContent {
            FamilyHubTheme(themeMode = ThemeMode.DARK) {
                EditorialEmptyState(
                    drawableRes = R.drawable.img_empty_album,
                    title = "Your Family Album Awaits",
                    subtitle = "Create a profile in seconds.",
                    actionLabel = "Create First Profile",
                    actionTag = "empty_cta",
                    onAction = { clicked = true }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Your Family Album Awaits").assertIsDisplayed()
        composeRule.onNodeWithTag("empty_cta").performClick()
        assertTrue(clicked)
    }
}
