package tw.taipei.veges.releasetool.catalog

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogIllustrationBudgetTest {
    @Test
    fun everyCatalogConceptHasOneUniqueOptimizedWebp() {
        val repoRoot = Path.of(requireNotNull(System.getProperty("veges.repoRoot")))
        val auditPath = repoRoot.resolve(
            "release-tool/audits/2026-07-29-catalog-illustration-audit.json",
        )
        val audit = Json.parseToJsonElement(Files.readString(auditPath)).jsonObject
        val assets = audit.getValue("assets").jsonArray
        val actualHashes = mutableSetOf<String>()
        var actualTotalBytes = 0L

        assertEquals(175, assets.size)
        assertEquals("APPROVED", audit.getValue("status").jsonPrimitive.content)
        assertEquals(
            assets.size,
            assets.count {
                val approval = it.jsonObject
                    .getValue("approval")
                    .jsonObject
                approval.getValue("status").jsonPrimitive.content == "APPROVED" &&
                    approval.getValue("scope").jsonPrimitive.content == "EXACT_SHA256"
            },
        )
        assertEquals(
            175,
            audit.getValue("summary").jsonObject.getValue("approvedAssets").jsonPrimitive.int,
        )
        assertEquals(
            0,
            audit.getValue("summary").jsonObject.getValue("pendingAssets").jsonPrimitive.int,
        )
        assets.forEach { element ->
            val row = element.jsonObject
            val relativePath = row.getValue("assetPath").jsonPrimitive.content
            val file = repoRoot.resolve("data/src/main/assets").resolve(relativePath)
            val bytes = Files.readAllBytes(file)
            val sha256 = MessageDigest.getInstance("SHA-256")
                .digest(bytes)
                .joinToString("") { "%02x".format(it) }

            assertTrue(relativePath.endsWith(".webp"))
            assertTrue(Files.isRegularFile(file))
            assertTrue(bytes.size <= MAX_ASSET_BYTES)
            assertEquals(row.getValue("bytes").jsonPrimitive.long, bytes.size.toLong())
            assertEquals(row.getValue("sha256").jsonPrimitive.content, sha256)
            assertTrue(row.getValue("width").jsonPrimitive.int <= MAX_EDGE_PIXELS)
            assertTrue(row.getValue("height").jsonPrimitive.int <= MAX_EDGE_PIXELS)
            assertTrue(row.getValue("transparentCorners").jsonPrimitive.boolean)
            assertTrue("Duplicate image bytes for $relativePath", actualHashes.add(sha256))
            actualTotalBytes += bytes.size
        }

        assertTrue(actualTotalBytes <= MAX_CATALOG_BYTES)
        assertEquals(
            audit.getValue("summary").jsonObject.getValue("totalBytes").jsonPrimitive.long,
            actualTotalBytes,
        )
        assertTrue(
            Files.walk(repoRoot.resolve("data/src/main/assets/illustrations")).use { paths ->
                paths.noneMatch { Files.isRegularFile(it) && it.fileName.toString().endsWith(".png") }
            },
        )
    }

    private companion object {
        const val MAX_EDGE_PIXELS = 512
        const val MAX_ASSET_BYTES = 128 * 1024
        const val MAX_CATALOG_BYTES = 6 * 1024 * 1024
    }
}
