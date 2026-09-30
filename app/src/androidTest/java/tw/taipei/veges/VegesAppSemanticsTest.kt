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
import androidx.compose.ui.test.onAllNodesWithContentDescription
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
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.taipei.veges.alerts.AlertRuleItem
import tw.taipei.veges.alerts.SettingsScreen
import tw.taipei.veges.catalog.CatalogScreen
import tw.taipei.veges.catalog.CatalogUiState
import tw.taipei.veges.designsystem.EstimateDisclosureLabel
import tw.taipei.veges.designsystem.VegesTheme
import tw.taipei.veges.detail.DetailScreen
import tw.taipei.veges.detail.DetailUiState
import tw.taipei.veges.detail.WholesaleTrendChart
import tw.taipei.veges.domain.AlertRule
import tw.taipei.veges.domain.CatalogSearchResult
import tw.taipei.veges.domain.Estimate
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
import tw.taipei.veges.domain.ThemeMode
import tw.taipei.veges.domain.TrendPeriod
import tw.taipei.veges.domain.TrendPoint
import tw.taipei.veges.domain.TrendSummary
import tw.taipei.veges.domain.VariantMarketPrice
import tw.taipei.veges.domain.accessibilityText
import tw.taipei.veges.home.HomeScreen
import tw.taipei.veges.home.HomeUiState

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
    fun homePriceSurgeRadarExplainsObservedMarketOutlook() {
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
                                kind = PriceSurgeReasonKind.VOLUME_CONTRACTION,
                                contribution = 25,
                                headline = "到貨量普遍縮減，整體價格可能上揚",
                                shortLabel = "到貨量縮",
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
        composeRule.onNodeWithText("到貨量普遍縮減，整體價格可能上揚").assertIsDisplayed()
        composeRule.onNodeWithText("近 3 個交易日，6 項蔬果價格偏高").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "近 3 個交易日有6項蔬果價格偏高",
            substring = true,
        ).assertIsDisplayed()
    }

    @Test
    fun homePriceSurgeRadarDoesNotExposeInternalAnimationControls() {
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

        composeRule.onAllNodesWithText("測試動畫").assertCountEquals(0)
        composeRule.onAllNodesWithText("動態訊號圖鑑").assertCountEquals(0)
    }

    @Test
    fun trackedListOffersReorderWithoutVisibleHint() {
        composeRule.setContent {
            VegesTheme {
                HomeScreen(
                    state = HomeUiState(
                        tracked = listOf(
                            HomeItem(
                                concept = concept("vegetable.cabbage", "高麗菜"),
                                latestEstimate = null,
                                unavailableReason = null,
                            ),
                            HomeItem(
                                concept = concept("vegetable.spinach", "菠菜"),
                                latestEstimate = null,
                                unavailableReason = null,
                            ),
                        ),
                    ),
                    onBrowseCatalog = {},
                    onRefresh = {},
                    onDeclinerLookbackSelected = {},
                    onConceptSelected = {},
                )
            }
        }

        composeRule.onAllNodesWithText("長按並拖曳可調整順序").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("長按並拖曳可調整順序", substring = true)
            .assertCountEquals(2)
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
                        marketPriceSurgeOutlook = MarketPriceSurgeOutlook(
                            riskScore = 70,
                            riskLevel = PriceSurgeRiskLevel.ELEVATED,
                            projectedRisePercent = BigDecimal("21.0"),
                            affectedItemCount = 3,
                            eligibleItemCount = 10,
                            marketBreadthPercent = 30,
                            primaryReason = PriceSurgeReason(
                                kind = PriceSurgeReasonKind.VOLUME_CONTRACTION,
                                contribution = 25,
                                headline = "到貨量普遍縮減，整體價格可能上揚",
                                shortLabel = "到貨量縮",
                            ),
                            affectedNames = emptyList(),
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
        composeRule.onRoot().performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("漲價雷達").assertIsNotDisplayed()

        composeRule.onRoot().performTouchInput {
            swipeDown(
                startY = center.y,
                endY = bottom - 24f,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("漲價雷達").assertIsDisplayed()

        composeRule.onNodeWithText("大跌").performClick()
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("漲價雷達").assertIsNotDisplayed()

        composeRule.onRoot().performTouchInput {
            swipeDown(
                startY = center.y,
                endY = bottom - 24f,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("漲價雷達").assertIsDisplayed()
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
        var backClicks = 0
        var alertClicks = 0
        var trackedClicks = 0
        composeRule.setContent {
            VegesTheme {
                DetailScreen(
                    state = DetailUiState(),
                    onBasisSelected = { selectedBasis = it },
                    onToggleTracked = { trackedClicks += 1 },
                    onToggleMethodology = {},
                    onSetAlert = { alertClicks += 1 },
                    onBack = { backClicks += 1 },
                )
            }
        }

        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.onNodeWithContentDescription("設定提醒").performClick()
        composeRule.onNodeWithContentDescription("加入追蹤").performClick()
        composeRule.runOnIdle {
            assertEquals(1, backClicks)
            assertEquals(1, alertClicks)
            assertEquals(1, trackedClicks)
        }
        composeRule.onNodeWithText("台北一").performClick()
        composeRule.runOnIdle { assertEquals(MarketBasis.TAIPEI_FIRST, selectedBasis) }
        composeRule.onNodeWithText("目前無可用估算").assertIsDisplayed()
        composeRule.onAllNodesWithText("估算").assertCountEquals(0)
        composeRule.onAllNodesWithText("Taipei retail reference estimate").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("返回").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("設定提醒").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("加入追蹤").assertIsDisplayed()
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
    fun detailShowsWholesaleDateWithoutInternalApprovalDate() {
        val estimate = Estimate(
            conceptId = ProduceConceptId("vegetable.cabbage"),
            basis = MarketBasis.TAIPEI_COMBINED,
            modelVersion = "test",
            wholesaleSourceDates = listOf(LocalDate.parse("2026-09-29")),
            calibrationCutoff = LocalDate.parse("2026-07-26"),
            calculatedAt = Instant.parse("2026-09-29T08:00:00Z"),
            point = ScaledPrice(BigDecimal("32.5"), PriceUnit.NTD_PER_TAI_JIN),
            intervalLower = null,
            intervalUpper = null,
            confidence = null,
            unavailableReason = null,
        )
        composeRule.setContent {
            VegesTheme {
                DetailScreen(
                    state = DetailUiState(
                        concept = concept("vegetable.cabbage", "高麗菜"),
                        estimate = estimate,
                    ),
                    onBasisSelected = {},
                    onToggleTracked = {},
                    onToggleMethodology = {},
                )
            }
        }

        composeRule.onNodeWithText("9/29 批發行情").assertIsDisplayed()
        composeRule.onAllNodesWithText("核准", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("2026-07-26", substring = true).assertCountEquals(0)
    }

    @Test
    fun settingsCombinesThemeAlertsAndSources() {
        var selectedTheme: ThemeMode? = null
        var edited: AlertRuleItem? = null
        val rule = AlertRuleItem(
            rule = AlertRule(
                ruleId = "rule-1",
                conceptId = ProduceConceptId("vegetable.cabbage"),
                basis = MarketBasis.TAIPEI_FIRST,
                thresholdNtdPerTaiJin = BigDecimal("25"),
                enabled = true,
                conditionMet = false,
                lastEvaluatedSourceDate = null,
            ),
            produceName = "高麗菜",
            illustrationAsset = null,
        )
        composeRule.setContent {
            VegesTheme {
                SettingsScreen(
                    themeMode = ThemeMode.SYSTEM,
                    alertRules = listOf(rule),
                    notificationsEnabled = false,
                    onThemeModeSelected = { selectedTheme = it },
                    onToggleAlert = {},
                    onEditAlert = { edited = it },
                    onOpenNotificationSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("深色").performClick()
        composeRule.onNodeWithText("高麗菜").performClick()
        composeRule.runOnIdle {
            assertEquals(ThemeMode.DARK, selectedTheme)
            assertEquals(rule, edited)
        }
        composeRule.onNodeWithText("台北一・低於 25 元 / 台斤").assertIsDisplayed()
        composeRule.onNodeWithText("通知已關閉").assertIsDisplayed()
        composeRule.onAllNodesWithText("vegetable.cabbage", substring = true).assertCountEquals(0)
        composeRule.onNodeWithText("資料來源與隱私").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun settingsExplainsHowToAddTheFirstAlert() {
        composeRule.setContent {
            VegesTheme {
                SettingsScreen(
                    themeMode = ThemeMode.SYSTEM,
                    alertRules = emptyList(),
                    notificationsEnabled = true,
                    onThemeModeSelected = {},
                    onToggleAlert = {},
                    onEditAlert = {},
                    onOpenNotificationSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("尚未設定提醒").assertIsDisplayed()
        composeRule.onAllNodesWithText("通知已關閉").assertCountEquals(0)
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
