package tw.taipei.veges.data.alerts

import java.time.Instant
import javax.inject.Inject
import tw.taipei.veges.data.local.EstimateEntity
import tw.taipei.veges.data.local.NotificationEventEntity
import tw.taipei.veges.data.local.VegesDatabase

data class AlertEvaluationSummary(
    val evaluatedRules: Int,
    val enteredMetState: Int,
    val notificationEventsCreated: Int,
)

class AlertEvaluationCoordinator @Inject constructor(
    private val database: VegesDatabase,
) {
    suspend fun evaluate(estimate: EstimateEntity, now: Instant): AlertEvaluationSummary {
        if (estimate.unavailableReason != null || estimate.pointValue == null) {
            return AlertEvaluationSummary(0, 0, 0)
        }
        val rules = database.alertDao().enabledRulesFor(estimate.conceptId, estimate.basis)
        var enteredMet = 0
        var events = 0
        rules.forEach { rule ->
            val entered = database.alertDao().evaluateBelowThreshold(
                ruleId = rule.ruleId,
                estimate = estimate,
                now = now,
                notificationEvent = if (!rule.conditionMet && estimate.pointValue < rule.thresholdNtdPerTaiJin) {
                    NotificationEventEntity(
                        eventId = notificationEventId(rule.ruleId, estimate.estimateId),
                        eventIdentity = notificationEventIdentity(rule.ruleId, estimate.estimateId),
                        ruleId = rule.ruleId,
                        estimateId = estimate.estimateId,
                        conceptId = estimate.conceptId,
                        basis = estimate.basis,
                        sourceDate = estimate.sourceDate,
                        createdAt = now,
                        deliveredAt = null,
                    )
                } else {
                    null
                },
            )
            if (entered) {
                enteredMet++
                events++
            }
        }
        return AlertEvaluationSummary(rules.size, enteredMet, events)
    }

    private fun notificationEventId(ruleId: String, estimateId: String): String =
        "notification:$ruleId:$estimateId"

    private fun notificationEventIdentity(ruleId: String, estimateId: String): String =
        "below-threshold-transition:$ruleId:$estimateId"
}
