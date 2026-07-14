package tw.taipei.veges.data

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.taipei.veges.domain.Estimate
import tw.taipei.veges.domain.EstimateDisclosure
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.PriceUnit
import tw.taipei.veges.domain.ProduceConceptId

class OfflineFirstScenarioTest {
    @Test
    fun failedRefreshKeepsCachedEstimateAndDoesNotCreateUnavailableValue() = runTest {
        val fakeSource = FakeSource(::estimate)
        fakeSource.cache.value = estimate()
        fakeSource.failNextRefresh = true

        val result = fakeSource.refresh()

        assertEquals(SourceState.FAILED, result)
        assertEquals(BigDecimal("52.00"), fakeSource.cache.first()?.point?.amount)
        assertEquals(EstimateDisclosure.SHORT_TAG, fakeSource.cache.first()?.disclosure?.shortTag)
    }

    @Test
    fun invalidSourceStateCannotBecomeAlertableEstimate() = runTest {
        val fakeSource = FakeSource(::estimate)
        fakeSource.sourceState = SourceState.INVALID

        val result = fakeSource.refresh()

        assertEquals(SourceState.INVALID, result)
        assertTrue(fakeSource.cache.value == null)
        assertEquals(0, fakeSource.notificationEvents)
    }

    private fun estimate() = Estimate(
        conceptId = ProduceConceptId("vegetable.cabbage"),
        basis = MarketBasis.TAIPEI_COMBINED,
        modelVersion = "model-v1",
        wholesaleSourceDates = listOf(LocalDate.parse("2026-07-14")),
        calibrationCutoff = LocalDate.parse("2026-07-01"),
        calculatedAt = Instant.parse("2026-07-14T03:00:00Z"),
        point = tw.taipei.veges.domain.ScaledPrice(BigDecimal("52.00"), PriceUnit.NTD_PER_TAI_JIN),
        intervalLower = null,
        intervalUpper = null,
        confidence = null,
        unavailableReason = null,
    )

    private class FakeSource(
        private val estimateFactory: () -> Estimate,
    ) {
        val cache = MutableStateFlow<Estimate?>(null)
        var failNextRefresh = false
        var sourceState = SourceState.VALID
        var notificationEvents = 0

        suspend fun refresh(): SourceState {
            if (failNextRefresh) {
                failNextRefresh = false
                return SourceState.FAILED
            }
            if (sourceState != SourceState.VALID) return sourceState
            cache.value = estimateFactory()
            return SourceState.VALID
        }
    }

    private enum class SourceState {
        VALID,
        INVALID,
        FAILED,
    }
}
