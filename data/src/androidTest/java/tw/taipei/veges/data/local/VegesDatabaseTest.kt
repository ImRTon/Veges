package tw.taipei.veges.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.time.LocalDate
import java.math.BigDecimal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tw.taipei.veges.data.repository.normalizeSearchQuery
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.SourceDayState
import tw.taipei.veges.domain.SourceKind
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.PriceUnit
import tw.taipei.veges.domain.EstimateDisclosure

@RunWith(AndroidJUnit4::class)
class VegesDatabaseTest {
    private lateinit var database: VegesDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, VegesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun schemaOpensWithAllCoreTables() {
        val tables = database.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type = 'table'",
        ).use { cursor ->
            buildSet {
                val nameColumn = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) add(cursor.getString(nameColumn))
            }
        }

        assertTrue(tables.containsAll(setOf(
            "taxonomy_concepts",
            "taxonomy_aliases",
            "official_variants",
            "source_observations",
            "source_day_states",
            "sync_runs",
            "model_metadata",
            "estimates",
            "tracked_concepts",
            "alert_rules",
            "notification_events",
        )))
    }

    @Test
    fun exportedSchemaCanBeOpenedAtCurrentVersion() {
        val helper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            VegesDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

        helper.createDatabase("schema-test", 3).close()
    }

    @Test
    fun migrationOneToTwoPreservesEstimateAndAddsProvenanceColumns() {
        val helper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            VegesDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )
        helper.createDatabase("migration-test", 1).apply {
            execSQL(
                """
                INSERT INTO estimates(
                    estimateId, conceptId, basis, modelVersion, sourceDate, calibrationCutoff,
                    calculatedAt, pointValue, pointUnit, intervalLower, intervalUpper, confidence,
                    unavailableReason
                ) VALUES (
                    'estimate-1', 'vegetable.cabbage', 'TAIPEI_COMBINED', 'model-v1',
                    '2026-07-14', '2026-06-30', '2026-07-14T03:00:00Z', 52.0,
                    'NTD_PER_TAI_JIN', NULL, NULL, NULL, NULL
                )
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate("migration-test", 2, true, MIGRATION_1_2)
        migrated.query("SELECT disclosureShortTag, disclosureFullLabel FROM estimates WHERE estimateId = 'estimate-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(EstimateDisclosure.SHORT_TAG, cursor.getString(0))
            assertEquals(EstimateDisclosure.FULL_LABEL, cursor.getString(1))
        }
        migrated.close()
    }

    @Test
    fun migrationTwoToThreePreservesEstimateAndAddsEstimatorPolicyProvenance() {
        val helper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            VegesDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )
        helper.createDatabase("migration-2-3-test", 2).apply {
            execSQL(
                """
                INSERT INTO estimates(
                    estimateId, conceptId, basis, modelVersion, sourceDate, sourceDatesJson,
                    calibrationCutoff, pairedCalibrationPeriods, calculatedAt, pointValue,
                    pointUnit, intervalLower, intervalUpper, confidence, unavailableReason,
                    disclosureShortTag, disclosureFullLabel
                ) VALUES (
                    'estimate-1', 'vegetable.cabbage', 'TAIPEI_COMBINED', 'model-v1',
                    '2026-07-14', '["2026-07-14"]', '2026-06-30', 18,
                    '2026-07-14T03:00:00Z', 52.0, 'NTD_PER_TAI_JIN',
                    NULL, NULL, NULL, NULL, '估算', 'Taipei retail reference estimate'
                )
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            "migration-2-3-test",
            3,
            true,
            MIGRATION_2_3,
        )
        migrated.query(
            "SELECT estimatorApprovedOn, formula FROM estimates WHERE estimateId = 'estimate-1'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("2026-06-30", cursor.getString(0))
            assertEquals("CALIBRATED_MODEL", cursor.getString(1))
        }
        migrated.close()
    }

    @Test
    fun normalizedAliasSearchReturnsPublishedConcept() = runTest {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        database.taxonomyDao().replaceVersion(
            version = "test-v1",
            concepts = listOf(
                TaxonomyConceptEntity(
                    stableId = "vegetable.cabbage",
                    householdName = "高麗菜",
                    normalizedHouseholdName = normalizeSearchQuery("高麗菜"),
                    category = ProduceCategory.VEGETABLE,
                    published = true,
                    illustrationAsset = null,
                    illustrationDisclosure = "AI 生成示意圖，非實物照片。",
                    taxonomyVersion = "test-v1",
                    artifactChecksum = "checksum",
                    reviewedAt = now,
                    reviewer = "test",
                ),
            ),
            aliases = listOf(
                TaxonomyAliasEntity("vegetable.cabbage", normalizeSearchQuery("包心菜"), "包心菜"),
            ),
            variants = emptyList(),
        )

        val results = database.taxonomyDao()
            .observeSearch("%${normalizeSearchQuery("包心菜")}%")
            .first()

        assertEquals(listOf("vegetable.cabbage"), results.map { it.concept.stableId })
    }

    @Test
    fun transactionRollbackLeavesNoSyncRun() = runTest {
        val run = SyncRunEntity(
            runId = "run-1",
            sourceKind = SourceKind.MOA_WHOLESALE,
            startedAt = Instant.parse("2026-01-01T00:00:00Z"),
            completedAt = null,
            status = SourceDayState.FAILED,
            requestedFrom = LocalDate.parse("2026-01-01"),
            requestedTo = LocalDate.parse("2026-01-01"),
            pagesFetched = 1,
            recordsAccepted = 0,
            diagnostic = "test failure",
        )

        try {
            database.withTransaction {
                database.sourceDao().replaceRun(run)
                error("interrupted import")
            }
        } catch (_: IllegalStateException) {
            // Expected: the transaction must roll back the staged run.
        }

        assertNull(database.sourceDao().run("run-1"))
    }

    @Test
    fun repeatedObservationUsesStableIdentityWithoutDuplication() = runTest {
        val observation = SourceObservationEntity(
            observationId = "MOA_WHOLESALE:TAIPEI_SECOND:LA1:2026-07-14",
            sourceKind = SourceKind.MOA_WHOLESALE,
            market = MarketBasis.TAIPEI_SECOND,
            commodityCode = "LA1",
            officialName = "甘藍-初秋",
            observedOn = LocalDate.parse("2026-07-14"),
            lowerPrice = BigDecimal("34.3"),
            averagePrice = BigDecimal("40.5"),
            upperPrice = BigDecimal("49.7"),
            priceUnit = PriceUnit.NTD_PER_KILOGRAM,
            volume = BigDecimal("25470"),
            volumeUnit = "kg",
            state = SourceDayState.VALID,
            sourceUrl = "https://example.test",
            attribution = "test",
            retrievedAt = Instant.parse("2026-07-14T02:00:00Z"),
            syncRunId = "run-1",
        )

        database.sourceDao().replaceObservations(listOf(observation))
        database.sourceDao().replaceObservations(listOf(observation.copy(averagePrice = BigDecimal("41.0"))))

        val rows = database.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM source_observations WHERE observationId = 'MOA_WHOLESALE:TAIPEI_SECOND:LA1:2026-07-14'",
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
        assertEquals(1, rows)
    }
}
