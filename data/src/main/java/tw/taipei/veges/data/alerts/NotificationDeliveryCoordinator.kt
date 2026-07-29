package tw.taipei.veges.data.alerts

import java.time.Instant
import javax.inject.Inject
import tw.taipei.veges.data.local.VegesDatabase

data class NotificationDeliverySummary(
    val pending: Int,
    val delivered: Int,
    val deferred: Int,
)

class NotificationDeliveryCoordinator @Inject constructor(
    private val database: VegesDatabase,
    private val publisher: LocalNotificationPublisher,
) {
    suspend fun deliverPending(now: Instant): NotificationDeliverySummary {
        publisher.ensureChannels()
        val pending = database.alertDao().pendingNotificationDeliveries(MAX_DELIVERIES_PER_RUN)
        var delivered = 0
        pending.forEach { row ->
            val notification = PriceAlertNotification(
                notificationId = row.eventId.hashCode(),
                conceptId = row.conceptId,
                householdName = row.householdName,
                basis = row.basis,
                estimateNtdPerTaiJin = row.estimateNtdPerTaiJin,
                thresholdNtdPerTaiJin = row.thresholdNtdPerTaiJin,
                sourceDate = row.sourceDate,
                deepLink = "veges://produce/${row.conceptId}",
            )
            if (publisher.publish(notification)) {
                delivered += database.alertDao().markNotificationDelivered(row.eventId, now)
            }
        }
        return NotificationDeliverySummary(
            pending = pending.size,
            delivered = delivered,
            deferred = pending.size - delivered,
        )
    }

    private companion object {
        const val MAX_DELIVERIES_PER_RUN = 100
    }
}
