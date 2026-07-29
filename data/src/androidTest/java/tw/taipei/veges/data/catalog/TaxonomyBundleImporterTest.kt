package tw.taipei.veges.data.catalog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tw.taipei.veges.data.local.TrackedConceptEntity
import tw.taipei.veges.data.local.VegesDatabase

@RunWith(AndroidJUnit4::class)
class TaxonomyBundleImporterTest {
    private lateinit var database: VegesDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            VegesDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun approvedBundleImportsIdempotentlyAndPublishesReviewedConcepts() = runTest {
        val importer = TaxonomyBundleImporter(database)
        val first = importer.importAsset(ApplicationProvider.getApplicationContext())
        val trackedAt = Instant.parse("2026-07-28T00:00:00Z")
        database.trackingDao().replaceTrackedConcept(
            TrackedConceptEntity(
                conceptId = "vegetable.moa.lp2",
                trackedAt = trackedAt,
                updatedAt = trackedAt,
            ),
        )
        val second = importer.importAsset(ApplicationProvider.getApplicationContext())

        assertEquals(first, second)
        assertEquals(121, first.importedConcepts)
        val vegetables = database.taxonomyDao().observePublished(tw.taipei.veges.domain.ProduceCategory.VEGETABLE)
            .first()
        val fruit = database.taxonomyDao().observePublished(tw.taipei.veges.domain.ProduceCategory.FRUIT)
            .first()
        assertEquals(120, vegetables.size)
        assertEquals(1, fruit.size)
        assertTrue(vegetables.all { it.concept.published })
        assertTrue(vegetables.any { it.concept.householdName == "空心菜" })
        assertTrue(vegetables.any { it.concept.householdName == "青江菜" })
        assertTrue(vegetables.any { it.concept.householdName == "地瓜葉" })
        assertTrue(database.trackingDao().observeIsTracked("vegetable.moa.lp2").first())
    }
}
