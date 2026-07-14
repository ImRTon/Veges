package tw.taipei.veges.designsystem

import java.math.BigDecimal
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.taipei.veges.domain.PriceUnit

class PriceFormattingTest {
    @Test
    fun taiwanPriceFormattingIncludesUnit() {
        val result = formatPrice(BigDecimal("52.5"), PriceUnit.NTD_PER_TAI_JIN)
        assertTrue(result.contains("台斤"))
        assertTrue(result.contains("52.5"))
    }
}
