package tw.taipei.veges

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.taipei.veges.designsystem.VegesTheme
import tw.taipei.veges.domain.PriceRefresh
import tw.taipei.veges.domain.PriceRefreshStage
import tw.taipei.veges.home.HomeScreen
import tw.taipei.veges.home.HomeUiState

@RunWith(AndroidJUnit4::class)
class PriceRefreshProgressSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeShowsHistoricalPriceDownloadProgress() {
        composeRule.setContent {
            VegesTheme {
                HomeScreen(
                    state = HomeUiState(
                        priceRefresh = PriceRefresh(
                            isRunning = true,
                            stage = PriceRefreshStage.HISTORY,
                            fraction = 0.6f,
                        ),
                    ),
                    onBrowseCatalog = {},
                    onRefresh = {},
                    onDeclinerLookbackSelected = {},
                    onConceptSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("正在下載歷史行情").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("正在下載歷史行情").assertIsDisplayed()
    }
}
