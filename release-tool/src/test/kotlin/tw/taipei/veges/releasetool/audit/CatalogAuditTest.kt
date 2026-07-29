package tw.taipei.veges.releasetool.audit

import java.nio.file.Files
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.taipei.veges.releasetool.artifact.AmbiguitySetArtifact
import tw.taipei.veges.releasetool.artifact.ArtifactCodec
import tw.taipei.veges.releasetool.artifact.ImageMetadataArtifact
import tw.taipei.veges.releasetool.artifact.OfficialMappingArtifact
import tw.taipei.veges.releasetool.artifact.PublicationState
import tw.taipei.veges.releasetool.artifact.ReviewMetadata
import tw.taipei.veges.releasetool.artifact.ReviewStatus
import tw.taipei.veges.releasetool.artifact.SourceObservationArtifact
import tw.taipei.veges.releasetool.artifact.SourceSnapshotArtifact
import tw.taipei.veges.releasetool.artifact.TaxonomyArtifact
import tw.taipei.veges.releasetool.artifact.TaxonomyCategory
import kotlinx.serialization.decodeFromString

class CatalogAuditTest {
    @Test
    fun bundledTaxonomyIsDeterministicAndExplicitlyApprovedForPublication() {
        val json = requireNotNull(javaClass.classLoader?.getResourceAsStream("catalog/candidate-taxonomy.json"))
            .bufferedReader()
            .use { it.readText() }
        val taxonomy = ArtifactCodec.strictJson.decodeFromString<TaxonomyArtifact>(json)

        ArtifactValidator.validateTaxonomy(taxonomy)
        assertEquals(taxonomy.artifactChecksum, ArtifactCodec.taxonomyChecksum(taxonomy))
        assertEquals(ReviewStatus.APPROVED, taxonomy.review.status)
        assertEquals("project-owner", taxonomy.review.reviewedBy)
        assertEquals(121, taxonomy.concepts.size)
        assertEquals(120, taxonomy.concepts.count { it.category == tw.taipei.veges.releasetool.artifact.TaxonomyCategory.VEGETABLE })
        assertEquals(1, taxonomy.concepts.count { it.category == tw.taipei.veges.releasetool.artifact.TaxonomyCategory.FRUIT })
        assertTrue(taxonomy.concepts.all { it.publicationState == PublicationState.PUBLISHED })
        assertTrue(taxonomy.concepts.all { it.image.reviewStatus == ReviewStatus.APPROVED })
        assertEquals(
            "青江菜",
            taxonomy.concepts.single { it.stableId == "vegetable.qingjiang-bok-choy" }.householdName,
        )
        assertTrue(taxonomy.concepts.any { it.householdName == "空心菜" })
        assertTrue(taxonomy.concepts.any { it.householdName == "地瓜葉" })
        assertTrue(taxonomy.ambiguitySets.any { it.normalizedAlias == "白菜" && it.targetConceptIds.size == 3 })
    }

