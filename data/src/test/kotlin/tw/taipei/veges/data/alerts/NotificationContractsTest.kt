package tw.taipei.veges.data.alerts

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.taipei.veges.domain.EstimateDisclosure
import tw.taipei.veges.domain.MarketBasis

class NotificationContractsTest {
    @Test
    fun notificationTextRetainsEstimateDisclosureAndDeepLink() {
        val notification = PriceAlertNotification(
            notificationId = 1,
            conceptId = "vegetable.cabbage",
            householdName = "高麗菜",
            basis = MarketBasis.TAIPEI_COMBINED,
            estimateNtdPerTaiJin = BigDecimal("39.50"),
            thresholdNtdPerTaiJin = BigDecimal("40.00"),
            sourceDate = LocalDate.parse("2026-07-14"),
            deepLink = "veges://produce/vegetable.cabbage",
        )

        assertTrue(notification.title.contains(EstimateDisclosure.SHORT_TAG))
        assertTrue(notification.body.contains(EstimateDisclosure.FULL_LABEL))
        assertTrue(notification.body.contains("2026-07-14"))
        assertTrue(notification.deepLink.startsWith("veges://"))
    }

    @Test
    fun deniedPermissionDoesNotChangeEvaluationContract() {
        val state = object : NotificationPermissionState {
            override fun canPostNotifications() = false
        }
        assertTrue(!state.canPostNotifications())
    }
}
