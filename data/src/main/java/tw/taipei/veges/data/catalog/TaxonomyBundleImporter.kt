package tw.taipei.veges.data.catalog

import android.content.Context
import androidx.room.withTransaction
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tw.taipei.veges.data.local.OfficialVariantEntity
import tw.taipei.veges.data.local.TaxonomyAliasEntity
import tw.taipei.veges.data.local.TaxonomyConceptEntity
import tw.taipei.veges.data.local.VegesDatabase
import tw.taipei.veges.data.repository.normalizeSearchQuery
import tw.taipei.veges.data.repository.AI_ILLUSTRATION_DISCLOSURE
import tw.taipei.veges.domain.ProduceCategory

@Serializable
private data class BundledTaxonomy(
    val schemaVersion: Int,
    val taxonomyVersion: String,
    val artifactChecksum: String,
    val review: ReviewDto,
    val concepts: List<ConceptDto>,
    val ambiguitySets: List<AmbiguityDto> = emptyList(),
)

@Serializable
private data class ReviewDto(
    val status: String,
    val reviewedAt: String,
    val reviewedBy: String,
    val notes: String? = null,
)

@Serializable
private data class ConceptDto(
    val stableId: String,
    val householdName: String,
    val aliases: List<String>,
    val category: String,
    val publicationState: String,
    val officialMappings: List<MappingDto>,
    val image: ImageDto,
)

@Serializable
private data class MappingDto(val commodityCode: String, val officialName: String, val market: String)

@Serializable
private data class ImageDto(
    val assetPath: String,
    val generated: Boolean,
    val disclosure: String,
    val reviewedAt: String?,
    val reviewedBy: String?,
    val reviewStatus: String,
)

@Serializable
private data class AmbiguityDto(
    val normalizedAlias: String,
    val displayAlias: String,
    val targetConceptIds: List<String>,
)

data class TaxonomyImportResult(
    val taxonomyVersion: String,
    val importedConcepts: Int,
    val importedAliases: Int,
    val importedMappings: Int,
)

class TaxonomyBundleImporter @Inject constructor(
    private val database: VegesDatabase,
) {
    private val json = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
        explicitNulls = false
    }

    suspend fun importAsset(context: Context, assetPath: String = DEFAULT_ASSET): TaxonomyImportResult {
        val raw = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        return importJson(raw)
    }

    suspend fun importJson(raw: String): TaxonomyImportResult {
        val artifact = json.decodeFromString<BundledTaxonomy>(raw)
        validate(artifact, raw)
        val concepts = artifact.concepts.map { concept ->
            TaxonomyConceptEntity(
                stableId = concept.stableId,
                householdName = concept.householdName,
                normalizedHouseholdName = normalizeSearchQuery(concept.householdName),
                category = ProduceCategory.valueOf(concept.category),
                published = concept.publicationState == "PUBLISHED",
                illustrationAsset = concept.image.assetPath,
                illustrationDisclosure = concept.image.disclosure,
                taxonomyVersion = artifact.taxonomyVersion,
                artifactChecksum = artifact.artifactChecksum,
                reviewedAt = java.time.Instant.parse("${concept.image.reviewedAt ?: artifact.review.reviewedAt}T00:00:00Z"),
                reviewer = concept.image.reviewedBy ?: artifact.review.reviewedBy,
            )
        }
        val aliases = artifact.concepts.flatMap { concept ->
            concept.aliases.map { alias ->
                TaxonomyAliasEntity(concept.stableId, normalizeSearchQuery(alias), alias)
            }
        }
        val variants = artifact.concepts.flatMap { concept ->
            concept.officialMappings.map { mapping ->
                OfficialVariantEntity(concept.stableId, mapping.commodityCode, mapping.officialName, tw.taipei.veges.domain.MarketBasis.valueOf(mapping.market))
            }
        }
        database.withTransaction {
            database.taxonomyDao().replaceVersion(artifact.taxonomyVersion, concepts, aliases, variants)
        }
        return TaxonomyImportResult(artifact.taxonomyVersion, concepts.size, aliases.size, variants.size)
    }

    private fun validate(artifact: BundledTaxonomy, raw: String) {
        require(artifact.schemaVersion == 1) { "Unsupported taxonomy schema" }
        require(artifact.artifactChecksum == computeBundledTaxonomyChecksum(raw)) {
            "Taxonomy checksum mismatch"
        }
        require(artifact.review.reviewedAt.isNotBlank() && artifact.review.reviewedBy.isNotBlank()) { "Review metadata required" }
        require(artifact.review.status == "APPROVED") { "Bundled taxonomy review must be approved" }
        require(artifact.concepts.map { it.stableId }.distinct().size == artifact.concepts.size) { "Stable IDs must be unique" }
        val conceptIds = artifact.concepts.map { it.stableId }.toSet()
        artifact.concepts.forEach { concept ->
            require(concept.householdName.isNotBlank() && concept.aliases.isNotEmpty()) { "Concept identity incomplete" }
            require(concept.image.disclosure == AI_ILLUSTRATION_DISCLOSURE) { "Invalid image disclosure" }
            require(concept.officialMappings.isNotEmpty()) { "Official mapping required" }
            if (concept.publicationState == "PUBLISHED") {
                require(
                    concept.image.reviewStatus == "APPROVED" &&
                        !concept.image.reviewedAt.isNullOrBlank() &&
                        !concept.image.reviewedBy.isNullOrBlank(),
                ) {
                    "Published concept ${concept.stableId} requires approved image review"
                }
            }
            concept.officialMappings.forEach { mapping ->
                require(mapping.market == "TAIPEI_FIRST" || mapping.market == "TAIPEI_SECOND") { "Invalid market mapping" }
            }
        }
        artifact.ambiguitySets.forEach { set ->
            require(set.targetConceptIds.size >= 2 && set.targetConceptIds.all { it in conceptIds }) { "Invalid ambiguity set" }
            require(normalizeSearchQuery(set.displayAlias) == set.normalizedAlias) { "Ambiguity normalization mismatch" }
        }
    }

    private companion object {
        const val DEFAULT_ASSET = "taxonomy/candidate-taxonomy.json"
    }
}

internal fun computeBundledTaxonomyChecksum(raw: String): String {
    val checksumJson = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
        explicitNulls = false
    }
    val artifact = checksumJson.decodeFromString<BundledTaxonomy>(raw)
    val canonical = artifact.copy(artifactChecksum = "").canonical()
    return MessageDigest.getInstance("SHA-256")
        .digest(checksumJson.encodeToString(canonical).toByteArray())
        .joinToString("") { "%02x".format(it) }
}

private fun BundledTaxonomy.canonical(): BundledTaxonomy = copy(
    concepts = concepts.sortedBy { it.stableId }.map { concept ->
        concept.copy(
            aliases = concept.aliases.sortedWith(String.CASE_INSENSITIVE_ORDER),
            officialMappings = concept.officialMappings.sortedWith(
                compareBy({ it.commodityCode }, { it.market }, { it.officialName }),
            ),
        )
    },
    ambiguitySets = ambiguitySets.sortedBy { it.normalizedAlias }.map { ambiguity ->
        ambiguity.copy(targetConceptIds = ambiguity.targetConceptIds.sorted())
    },
)
