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
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
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
import tw.taipei.veges.domain.HomeItem
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.MarketItem
import tw.taipei.veges.domain.MarketPriceSurgeOutlook
import tw.taipei.veges.domain.OfficialCommodityCode
import tw.taipei.veges.domain.OfficialVariant
import tw.taipei.veges.domain.PriceUnit
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.ProduceConcept
import tw.taipei.veges.domain.ProduceConceptId
import tw.taipei.veges.domain.PriceSurgeReason
import tw.taipei.veges.domain.PriceSurgeReasonKind
import tw.taipei.veges.domain.PriceSurgeRiskLevel
import tw.taipei.veges.domain.ScaledPrice
import tw.taipei.veges.domain.TrendPeriod
import tw.taipei.veges.domain.TrendPoint
import tw.taipei.veges.domain.TrendSummary
import tw.taipei.veges.domain.VariantMarketPrice
import tw.taipei.veges.domain.accessibilityText
import tw.taipei.veges.home.HomeScreen
import tw.taipei.veges.home.HomeUiState

private val AnimationTestContentDescriptions = listOf(
    "颱風來襲，整體蔬果價格可能上揚",
    "連續暴雨，整體蔬果價格可能上揚",
    "高溫持續，整體蔬果供應承壓",
    "到貨量普遍縮減，整體價格可能上揚",
    "多項蔬果價格同步上揚",
    "多項蔬果價格高於近期常態",
)

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
    fun homePriceSurgeRadarExplainsMarketWideTyphoonOutlook() {
        composeRule.setContent {
            VegesTheme {
                HomeScreen(
                    state = HomeUiState(
                        marketPriceSurgeOutlook = MarketPriceSurgeOutlook(
                            riskScore = 82,
                            riskLevel = PriceSurgeRiskLevel.HIGH,
                            projectedRisePercent = BigDecimal("26.5"),
                            affectedItemCount = 6,
                            eligibleItemCount = 18,
                            marketBreadthPercent = 33,
                            primaryReason = PriceSurgeReason(
                                kind = PriceSurgeReasonKind.TYPHOON,
                                contribution = 25,
                                headline = "颱風來襲，整體蔬果價格可能上揚",
                                shortLabel = "颱風警報",
                            ),
                            affectedNames = listOf("高麗菜", "青蔥", "小白菜"),
                        ),
                        predictionEligibleCount = 18,
                    ),
                    onBrowseCatalog = {},
                    onRefresh = {},
                    onDeclinerLookbackSelected = {},
                    onConceptSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("漲價雷達").assertIsDisplayed()
        composeRule.onNodeWithText("颱風來襲，整體蔬果價格可能上揚").assertIsDisplayed()
        composeRule.onNodeWithText("6/18 項同步承壓 · 市場廣度 33%").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "6項蔬果同步出現訊號",
            substring = true,
        ).assertIsDisplayed()
    }

    @Test
    fun homePriceSurgeRadarCanPreviewEveryReasonAnimation() {
        composeRule.setContent {
            VegesTheme {
                HomeScreen(
                    state = HomeUiState(),
                    onBrowseCatalog = {},
                    onRefresh = {},
                    onDeclinerLookbackSelected = {},
                    onConceptSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("測試動畫").performClick()

        composeRule.onNodeWithText("動態訊號圖鑑").assertIsDisplayed()
        composeRule.onNodeWithText("即時預覽").assertIsDisplayed()
        AnimationTestContentDescriptions.forEach { description ->
            composeRule.onNodeWithContentDescription(description).assertIsDisplayed()
        }
        composeRule.onNodeWithText("結束測試").assertIsDisplayed()
    }

    @Test
    fun trackedListDragCollapsesAndExpandsPriceSurgeRadar() {
        composeRule.setContent {
            VegesTheme {
                HomeScreen(
                    state = HomeUiState(
                        tracked = List(24) { index ->
                            HomeItem(
                                concept = concept("vegetable.tracked-$index", "追蹤品項 $index"),
                                latestEstimate = null,
                                unavailableReason = null,
                            )
                        },
                        decliners = List(24) { index ->
                            MarketItem(
                                concept = concept("vegetable.decliner-$index", "大跌品項 $index"),
                                latestEstimate = null,
                            )
                        },
                        declinerEligibleCount = 24,
                    ),
                    onBrowseCatalog = {},
                    onRefresh = {},
                    onDeclinerLookbackSelected = {},
                    onConceptSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("測試動畫").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("測試動畫").assertIsNotDisplayed()

        composeRule.onRoot().performTouchInput {
            swipeDown(
                startY = center.y,
                endY = bottom - 24f,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("測試動畫").assertIsDisplayed()

        composeRule.onNodeWithText("大跌").performClick()
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("測試動畫").assertIsNotDisplayed()

        composeRule.onRoot().performTouchInput {
            swipeDown(
                startY = center.y,
                endY = bottom - 24f,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("測試動畫").assertIsDisplayed()
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
    fun detailUsesOneHouseholdNameWithoutExposingOfficialCodes() {
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

        composeRule.onAllNodesWithText("九層塔").assertCountEquals(1)
        composeRule.onAllNodesWithText("LP2", substring = true).assertCountEquals(0)
    }

    @Test
    fun fruitVariantPricesShowUsefulMarketDetailsWithoutInternalCodesOrFiller() {
        val mango = concept("fruit.mango", "芒果").copy(
            category = ProduceCategory.FRUIT,
            officialVariants = listOf(
                OfficialVariant(
                    code = OfficialCommodityCode("A1"),
                    officialName = "芒果-愛文",
                    market = MarketBasis.TAIPEI_FIRST,
                ),
                OfficialVariant(
                    code = OfficialCommodityCode("A2"),
                    officialName = "芒果-金煌",
                    market = MarketBasis.TAIPEI_FIRST,
                ),
            ),
        )
        val variantPrices = listOf(
            VariantMarketPrice(
                commodityCode = OfficialCommodityCode("A1"),
                officialName = "芒果-愛文",
                basis = MarketBasis.TAIPEI_FIRST,
                observedOn = LocalDate.parse("2026-07-29"),
                wholesaleAverage = ScaledPrice(
                    amount = BigDecimal("120.0"),
                    unit = PriceUnit.NTD_PER_TAI_JIN,
                ),
                volumeKg = BigDecimal("800"),
            ),
            VariantMarketPrice(
                commodityCode = OfficialCommodityCode("A2"),
                officialName = "芒果-金煌",
                basis = MarketBasis.TAIPEI_FIRST,
                observedOn = LocalDate.parse("2026-07-28"),
                wholesaleAverage = ScaledPrice(
                    amount = BigDecimal("95.0"),
                    unit = PriceUnit.NTD_PER_TAI_JIN,
                ),
                volumeKg = BigDecimal("600"),
            ),
        )
        composeRule.setContent {
            VegesTheme {
                DetailScreen(
                    state = DetailUiState(
                        concept = mango,
                        variantPrices = variantPrices,
                    ),
                    onBasisSelected = {},
                    onToggleTracked = {},
                    onToggleMethodology = {},
                )
            }
        }

        composeRule.onNodeWithText("品種批發行情").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("行情日期 2026/7/29").assertIsDisplayed()
        composeRule.onAllNodesWithText("官方代碼", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("A1", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("AI 生成示意圖", substring = true).assertCountEquals(0)
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
