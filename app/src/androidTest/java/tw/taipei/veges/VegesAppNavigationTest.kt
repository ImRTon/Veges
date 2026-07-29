package tw.taipei.veges

import android.os.SystemClock
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VegesAppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun topLevelNavigationRestoresCatalogAfterActivityRecreation() {
        composeRule.onNodeWithTag("top-level-蔬菜市場").performClick()
        composeRule.onNodeWithText("搜尋名稱或官方代碼").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("搜尋名稱或官方代碼").assertIsDisplayed()
        composeRule.onNodeWithTag("top-level-蔬菜市場").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun officialCodeSearchReturnsTheReviewedConceptWithinThreeSeconds() {
        composeRule.onNodeWithTag("top-level-蔬菜市場").performClick()
        composeRule.onNodeWithText("葉菜類").assertIsDisplayed()

        val startedAt = SystemClock.elapsedRealtime()
        composeRule.onNodeWithText("搜尋名稱或官方代碼").performTextInput("LP2")
        composeRule.onNodeWithText("九層塔").assertIsDisplayed()
        val elapsedMs = SystemClock.elapsedRealtime() - startedAt

        assertTrue("Official-code search took ${elapsedMs}ms", elapsedMs <= 3_000L)
    }
}