    @Test
    fun auditProducesMetricsAndExclusionsWithoutPublishingCandidates() {
        val taxonomy = reviewedTaxonomy()
        val snapshot = reviewedSnapshot()
        val result = CatalogAuditRunner().audit(
            taxonomy = taxonomy,
            snapshot = snapshot,
            options = AuditOptions("2026-07-14T03:00:00Z"),
        )

        assertEquals(2, result.metrics.size)
        assertEquals(2, result.metrics.first { it.conceptId == "fruit.banana" }.mappingCount)
        assertEquals(
            "NO_VALID_SOURCE_OBSERVATION",
            result.exclusions.first { it.conceptId == "fruit.banana" }.reasonCode,
        )
        assertTrue(result.metrics.all { it.publicationState == PublicationState.CANDIDATE })
        assertTrue(result.outputChecksum.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun identicalInputsProduceIdenticalCanonicalArtifacts() {
        val first = reviewedTaxonomy()
        val second = first.copy(
            concepts = first.concepts.reversed(),
            ambiguitySets = first.ambiguitySets.reversed(),
        )

        assertEquals(ArtifactCodec.encodeTaxonomy(first), ArtifactCodec.encodeTaxonomy(second))
        assertEquals(ArtifactCodec.taxonomyChecksum(first), ArtifactCodec.taxonomyChecksum(second))
    }

    @Test
    fun changedReviewedMappingChangesChecksum() {
        val original = reviewedTaxonomy()
        val changed = original.copy(
            concepts = original.concepts.map { concept ->
                if (concept.stableId == "vegetable.cabbage") {
                    concept.copy(officialMappings = concept.officialMappings.dropLast(1))
                } else {
                    concept
                }
            },
            artifactChecksum = "",
        )

        assertNotEquals(ArtifactCodec.taxonomyChecksum(original), ArtifactCodec.taxonomyChecksum(changed))
    }

    @Test
    fun invalidTaxonomyRejectsDanglingAmbiguity() {
        val invalid = reviewedTaxonomy().copy(
            ambiguitySets = listOf(
                AmbiguitySetArtifact("白菜", "白菜", listOf("missing.one", "missing.two")),
            ),
        )

        val error = runCatching { ArtifactValidator.validateTaxonomy(invalid) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun invalidSnapshotRejectsFutureObservationAndZeroValues() {
        val invalid = reviewedSnapshot().copy(
            observations = listOf(
                SourceObservationArtifact(
                    observedOn = "2026-07-15",
                    commodityCode = "LA1",
                    officialName = "甘藍-初秋",
                    market = "TAIPEI_SECOND",
                    valid = true,
                    averagePriceNtdPerKg = "0",
                    volumeKg = "100",
                ),
            ),
        )

        val error = runCatching { ArtifactValidator.validateSnapshot(invalid) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    private fun reviewedTaxonomy(): TaxonomyArtifact {
        val artifact = TaxonomyArtifact(
            schemaVersion = 1,
            taxonomyVersion = "candidate-2026-07-14",
            artifactChecksum = "",
            review = ReviewMetadata(
                status = ReviewStatus.REVIEWED,
                reviewedAt = "2026-07-14",
                reviewedBy = "data-review",
            ),
            concepts = listOf(
                concept(
                    id = "vegetable.cabbage",
                    name = "高麗菜",
                    aliases = listOf("甘藍", "高麗菜"),
                    category = TaxonomyCategory.VEGETABLE,
                    mappings = listOf(
                        OfficialMappingArtifact("LA1", "甘藍-初秋", "TAIPEI_SECOND"),
                        OfficialMappingArtifact("LA2", "甘藍-改良種", "TAIPEI_FIRST"),
                    ),
                ),
                concept(
                    id = "fruit.banana",
                    name = "香蕉",
                    aliases = listOf("香蕉"),
                    category = TaxonomyCategory.FRUIT,
                    mappings = listOf(
                        OfficialMappingArtifact("A1", "香蕉", "TAIPEI_FIRST"),
                        OfficialMappingArtifact("A1", "香蕉", "TAIPEI_SECOND"),
                    ),
                ),
            ),
            ambiguitySets = emptyList(),
        )
        return artifact.copy(artifactChecksum = ArtifactCodec.taxonomyChecksum(artifact))
    }

    private fun concept(
        id: String,
        name: String,
        aliases: List<String>,
        category: TaxonomyCategory,
        mappings: List<OfficialMappingArtifact>,
    ) = tw.taipei.veges.releasetool.artifact.TaxonomyConceptArtifact(
        stableId = id,
        householdName = name,
        aliases = aliases,
        category = category,
        publicationState = PublicationState.CANDIDATE,
        officialMappings = mappings,
        image = ImageMetadataArtifact(
            assetPath = "illustrations/$id.webp",
            generated = true,
            disclosure = AI_IMAGE_DISCLOSURE,
            reviewedAt = null,
            reviewedBy = null,
            reviewStatus = ReviewStatus.REVIEWED,
        ),
    )

    private fun reviewedSnapshot(): SourceSnapshotArtifact {
        val snapshot = SourceSnapshotArtifact(
            schemaVersion = 1,
            source = "MOA_WHOLESALE",
            snapshotId = "moa-2026-07-14",
            dataCutoff = "2026-07-14",
            observations = listOf(
                SourceObservationArtifact(
                    observedOn = "2026-07-14",
                    commodityCode = "LA1",
                    officialName = "甘藍-初秋",
                    market = "TAIPEI_SECOND",
                    valid = true,
                    averagePriceNtdPerKg = "40.5",
                    volumeKg = "25470",
                ),
                SourceObservationArtifact(
                    observedOn = "2026-07-14",
                    commodityCode = "LA2",
                    officialName = "甘藍-改良種",
                    market = "TAIPEI_FIRST",
                    valid = true,
                    averagePriceNtdPerKg = "41.8",
                    volumeKg = "19800",
                ),
            ),
        )
        return snapshot.copy(snapshotChecksum = ArtifactCodec.snapshotChecksum(snapshot))
    }
}
