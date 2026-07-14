package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingAlertUseCasesTest {
    @Test
    fun untrackRequiresConfirmationWhenActiveAlertsExist() = runTest {
        val repository = FakeTrackingRepository(activeAlerts = 2)
        val useCases = TrackingUseCases(repository)

        val result = useCases.untrack(ProduceConceptId("vegetable.cabbage"), removeActiveAlerts = false)

        assertEquals(UntrackResult.RequiresActiveAlertConfirmation(2), result)
        assertTrue(repository.untrackCalls.isEmpty())
    }

    @Test
    fun untrackWithConfirmationRemovesTrackedConceptAndAlerts() = runTest {
        val repository = FakeTrackingRepository(activeAlerts = 1)
        val useCases = TrackingUseCases(repository)

        val result = useCases.untrack(ProduceConceptId("vegetable.cabbage"), removeActiveAlerts = true)

        assertEquals(UntrackResult.Untracked, result)
        assertEquals(listOf(true), repository.untrackCalls)
    }

    @Test
    fun alertRuleRejectsZeroOrNegativeThreshold() = runTest {
        val useCases = AlertRuleUseCases(FakeAlertRuleRepository())

        val error = runCatching {
            useCases.create(
                ProduceConceptId("vegetable.cabbage"),
                MarketBasis.TAIPEI_COMBINED,
                BigDecimal.ZERO,
                Instant.parse("2026-07-14T03:00:00Z"),
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun alertKeepsOriginalBasisWhenRuleIsUpdated() = runTest {
        val repository = FakeAlertRuleRepository()
        val useCases = AlertRuleUseCases(repository)
        val original = useCases.create(
            ProduceConceptId("vegetable.cabbage"),
            MarketBasis.TAIPEI_FIRST,
            BigDecimal("40"),
            Instant.parse("2026-07-14T03:00:00Z"),
        )

        val updated = useCases.update(
            original,
            thresholdNtdPerTaiJin = BigDecimal("38"),
            enabled = false,
            now = Instant.parse("2026-07-15T03:00:00Z"),
        )

        assertEquals(MarketBasis.TAIPEI_FIRST, updated.basis)
        assertEquals(BigDecimal("38"), updated.thresholdNtdPerTaiJin)
        assertTrue(!updated.enabled)
    }

    private class FakeTrackingRepository(
        private val activeAlerts: Int,
    ) : TrackingRepository {
        val untrackCalls = mutableListOf<Boolean>()

        override suspend fun track(produce: TrackedProduce) = produce
        override suspend fun untrack(conceptId: ProduceConceptId, removeActiveAlerts: Boolean) {
            untrackCalls += removeActiveAlerts
        }
        override suspend fun activeAlertCount(conceptId: ProduceConceptId) = activeAlerts
        override fun observeTracked(): Flow<List<TrackedProduce>> = emptyFlow()
    }

    private class FakeAlertRuleRepository : AlertRuleRepository {
        private var count = 0
        override fun newRuleId() = "rule-${++count}"
        override suspend fun save(rule: AlertRule, now: Instant) = rule
        override suspend fun delete(ruleId: String) = Unit
        override fun observeRules(): Flow<List<AlertRule>> = emptyFlow()
    }
}
