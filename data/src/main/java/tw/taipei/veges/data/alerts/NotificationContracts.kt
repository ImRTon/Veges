package tw.taipei.veges.data.alerts

import java.math.BigDecimal
import java.time.LocalDate
import tw.taipei.veges.domain.EstimateDisclosure
import tw.taipei.veges.domain.MarketBasis

data class PriceAlertNotification(
    val notificationId: Int,
    val conceptId: String,
    val householdName: String,
    val basis: MarketBasis,
    val estimateNtdPerTaiJin: BigDecimal,
    val thresholdNtdPerTaiJin: BigDecimal,
    val sourceDate: LocalDate,
    val disclosure: EstimateDisclosure = EstimateDisclosure(),
    val deepLink: String,
) {
    val title: String
        get() = "$householdName ${disclosure.shortTag}"

    val body: String
        get() = "${disclosure.fullLabel}：NT$${estimateNtdPerTaiJin.toPlainString()} / 台斤，低於門檻 NT$${thresholdNtdPerTaiJin.toPlainString()}；來源日期 $sourceDate"
}

interface NotificationPermissionState {
    fun canPostNotifications(): Boolean
}

interface LocalNotificationPublisher {
    fun ensureChannels()

    fun publish(notification: PriceAlertNotification): Boolean
}
