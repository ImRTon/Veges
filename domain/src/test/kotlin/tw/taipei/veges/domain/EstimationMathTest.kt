package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EstimationMathTest {
    @Test
    fun normalizesKilogramToTaiJinWithDecimalRounding() {
        assertEquals(BigDecimal("16.67"), EstimationMath.kilogramsToTaiJin(BigDecimal("10")))
    }

    @Test
    fun combinesSameDateMarketsByTransactionVolume() {
        val result = combineTaipeiWholesale(
            first = MarketWholesaleInput(
                MarketBasis.TAIPEI_FIRST,
                LocalDate.parse("2026-07-14"),
                BigDecimal("40"), BigDecimal("30"), BigDecimal("50"), BigDecimal("100"),
            ),
            second = MarketWholesaleInput(
                MarketBasis.TAIPEI_SECOND,
                LocalDate.parse("2026-07-14"),
                BigDecimal("60"), BigDecimal("50"), BigDecimal("70"), BigDecimal("300"),
            ),
        )

        assertEquals(BigDecimal("55.00000000"), (result as CombinedWholesaleResult.Success).averageNtdPerKg)
        assertEquals(BigDecimal("400"), result.volumeKg)
    }

    @Test
    fun combinedBasisDoesNotSubstituteMissingMarket() {
        val result = combineTaipeiWholesale(null, null)

        assertTrue(result is CombinedWholesaleResult.Unavailable)
        assertEquals(UnavailableReason.MISSING_SOURCE_DATA, (result as CombinedWholesaleResult.Unavailable).reason)
    }
}
