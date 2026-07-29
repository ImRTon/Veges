package tw.taipei.veges

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.taipei.veges.catalog.CatalogScreen
import tw.taipei.veges.catalog.CatalogUiState
import tw.taipei.veges.designsystem.EstimateDisclosureLabel
import tw.taipei.veges.designsystem.VegesTheme
import tw.taipei.veges.detail.DetailScreen
import tw.taipei.veges.detail.DetailUiState
import tw.taipei.veges.detail.WholesaleTrendChart
import tw.taipei.veges.domain.CatalogSearchResult
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.OfficialCommodityCode
import tw.taipei.veges.domain.OfficialVariant
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.ProduceConcept
import tw.taipei.veges.domain.ProduceConceptId
import tw.taipei.veges.domain.TrendPeriod
import tw.taipei.veges.domain.TrendPoint
import tw.taipei.veges.domain.TrendSummary
import tw.taipei.veges.domain.accessibilityText

@RunWith(AndroidJUnit4::class)
class VegesAppSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeExposesTraditionalChineseAndEstimateDisclosureSemantics() {
        composeRule.setContent {
            VegesTheme { EstimateDisclosureLabel() }
        }

        composeRule.onNodeWithText("估算").fetchSemanticsNode()
        composeRule.onNodeWithText("Taipei retail reference estimate").fetchSemanticsNode()
    }

    @Test
    fun ambiguityRequiresAnExplicitReviewedConceptChoice() {
        var selected: String? = null
        val first = concept("vegetable.small-bok-choy", "小白菜")
        val second = concept("vegetable.qingjiang-bok-choy", "青江菜")
        composeRule.setContent {
            VegesTheme {
                CatalogScreen(
                    state = CatalogUiState(
                        query = "小白菜",
                        ambiguity = CatalogSearchResult.Ambiguous("小白菜", listOf(first, second)),
                    ),
                    onQueryChange = {},
                    onCategorySelected = {},
                    onConceptSelected = { selected = it },
                    onDismissAmbiguity = {},
                )
            }
        }

        composeRule.onNodeWithText("請選擇「小白菜」的意思").assertIsDisplayed()
        composeRule.onNodeWithText("青江菜").performClick()
        composeRule.runOnIdle { assertEquals("vegetable.qingjiang-bok-choy", selected) }
    }

    @Test
    fun sourceSwitchUnavailableDisclosureAndChartSemanticsAreExposed() {
        var selectedBasis: MarketBasis? = null
        composeRule.setContent {
            VegesTheme {
                DetailScreen(
                    state = DetailUiState(),
                    onBasisSelected = { selectedBasis = it },
                    onToggleTracked = {},
                    onToggleMethodology = {},
                )
            }
        }

        composeRule.onNodeWithText("台北一").performClick()
        composeRule.runOnIdle { assertEquals(MarketBasis.TAIPEI_FIRST, selectedBasis) }
        composeRule.onNodeWithText("目前無可用估算").assertIsDisplayed()
        composeRule.onNodeWithText("Taipei retail reference estimate").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "線體連接前一交易日與當日平均價",
            substring = true,
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun detailShowsOneIdentityWhenTheSameOfficialCodeServesBothMarkets() {
        val basil = concept("vegetable.lp2", "九層塔").copy(
            officialVariants = listOf(
                OfficialVariant(
                    code = OfficialCommodityCode("LP2"),
                    officialName = "九層塔",
                    market = MarketBasis.TAIPEI_FIRST,
                ),
                OfficialVariant(
                    code = OfficialCommodityCode("LP2"),
                    officialName = "九層塔",
                    market = MarketBasis.TAIPEI_SECOND,
                ),
            ),
        )
        composeRule.setContent {
            VegesTheme {
                DetailScreen(
                    state = DetailUiState(concept = basil),
                    onBasisSelected = {},
                    onToggleTracked = {},
                    onToggleMethodology = {},
                )
            }
        }

        composeRule.onAllNodesWithText("九層塔 · LP2").assertCountEquals(1)
    }

    @Test
    fun largeFontKeepsDisclosuresAndChartSummaryReachable() {
        composeRule.setContent {
            VegesTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalDensity provides androidx.compose.ui.unit.Density(1f, 2f),
                ) {
                    Column {
                        EstimateDisclosureLabel()
                        WholesaleTrendChart(trendSummary())
                    }
                }
            }
        }

        composeRule.onNodeWithText("估算").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(trendSummary().accessibilityText())
            .assertIsDisplayed()
    }

    @Test
    fun lightAndDarkScreenshotSurfacesAreDistinctAndMeetBodyContrast() {
        val dark = mutableStateOf(false)
        var foreground = Color.Unspecified
        var background = Color.Unspecified
        composeRule.setContent {
            VegesTheme(darkTheme = dark.value) {
                foreground = MaterialTheme.colorScheme.onSurface
                background = MaterialTheme.colorScheme.surface
                Surface(Modifier.size(240.dp).testTag("theme-screenshot")) {
                    Column {
                        EstimateDisclosureLabel()
                        WholesaleTrendChart(trendSummary())
                    }
                }
            }
        }

        val light = composeRule.onNodeWithTag("theme-screenshot").captureToImage()
        composeRule.runOnIdle { assertTrue(contrastRatio(foreground, background) >= 4.5) }
        composeRule.runOnUiThread { dark.value = true }
        composeRule.waitForIdle()
        val darkImage = composeRule.onNodeWithTag("theme-screenshot").captureToImage()
        composeRule.runOnIdle { assertTrue(contrastRatio(foreground, background) >= 4.5) }

        assertEquals(light.width, darkImage.width)
        assertEquals(light.height, darkImage.height)
        assertNotEquals(
            light.toPixelMap()[2, 2],
            darkImage.toPixelMap()[2, 2],
        )
    }

    private fun concept(id: String, name: String) = ProduceConcept(
        id = ProduceConceptId(id),
        householdName = name,
        aliases = emptyList(),
        category = ProduceCategory.VEGETABLE,
        published = true,
        illustrationAsset = null,
        officialVariants = emptyList(),
    )

    private fun trendSummary() = TrendSummary(
        period = TrendPeriod.SEVEN_DAYS,
        latest = TrendPoint(
            date = LocalDate.parse("2026-07-26"),
            basis = MarketBasis.TAIPEI_FIRST,
            lowerNtdPerKg = BigDecimal("28"),
            averageNtdPerKg = BigDecimal("35"),
            upperNtdPerKg = BigDecimal("42"),
            volumeKg = BigDecimal("1200"),
        ),
        minimumAverage = BigDecimal("30"),
        maximumAverage = BigDecimal("40"),
        directionText = "持平",
    )

    private fun contrastRatio(first: Color, second: Color): Float {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
