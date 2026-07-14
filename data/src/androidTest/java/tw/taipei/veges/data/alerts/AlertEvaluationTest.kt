package tw.taipei.veges.data.alerts

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tw.taipei.veges.data.local.AlertRuleEntity
import tw.taipei.veges.data.local.EstimateEntity
import tw.taipei.veges.data.local.VegesDatabase
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.PriceUnit

@RunWith(AndroidJUnit4::class)
class AlertEvaluationTest {
    private lateinit var database: VegesDatabase
    private lateinit var coordinator: AlertEvaluationCoordinator

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, VegesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        coordinator = AlertEvaluationCoordinator(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun belowThresholdCreatesOneEventAndDuplicateEvaluationIsIgnored() = runTest {
        database.alertDao().replaceRule(rule())
        val estimate = estimate("estimate-1", BigDecimal("39"), LocalDate.parse("2026-07-14"))

        val first = coordinator.evaluate(estimate, instant())
        val second = coordinator.evaluate(estimate, instant())

        assertEquals(AlertEvaluationSummary(1, 1, 1), first)
        assertEquals(AlertEvaluationSummary(1, 0, 0), second)
        assertEquals(1, countEvents())
    }

    @Test
    fun conditionMustClearBeforeItCanRearm() = runTest {
        database.alertDao().replaceRule(rule())
        coordinator.evaluate(estimate("estimate-1", BigDecimal("39"), date("2026-07-14")), instant())
        val remainsMet = coordinator.evaluate(estimate("estimate-2", BigDecimal("38"), date("2026-07-15")), instant())
        val clears = coordinator.evaluate(estimate("estimate-3", BigDecimal("50"), date("2026-07-16")), instant())
        val rearms = coordinator.evaluate(estimate("estimate-4", BigDecimal("39"), date("2026-07-17")), instant())

        assertEquals(0, remainsMet.enteredMetState)
        assertEquals(0, clears.enteredMetState)
        assertEquals(1, rearms.enteredMetState)
        assertEquals(2, countEvents())
    }

    @Test
    fun unavailableEstimateCannotTriggerAlert() = runTest {
        database.alertDao().replaceRule(rule())
        val unavailable = estimate("estimate-1", null, date("2026-07-14"), unavailable = "MODEL_INELIGIBLE")

        val summary = coordinator.evaluate(unavailable, instant())

        assertEquals(AlertEvaluationSummary(0, 0, 0), summary)
        assertEquals(0, countEvents())
    }

    private fun rule() = AlertRuleEntity(
        ruleId = "rule-1",
        conceptId = "vegetable.cabbage",
        basis = MarketBasis.TAIPEI_COMBINED,
        thresholdNtdPerTaiJin = BigDecimal("40"),
        enabled = true,
        conditionMet = false,
        lastEvaluatedSourceDate = null,
        lastTransitionEstimateId = null,
        createdAt = instant(),
        updatedAt = instant(),
    )

    private fun estimate(id: String, point: BigDecimal?, date: LocalDate, unavailable: String? = null) = EstimateEntity(
        estimateId = id,
        conceptId = "vegetable.cabbage",
        basis = MarketBasis.TAIPEI_COMBINED,
        modelVersion = "model-v1",
        sourceDate = date,
        sourceDatesJson = "[\"$date\"]",
        calibrationCutoff = date,
        pairedCalibrationPeriods = 30,
        calculatedAt = instant(),
        pointValue = point,
        pointUnit = point?.let { PriceUnit.NTD_PER_TAI_JIN },
        intervalLower = null,
        intervalUpper = null,
        confidence = null,
        unavailableReason = unavailable?.let(tw.taipei.veges.domain.UnavailableReason::valueOf),
        disclosureShortTag = "估算",
        disclosureFullLabel = "Taipei retail reference estimate",
    )

    private fun countEvents(): Int = database.openHelper.readableDatabase.query(
        "SELECT COUNT(*) FROM notification_events",
    ).use { cursor ->
        cursor.moveToFirst()
        cursor.getInt(0)
    }

    private fun instant() = Instant.parse("2026-07-14T03:00:00Z")

    private fun date(value: String) = LocalDate.parse(value)
}
