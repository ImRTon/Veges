package tw.taipei.veges.data.catalog

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class BundledTaxonomyChecksumTest {
    @Test
    fun `published bundled taxonomy matches reviewed canonical checksum`() {
        val raw = File("src/main/assets/taxonomy/candidate-taxonomy.json").readText()

        assertEquals(
            "df8e74f9c76891e0f35485c9dfc602958b840fa575ccf8f9434746c97e45b9e2",
            computeBundledTaxonomyChecksum(raw),
        )
    }
}
