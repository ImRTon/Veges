package tw.taipei.veges.data.catalog

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class BundledTaxonomyChecksumTest {
    @Test
    fun `published bundled taxonomy matches reviewed canonical checksum`() {
        val raw = File("src/main/assets/taxonomy/candidate-taxonomy.json").readText()

        assertEquals(
            "2c17d98130a15671becd4aeb87d61f25e1ca631f46c334767ae0d7ab5867dbc0",
            computeBundledTaxonomyChecksum(raw),
        )
    }
}
