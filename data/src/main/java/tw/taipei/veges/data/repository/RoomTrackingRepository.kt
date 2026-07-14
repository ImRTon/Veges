package tw.taipei.veges.data.repository

import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tw.taipei.veges.data.local.TrackedConceptEntity
import tw.taipei.veges.data.local.VegesDatabase
import tw.taipei.veges.domain.AlertRule
import tw.taipei.veges.domain.AlertRuleRepository
import tw.taipei.veges.domain.ProduceConceptId
import tw.taipei.veges.domain.TrackingRepository
import tw.taipei.veges.domain.TrackedProduce

class RoomTrackingRepository @Inject constructor(
    private val database: VegesDatabase,
) : TrackingRepository, AlertRuleRepository {
    override suspend fun track(produce: TrackedProduce): TrackedProduce {
        database.trackingDao().replaceTrackedConcept(
            TrackedConceptEntity(
                conceptId = produce.conceptId.value,
                trackedAt = produce.trackedAt,
                updatedAt = produce.trackedAt,
            ),
        )
        return produce
    }

    override suspend fun untrack(conceptId: ProduceConceptId, removeActiveAlerts: Boolean) {
        database.withTransaction {
            database.trackingDao().deleteTrackedConcept(conceptId.value)
            if (removeActiveAlerts) database.trackingDao().deleteActiveAlerts(conceptId.value)
        }
    }

    override suspend fun activeAlertCount(conceptId: ProduceConceptId): Int =
        database.trackingDao().activeAlertCount(conceptId.value)

    override fun observeTracked(): Flow<List<TrackedProduce>> =
        database.trackingDao().observeTracked().map { rows ->
            rows.map { TrackedProduce(ProduceConceptId(it.conceptId), it.trackedAt) }
        }

    override fun newRuleId(): String = UUID.randomUUID().toString()

    override suspend fun save(rule: AlertRule, now: Instant): AlertRule {
        database.alertDao().replaceRule(rule.toEntity(now))
        return rule
    }

    override suspend fun delete(ruleId: String) {
        database.alertDao().delete(ruleId)
    }

    override fun observeRules(): Flow<List<AlertRule>> =
        database.alertDao().observeAllRules().map { rows -> rows.map { it.toDomain() } }
}

private fun AlertRule.toEntity(now: Instant) = tw.taipei.veges.data.local.AlertRuleEntity(
    ruleId = ruleId,
    conceptId = conceptId.value,
    basis = basis,
    thresholdNtdPerTaiJin = thresholdNtdPerTaiJin,
    enabled = enabled,
    conditionMet = conditionMet,
    lastEvaluatedSourceDate = lastEvaluatedSourceDate,
    lastTransitionEstimateId = null,
    createdAt = now,
    updatedAt = now,
)

private fun tw.taipei.veges.data.local.AlertRuleEntity.toDomain() = AlertRule(
    ruleId = ruleId,
    conceptId = ProduceConceptId(conceptId),
    basis = basis,
    thresholdNtdPerTaiJin = thresholdNtdPerTaiJin,
    enabled = enabled,
    conditionMet = conditionMet,
    lastEvaluatedSourceDate = lastEvaluatedSourceDate,
)
