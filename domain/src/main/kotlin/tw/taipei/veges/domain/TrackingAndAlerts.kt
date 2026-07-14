package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

const val BACKGROUND_REFRESH_DISCLOSURE = "背景更新由 Android 排程，可能延遲；開啟 App 可立即嘗試更新。"

data class TrackedProduce(
    val conceptId: ProduceConceptId,
    val trackedAt: Instant,
)

data class AlertRule(
    val ruleId: String,
    val conceptId: ProduceConceptId,
    val basis: MarketBasis,
    val thresholdNtdPerTaiJin: BigDecimal,
    val enabled: Boolean,
    val conditionMet: Boolean,
    val lastEvaluatedSourceDate: LocalDate?,
)

sealed interface UntrackResult {
    data object Untracked : UntrackResult
    data class RequiresActiveAlertConfirmation(val activeAlertCount: Int) : UntrackResult
}

class TrackingUseCases(
    private val repository: TrackingRepository,
) {
    suspend fun track(conceptId: ProduceConceptId, now: Instant): TrackedProduce =
        repository.track(TrackedProduce(conceptId, now))

    suspend fun untrack(
        conceptId: ProduceConceptId,
        removeActiveAlerts: Boolean,
    ): UntrackResult {
        val activeAlerts = repository.activeAlertCount(conceptId)
        if (activeAlerts > 0 && !removeActiveAlerts) {
            return UntrackResult.RequiresActiveAlertConfirmation(activeAlerts)
        }
        repository.untrack(conceptId, removeActiveAlerts)
        return UntrackResult.Untracked
    }
}

class AlertRuleUseCases(
    private val repository: AlertRuleRepository,
) {
    suspend fun create(
        conceptId: ProduceConceptId,
        basis: MarketBasis,
        thresholdNtdPerTaiJin: BigDecimal,
        now: Instant,
    ): AlertRule {
        require(thresholdNtdPerTaiJin > BigDecimal.ZERO) { "Alert threshold must be positive" }
        return repository.save(
            AlertRule(
                ruleId = repository.newRuleId(),
                conceptId = conceptId,
                basis = basis,
                thresholdNtdPerTaiJin = thresholdNtdPerTaiJin,
                enabled = true,
                conditionMet = false,
                lastEvaluatedSourceDate = null,
            ),
            now,
        )
    }

    suspend fun update(
        rule: AlertRule,
        thresholdNtdPerTaiJin: BigDecimal,
        enabled: Boolean,
        now: Instant,
    ): AlertRule {
        require(thresholdNtdPerTaiJin > BigDecimal.ZERO) { "Alert threshold must be positive" }
        return repository.save(rule.copy(thresholdNtdPerTaiJin = thresholdNtdPerTaiJin, enabled = enabled), now)
    }

    suspend fun delete(ruleId: String) = repository.delete(ruleId)
}

interface TrackingRepository {
    suspend fun track(produce: TrackedProduce): TrackedProduce

    suspend fun untrack(conceptId: ProduceConceptId, removeActiveAlerts: Boolean)

    suspend fun activeAlertCount(conceptId: ProduceConceptId): Int

    fun observeTracked(): Flow<List<TrackedProduce>>
}

interface AlertRuleRepository {
    fun newRuleId(): String

    suspend fun save(rule: AlertRule, now: Instant): AlertRule

    suspend fun delete(ruleId: String)

    fun observeRules(): Flow<List<AlertRule>>
}
