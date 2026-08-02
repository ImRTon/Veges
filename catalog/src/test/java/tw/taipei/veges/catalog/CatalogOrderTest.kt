package tw.taipei.veges.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tw.taipei.veges.domain.MarketItem
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.ProduceConcept
import tw.taipei.veges.domain.ProduceConceptId

class CatalogOrderTest {
    @Test
    fun horizontalSwipeChangesSectionOnlyAfterThreshold() {
        assertEquals(1, sectionSwipeDirection(horizontalDistance = -80f, threshold = 56f))
        assertEquals(-1, sectionSwipeDirection(horizontalDistance = 80f, threshold = 56f))
        assertNull(sectionSwipeDirection(horizontalDistance = 40f, threshold = 56f))
    }

    @Test
    fun movePlacesDraggedItemAtTargetPosition() {
        assertEquals(listOf("b", "c", "a"), listOf("a", "b", "c").move("a", "c"))
        assertEquals(listOf("c", "a", "b"), listOf("a", "b", "c").move("c", "a"))
    }

    @Test
    fun moveIgnoresUnknownOrSameTarget() {
        assertNull(listOf("a", "b").move("missing", "b"))
        assertNull(listOf("a", "b").move("a", "a"))
    }

    @Test
    fun savedOrderKeepsNewCatalogItemsAtTheEndInRepositoryOrder() {
        val market = listOf(item("new-a"), item("saved-b"), item("saved-a"), item("new-b"))

        val ordered = market.orderedByConceptIds(listOf("saved-a", "saved-b"))

        assertEquals(
            listOf("saved-a", "saved-b", "new-a", "new-b"),
            ordered.map { it.concept.id.value },
        )
    }

    private fun item(id: String) = MarketItem(
        concept = ProduceConcept(
            id = ProduceConceptId(id),
            householdName = id,
            aliases = emptyList(),
            category = ProduceCategory.VEGETABLE,
            published = true,
            illustrationAsset = null,
            officialVariants = emptyList(),
        ),
        latestEstimate = null,
    )
}
