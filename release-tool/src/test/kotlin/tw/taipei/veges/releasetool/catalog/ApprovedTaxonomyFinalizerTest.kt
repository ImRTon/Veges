package tw.taipei.veges.releasetool.catalog

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.taipei.veges.releasetool.artifact.ArtifactCodec
import tw.taipei.veges.releasetool.artifact.PublicationState
import tw.taipei.veges.releasetool.artifact.ReviewStatus
import tw.taipei.veges.releasetool.artifact.TaxonomyArtifact
import tw.taipei.veges.releasetool.artifact.TaxonomyCategory

class ApprovedTaxonomyFinalizerTest {
    private val repoRoot = Path.of(requireNotNull(System.getProperty("veges.repoRoot")))

    @Test
    fun exactHashApprovalFinalizesEveryCatalogConcept() {
        val result = finalize()

        assertEquals(121, result.concepts.size)
        assertEquals(120, result.concepts.count { it.category == TaxonomyCategory.VEGETABLE })
        assertEquals(1, result.concepts.count { it.category == TaxonomyCategory.FRUIT })
        assertEquals(ReviewStatus.APPROVED, result.review.status)
        assertTrue(result.concepts.all { it.publicationState == PublicationState.PUBLISHED })
        assertTrue(result.concepts.all { it.image.reviewStatus == ReviewStatus.APPROVED })
        assertTrue(result.concepts.all { it.image.reviewedAt == "2026-07-27" })
        assertEquals(result.artifactChecksum, ArtifactCodec.taxonomyChecksum(result))
    }

    @Test
    fun pendingOrChangedApprovalCannotFinalize() {
        val candidate = candidate()
        val audit = audit()
        val invalid = audit.copy(
            status = "PENDING_PROJECT_OWNER_APPROVAL",
            assets = audit.assets.mapIndexed { index, row ->
                if (index == 0) {
                    row.copy(approval = row.approval.copy(status = "PENDING_PROJECT_OWNER_APPROVAL"))
                } else {
                    row
                }
            },
        )

        val failure = runCatching {
            ApprovedTaxonomyFinalizer.finalize(
                candidate = candidate,
                illustrationAudit = invalid,
                taxonomyVersion = "launch-all-produce-2026-07-27",
                approvedAt = "2026-07-27",
                approvedBy = "project-owner",
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    private fun finalize(): TaxonomyArtifact = ApprovedTaxonomyFinalizer.finalize(
        candidate = candidate(),
        illustrationAudit = audit(),
        taxonomyVersion = "launch-all-produce-2026-07-27",
        approvedAt = "2026-07-27",
        approvedBy = "project-owner",
    )

    private fun candidate(): TaxonomyArtifact = ArtifactCodec.strictJson.decodeFromString(
        Files.readString(
            repoRoot.resolve("release-tool/audits/2026-07-26-exhaustive-vegetable-taxonomy.json"),
        ),
    )

    private fun audit(): CatalogIllustrationApprovalAudit = ApprovedTaxonomyFinalizer.decodeAudit(
        Files.readString(
            repoRoot.resolve("release-tool/audits/2026-07-27-catalog-illustration-audit.json"),
        ),
    )
}
