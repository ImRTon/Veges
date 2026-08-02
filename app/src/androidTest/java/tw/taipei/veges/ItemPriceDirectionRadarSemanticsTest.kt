package tw.taipei.veges

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import tw.taipei.veges.designsystem.VegesTheme
import tw.taipei.veges.detail.ItemPriceDirectionRadarCard
import tw.taipei.veges.domain.ItemPriceDirection
import tw.taipei.veges.domain.ItemPriceDirectionEvaluation
import tw.taipei.veges.domain.ItemPriceDirectionOutlook
import tw.taipei.veges.domain.ItemPriceDirectionReason
import tw.taipei.veges.domain.ItemPriceDirectionReasonKind
import tw.taipei.veges.domain.ItemPriceDirectionStatus
import tw.taipei.veges.domain.MarketBasis

@RunWith(AndroidJUnit4::class)
class ItemPriceDirectionRadarSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun fallingRadarUsesDirectionTextEvidenceAndQualifiedSemantics() {
        composeRule.setContent {
            VegesTheme {
                ItemPriceDirectionRadarCard(
                    householdName = "高麗菜",
                    basis = MarketBasis.TAIPEI_COMBINED,
                    evaluation = signalEvaluation(ItemPriceDirection.FALLING),
                )
            }
        }

        composeRule.onNodeWithText("降價訊號偏強").assertIsDisplayed()
        composeRule.onNodeWithText("-18.4%").assertIsDisplayed()
        composeRule.onAllNodesWithText("價格轉弱").assertCountEquals(0)
        composeRule.onNodeWithText("到貨量增").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "高麗菜 個別價格雷達",
            substring = true,
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText(
            "批發價量訊號",
            substring = true,
        ).assertCountEquals(0)
        composeRule.onAllNodesWithText(
            "依批發價量",
            substring = true,
        ).assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription(
            "不保證未來漲跌",
            substring = true,
        ).assertCountEquals(0)
    }

    @Test
    fun risingRadarUsesExplicitUpwardText() {
        composeRule.setContent {
            VegesTheme {
                ItemPriceDirectionRadarCard(
                    householdName = "青蔥",
                    basis = MarketBasis.TAIPEI_FIRST,
                    evaluation = signalEvaluation(ItemPriceDirection.RISING),
                )
            }
        }

        composeRule.onNodeWithText("漲價訊號偏強").assertIsDisplayed()
        composeRule.onNodeWithText("+18.4%").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "上漲，預估 +18.4%",
            substring = true,
        ).assertIsDisplayed()
    }

    @Test
    fun insufficientHistoryShowsProgressWithoutForecastPercent() {
        composeRule.setContent {
            VegesTheme {
                ItemPriceDirectionRadarCard(
                    householdName = "番茄",
                    basis = MarketBasis.TAIPEI_SECOND,
                    evaluation = ItemPriceDirectionEvaluation(
                        status = ItemPriceDirectionStatus.INSUFFICIENT_HISTORY,
                        validTradingDayCount = 6,
                        latestObservationDate = LocalDate.parse("2026-07-29"),
                    ),
                )
            }
        }

        composeRule.onNodeWithText("資料累積中").assertIsDisplayed()
        composeRule.onNodeWithText(
            "已有 6/10 個有效交易日",
            substring = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "資料累積中",
            substring = true,
        ).assertIsDisplayed()
    }

    @Test
    fun fallingRadarRendersVisualQaArtifact() {
        composeRule.setContent {
            VegesTheme {
                ItemPriceDirectionRadarCard(
                    householdName = "高麗菜",
                    basis = MarketBasis.TAIPEI_COMBINED,
                    evaluation = signalEvaluation(ItemPriceDirection.FALLING),
                )
            }
        }
        composeRule.waitForIdle()

        val output = File(
            requireNotNull(
                InstrumentationRegistry.getInstrumentation().targetContext.externalCacheDir,
            ),
            "item-price-direction-radar.png",
        )
        FileOutputStream(output).use { stream ->
            composeRule.onRoot()
                .captureToImage()
                .asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        assertTrue(output.length() > 0L)
    }

    private fun signalEvaluation(
        direction: ItemPriceDirection,
    ): ItemPriceDirectionEvaluation {
        val rising = direction == ItemPriceDirection.RISING
        return ItemPriceDirectionEvaluation(
            status = ItemPriceDirectionStatus.SIGNAL,
            validTradingDayCount = 23,
            latestObservationDate = LocalDate.parse("2026-07-29"),
            outlook = ItemPriceDirectionOutlook(
                direction = direction,
                strengthScore = 72,
                projectedChangePercent = BigDecimal(if (rising) "18.4" else "-18.4"),
                reasons = listOf(
                    ItemPriceDirectionReason(
                        kind = if (rising) {
                            ItemPriceDirectionReasonKind.PRICE_MOMENTUM_UP
                        } else {
                            ItemPriceDirectionReasonKind.PRICE_MOMENTUM_DOWN
                        },
                        contribution = 40,
                        headline = if (rising) "近期均價明顯轉強" else "近期均價明顯轉弱",
                        shortLabel = if (rising) "價格轉強" else "價格轉弱",
                    ),
                    ItemPriceDirectionReason(
                        kind = if (rising) {
                            ItemPriceDirectionReasonKind.VOLUME_CONTRACTION
                        } else {
                            ItemPriceDirectionReasonKind.VOLUME_EXPANSION
                        },
                        contribution = 25,
                        headline = if (rising) {
                            "近期到貨量縮，供應壓力增加"
                        } else {
                            "近期到貨量增加，供應較寬鬆"
                        },
                        shortLabel = if (rising) "到貨量縮" else "到貨量增",
                    ),
                ),
            ),
        )
    }
}
