package tw.taipei.veges.releasetool.catalog

import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import tw.taipei.veges.releasetool.artifact.ArtifactCodec
import tw.taipei.veges.releasetool.artifact.PublicationState
import tw.taipei.veges.releasetool.artifact.ReviewMetadata
import tw.taipei.veges.releasetool.artifact.ReviewStatus
import tw.taipei.veges.releasetool.artifact.TaxonomyArtifact
import tw.taipei.veges.releasetool.audit.ArtifactValidator

@Serializable
data class CatalogIllustrationApprovalAudit(
    val schemaVersion: Int,
    val status: String,
    val taxonomyVersion: String,
    val sourceConceptCount: Int,
    val assets: List<CatalogIllustrationApproval>,
)

@Serializable
data class CatalogIllustrationApproval(
    val conceptId: String,
    val assetPath: String,
    val sha256: String,
    val approval: ExactHashApproval,
)

@Serializable
data class ExactHashApproval(
    val status: String,
    val reviewedAt: String? = null,
    val reviewedBy: String? = null,
    val scope: String? = null,
)

object ApprovedTaxonomyFinalizer {
    private val auditJson = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }
    private val sha256Pattern = Regex("[0-9a-f]{64}")

    fun decodeAudit(raw: String): CatalogIllustrationApprovalAudit =
        auditJson.decodeFromString(raw)

    fun finalize(
        candidate: TaxonomyArtifact,
        illustrationAudit: CatalogIllustrationApprovalAudit,
        taxonomyVersion: String,
        approvedAt: String,
        approvedBy: String,
    ): TaxonomyArtifact {
        ArtifactValidator.validateTaxonomy(candidate)
        require(candidate.artifactChecksum == ArtifactCodec.taxonomyChecksum(candidate)) {
            "Candidate taxonomy checksum mismatch"
        }
        require(taxonomyVersion.isNotBlank()) { "Launch taxonomy version is required" }
        LocalDate.parse(approvedAt)
        require(approvedBy.isNotBlank()) { "Catalog approver is required" }
        require(illustrationAudit.schemaVersion == 1) {
            "Unsupported illustration approval audit schema"
        }
        require(illustrationAudit.status == "APPROVED") {
            "Every catalog illustration must be approved"
        }
        require(illustrationAudit.taxonomyVersion == candidate.taxonomyVersion) {
            "Illustration audit taxonomy version mismatch"
        }
        require(illustrationAudit.sourceConceptCount == candidate.concepts.size) {
            "Illustration audit concept count mismatch"
        }
        require(illustrationAudit.assets.map { it.conceptId }.distinct().size == illustrationAudit.assets.size) {
            "Illustration approval concept IDs must be unique"
        }
        require(illustrationAudit.assets.map { it.sha256 }.distinct().size == illustrationAudit.assets.size) {
            "Illustration approval hashes must be unique"
        }

        val approvalsByConcept = illustrationAudit.assets.associateBy { it.conceptId }
        require(approvalsByConcept.keys == candidate.concepts.map { it.stableId }.toSet()) {
            "Illustration approval coverage must exactly match taxonomy concepts"
        }
        candidate.concepts.forEach { concept ->
            val row = requireNotNull(approvalsByConcept[concept.stableId])
            require(row.assetPath == concept.image.assetPath) {
                "Illustration path mismatch for ${concept.stableId}"
            }
            require(row.sha256.matches(sha256Pattern)) {
                "Invalid illustration hash for ${concept.stableId}"
            }
            require(
                row.approval.status == "APPROVED" &&
                    row.approval.scope == "EXACT_SHA256" &&
                    row.approval.reviewedAt == approvedAt &&
                    row.approval.reviewedBy == approvedBy,
            ) {
                "Exact-hash approval is incomplete for ${concept.stableId}"
            }
        }

        val draft = candidate.copy(
            taxonomyVersion = taxonomyVersion,
            artifactChecksum = "",
            review = ReviewMetadata(
                status = ReviewStatus.APPROVED,
                reviewedAt = approvedAt,
                reviewedBy = approvedBy,
                notes = "Project owner approved the exhaustive 120-vegetable launch breadth and all 121 exact catalog illustration hashes.",
            ),
            concepts = candidate.concepts.map { concept ->
                concept.copy(
                    publicationState = PublicationState.PUBLISHED,
                    image = concept.image.copy(
                        reviewedAt = approvedAt,
                        reviewedBy = approvedBy,
                        reviewStatus = ReviewStatus.APPROVED,
                    ),
                )
            },
        )
        val result = draft.copy(artifactChecksum = ArtifactCodec.taxonomyChecksum(draft))
        ArtifactValidator.validateTaxonomy(result)
        return result
    }
}
