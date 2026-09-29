package tw.taipei.veges.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tw.taipei.veges.domain.HomeItem
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.ProduceConcept
import tw.taipei.veges.domain.ProduceConceptId

class HomeOrderTest {
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
    fun savedOrderKeepsNewTrackedItemsAtTheEnd() {
        val items = listOf(homeItem("a"), homeItem("b"), homeItem("new"))

        assertEquals(
            listOf("b", "a", "new"),
            items.orderedBySavedIds(listOf("b", "a")).map { it.concept.id.value },
        )
    }

    private fun homeItem(id: String) = HomeItem(
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
        unavailableReason = null,
    )
}
