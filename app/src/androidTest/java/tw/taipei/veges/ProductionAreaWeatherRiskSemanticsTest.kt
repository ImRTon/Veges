package tw.taipei.veges

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.math.BigDecimal
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.taipei.veges.designsystem.VegesTheme
import tw.taipei.veges.domain.MarketShockKind
import tw.taipei.veges.domain.ProductionAreaWeatherRisk
import tw.taipei.veges.home.HomeScreen
import tw.taipei.veges.home.HomeUiState

@RunWith(AndroidJUnit4::class)
class ProductionAreaWeatherRiskSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeShowsQualifiedTyphoonProductionRiskBeforeMarketSurge() {
        composeRule.setContent {
            VegesTheme {
                HomeScreen(
                    state = HomeUiState(
                        productionAreaWeatherRisk = ProductionAreaWeatherRisk(
                            kind = MarketShockKind.TYPHOON,
                            affectedCounties = listOf("苗栗縣", "臺中市", "南投縣", "雲林縣"),
                            severity = BigDecimal("0.9"),
                            expiresAt = Instant.parse("2026-08-09T15:00:00Z"),
                        ),
                    ),
                    onBrowseCatalog = {},
                    onRefresh = {},
                    onDeclinerLookbackSelected = {},
                    onConceptSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("漲價雷達").assertIsDisplayed()
        composeRule.onNodeWithText("颱風影響苗栗縣、臺中市、南投縣等 4 個產區").assertIsDisplayed()
        composeRule.onNodeWithText("近期蔬果價格可能上漲").assertIsDisplayed()
        composeRule.onAllNodesWithText(
            "產地風險，不代表價格已上漲",
            substring = true,
        ).assertCountEquals(0)
        composeRule.onNodeWithContentDescription(
            "颱風影響苗栗縣、臺中市、南投縣等 4 個產區。近期蔬果價格可能上漲",
        ).assertIsDisplayed()
    }
}
