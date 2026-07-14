package tw.taipei.veges.releasetool.artifact

import kotlinx.serialization.Serializable

@Serializable
data class TaxonomyArtifact(
    val schemaVersion: Int,
    val taxonomyVersion: String,
    val artifactChecksum: String,
    val review: ReviewMetadata,
    val concepts: List<TaxonomyConceptArtifact>,
    val ambiguitySets: List<AmbiguitySetArtifact> = emptyList(),
)

@Serializable
data class ReviewMetadata(
    val status: ReviewStatus,
    val reviewedAt: String,
    val reviewedBy: String,
    val notes: String? = null,
)

@Serializable
enum class ReviewStatus {
    DRAFT,
    REVIEWED,
    APPROVED,
    REJECTED,
}

@Serializable
enum class TaxonomyCategory {
    FRUIT,
    VEGETABLE,
}

@Serializable
data class TaxonomyConceptArtifact(
    val stableId: String,
    val householdName: String,
    val aliases: List<String>,
    val category: TaxonomyCategory,
    val publicationState: PublicationState,
    val officialMappings: List<OfficialMappingArtifact>,
    val image: ImageMetadataArtifact,
)

@Serializable
enum class PublicationState {
    CANDIDATE,
    PUBLISHED,
    EXCLUDED,
}

@Serializable
data class OfficialMappingArtifact(
    val commodityCode: String,
    val officialName: String,
    val market: String,
)

@Serializable
data class ImageMetadataArtifact(
    val assetPath: String,
    val generated: Boolean,
    val disclosure: String,
    val reviewedAt: String?,
    val reviewedBy: String?,
    val reviewStatus: ReviewStatus,
)

@Serializable
data class AmbiguitySetArtifact(
    val normalizedAlias: String,
    val displayAlias: String,
    val targetConceptIds: List<String>,
)
