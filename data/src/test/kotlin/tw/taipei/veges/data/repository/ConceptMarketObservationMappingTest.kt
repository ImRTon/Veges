package tw.taipei.veges.data.repository

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tw.taipei.veges.data.local.ConceptMarketObservation

class ConceptMarketObservationMappingTest {
    private val complete = ConceptMarketObservation(
        conceptId = "vegetable.cabbage",
        observedOn = LocalDate.parse("2026-09-12"),
        averagePrice = BigDecimal("40.5"),
        volume = BigDecimal("25470"),
    )

    @Test
    fun completeProjectionIsAccepted() {
        val result = complete.completeOrNull()

        assertEquals("vegetable.cabbage", result?.conceptId)
        assertEquals(LocalDate.parse("2026-09-12"), result?.observedOn)
        assertEquals(BigDecimal("40.5"), result?.averagePrice)
        assertEquals(BigDecimal("25470"), result?.volume)
    }

    @Test
    fun missingConceptIdIsDroppedInsteadOfCrashing() {
        assertNull(complete.copy(conceptId = null).completeOrNull())
    }

    @Test
    fun anyIncompletePriceObservationIsDropped() {
        assertNull(complete.copy(observedOn = null).completeOrNull())
        assertNull(complete.copy(averagePrice = null).completeOrNull())
        assertNull(complete.copy(volume = null).completeOrNull())
    }
}
