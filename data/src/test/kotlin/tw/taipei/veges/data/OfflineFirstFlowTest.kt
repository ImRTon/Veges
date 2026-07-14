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
import tw.taipei.veges.domain.AlertRule
import tw.taipei.veges.domain.AlertRuleRepository
import tw.taipei.veges.domain.Estimate
import tw.taipei.veges.domain.EstimateDisclosure
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.PriceUnit
import tw.taipei.veges.domain.ProduceConceptId
import tw.taipei.veges.domain.ProduceRepository
import tw.taipei.veges.domain.TrackingRepository
import tw.taipei.veges.domain.TrackingUseCases
import tw.taipei.veges.domain.TrackedProduce

class OfflineFirstFlowTest {
    @Test
    fun fakeOfflineFlowRetainsCachedEstimateAndLocalTracking() = runTest {
        val repository = FakeOfflineRepository()
        val conceptId = ProduceConceptId("vegetable.cabbage")
        val tracking = TrackingUseCases(repository)

        tracking.track(conceptId, Instant.parse("2026-07-14T03:00:00Z"))
        repository.cachedEstimate.value = estimate(conceptId)
        repository.refreshFailed = true

        val cached = repository.cachedEstimate.first()

        assertEquals(BigDecimal("52.00"), cached?.point?.amount)
        assertEquals(listOf(conceptId), repository.tracked.map { it.conceptId })
        assertTrue(repository.refreshFailed)
        assertEquals(EstimateDisclosure.SHORT_TAG, cached?.disclosure?.shortTag)
    }

    private fun estimate(conceptId: ProduceConceptId) = Estimate(
        conceptId = conceptId,
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

    private class FakeOfflineRepository : TrackingRepository {
        val cachedEstimate = MutableStateFlow<Estimate?>(null)
        val tracked = mutableListOf<TrackedProduce>()
        var refreshFailed = false

        override suspend fun track(produce: TrackedProduce): TrackedProduce {
            tracked += produce
            return produce
        }

        override suspend fun untrack(conceptId: ProduceConceptId, removeActiveAlerts: Boolean) {
            tracked.removeAll { it.conceptId == conceptId }
        }

        override suspend fun activeAlertCount(conceptId: ProduceConceptId) = 0
        override fun observeTracked() = MutableStateFlow(tracked.toList())
    }
}
