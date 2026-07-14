package tw.taipei.veges.data.catalog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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
    fun candidateBundleImportsIdempotentlyAndKeepsCandidatesUnpublished() = runTest {
        val importer = TaxonomyBundleImporter(database)
        val first = importer.importAsset(ApplicationProvider.getApplicationContext())
        val second = importer.importAsset(ApplicationProvider.getApplicationContext())

        assertEquals(first, second)
        assertEquals(3, first.importedConcepts)
        val published = database.taxonomyDao().observePublished(tw.taipei.veges.domain.ProduceCategory.VEGETABLE)
            .first()
        assertTrue(published.isEmpty())
    }
}
