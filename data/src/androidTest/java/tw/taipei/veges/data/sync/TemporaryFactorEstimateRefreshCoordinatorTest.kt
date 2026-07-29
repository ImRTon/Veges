package tw.taipei.veges.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tw.taipei.veges.data.estimation.TemporaryFactorArtifactLoader
import tw.taipei.veges.data.local.OfficialVariantEntity
import tw.taipei.veges.data.local.SourceObservationEntity
import tw.taipei.veges.data.local.TaxonomyConceptEntity
import tw.taipei.veges.data.local.VegesDatabase
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.PriceUnit
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.SourceDayState
import tw.taipei.veges.domain.SourceKind

@RunWith(AndroidJUnit4::class)
class TemporaryFactorEstimateRefreshCoordinatorTest {
    private lateinit var context: Context
    private lateinit var database: VegesDatabase
    private val clock = Clock.fixed(Instant.parse("2026-07-26T00:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, VegesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun persistsSingleMarketsAndUsesLatestCommonDateForCombinedBasis() = runTest {
        database.taxonomyDao().replaceConcepts(listOf(concept()))
        database.taxonomyDao().replaceVariants(
            listOf(
                OfficialVariantEntity(
                    conceptId = CONCEPT_ID,
                    commodityCode = "LC1",
                    officialName = "包心白-包白",
                    market = MarketBasis.TAIPEI_FIRST,
                ),
                OfficialVariantEntity(
                    conceptId = CONCEPT_ID,
                    commodityCode = "LC1",
                    officialName = "包心白-包白",
                    market = MarketBasis.TAIPEI_SECOND,
                ),
            ),
        )
        database.sourceDao().replaceObservations(
            listOf(
                observation("first-common", MarketBasis.TAIPEI_FIRST, "2026-07-14", "40", "100"),
                observation("second-common", MarketBasis.TAIPEI_SECOND, "2026-07-14", "60", "300"),
                observation("second-newer", MarketBasis.TAIPEI_SECOND, "2026-07-15", "100", "200"),
            ),
        )
        val coordinator = RoomTemporaryFactorEstimateRefreshCoordinator(
            database = database,
            artifactLoader = TemporaryFactorArtifactLoader(context),
            clock = clock,
        )

        val rows = coordinator.calculateNewEstimates()

        assertEquals(4, rows.size)
        val combined = database.estimateDao().latest(CONCEPT_ID, MarketBasis.TAIPEI_COMBINED)
        val first = database.estimateDao().latest(CONCEPT_ID, MarketBasis.TAIPEI_FIRST)
        val second = database.estimateDao().latest(CONCEPT_ID, MarketBasis.TAIPEI_SECOND)
        assertNotNull(combined)
        assertEquals(LocalDate.parse("2026-07-14"), combined?.sourceDate)
        assertEquals(BigDecimal("66.00"), combined?.pointValue)
        assertEquals(BigDecimal("48.00"), first?.pointValue)
        assertEquals(LocalDate.parse("2026-07-15"), second?.sourceDate)
        assertEquals(BigDecimal("120.00"), second?.pointValue)
        assertEquals("2026-07-26", combined?.estimatorApprovedOn.toString())
        assertEquals("(wholesale NTD/kg × 0.6 kg/台斤) × 2.0", combined?.formula)
        assertEquals(0, combined?.pairedCalibrationPeriods)
    }

    private fun concept() = TaxonomyConceptEntity(
        stableId = CONCEPT_ID,
        householdName = "包心白菜",
        normalizedHouseholdName = "包心白菜",
        category = ProduceCategory.VEGETABLE,
        published = true,
        illustrationAsset = null,
        illustrationDisclosure = "AI 生成示意圖，非實物照片。",
        taxonomyVersion = "test",
        artifactChecksum = "checksum",
        reviewedAt = Instant.parse("2026-07-26T00:00:00Z"),
        reviewer = "test",
    )

    private fun observation(
        id: String,
        market: MarketBasis,
        date: String,
        average: String,
        volume: String,
    ) = SourceObservationEntity(
        observationId = id,
        sourceKind = SourceKind.MOA_WHOLESALE,
        market = market,
        commodityCode = "LC1",
        officialName = "包心白-包白",
        observedOn = LocalDate.parse(date),
        lowerPrice = BigDecimal(average).subtract(BigDecimal.TEN),
        averagePrice = BigDecimal(average),
        upperPrice = BigDecimal(average).add(BigDecimal.TEN),
        priceUnit = PriceUnit.NTD_PER_KILOGRAM,
        volume = BigDecimal(volume),
        volumeUnit = "kg",
        state = SourceDayState.VALID,
        sourceUrl = "https://data.moa.gov.tw/",
        attribution = "農業部",
        retrievedAt = Instant.parse("2026-07-25T12:00:00Z"),
        syncRunId = "run",
    )

    private companion object {
        const val CONCEPT_ID = "vegetable.napa-cabbage"
    }
}
