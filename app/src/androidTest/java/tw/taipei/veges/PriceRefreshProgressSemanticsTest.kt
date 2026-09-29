package tw.taipei.veges

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.taipei.veges.designsystem.VegesTheme
import tw.taipei.veges.domain.PriceRefresh
import tw.taipei.veges.home.HomeScreen
import tw.taipei.veges.home.HomeUiState

@RunWith(AndroidJUnit4::class)
class PriceRefreshProgressSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeKeepsPriceDateInHeaderWhileRefreshing() {
        composeRule.setContent {
            VegesTheme {
                HomeScreen(
                    state = HomeUiState(
                        latestPriceDate = LocalDate.parse("2026-09-28"),
                        manualRefreshPending = true,
                        priceRefresh = PriceRefresh(
                            isRunning = true,
                        ),
                    ),
                    onBrowseCatalog = {},
                    onRefresh = {},
                    onDeclinerLookbackSelected = {},
                    onConceptSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("9/28 更新").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("正在更新行情").assertIsDisplayed()
        val headerCenterY = composeRule.onNodeWithText("行情")
            .fetchSemanticsNode().boundsInRoot.center.y
        listOf(
            composeRule.onNodeWithText("9/28 更新"),
            composeRule.onNodeWithContentDescription("正在更新行情"),
            composeRule.onNodeWithContentDescription("新增追蹤"),
        ).forEach { item ->
            assertEquals(headerCenterY, item.fetchSemanticsNode().boundsInRoot.center.y, 2f)
        }
    }

    @Test
    fun pullingEitherListRequestsRefresh() {
        var refreshCount = 0
        composeRule.setContent {
            VegesTheme {
                HomeScreen(
                    state = HomeUiState(),
                    onBrowseCatalog = {},
                    onRefresh = { refreshCount++ },
                    onDeclinerLookbackSelected = {},
                    onConceptSelected = {},
                )
            }
        }

        fun pullFromList() {
            composeRule.onRoot().performTouchInput {
                swipeDown(
                    startY = bottom * 0.65f,
                    endY = bottom - 24f,
                    durationMillis = 400,
                )
            }
            composeRule.waitForIdle()
        }

        pullFromList()
        assertEquals(1, refreshCount)
        composeRule.onNodeWithText("大跌").performClick()
        composeRule.waitForIdle()
        pullFromList()
        assertEquals(2, refreshCount)
    }
}
