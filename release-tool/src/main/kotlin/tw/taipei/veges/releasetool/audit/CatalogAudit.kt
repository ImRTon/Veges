package tw.taipei.veges.releasetool.audit

import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import tw.taipei.veges.releasetool.artifact.AmbiguitySetArtifact
import tw.taipei.veges.releasetool.artifact.ArtifactCodec
import tw.taipei.veges.releasetool.artifact.OfficialMappingArtifact
import tw.taipei.veges.releasetool.artifact.PublicationState
import tw.taipei.veges.releasetool.artifact.SourceSnapshotArtifact
import tw.taipei.veges.releasetool.artifact.TaxonomyArtifact

const val AI_IMAGE_DISCLOSURE = "AI 生成示意圖，非實物照片。"

data class AuditOptions(
    val generatedAt: String,
)

data class CatalogAuditResult(
    val taxonomyVersion: String,
    val taxonomyChecksum: String,
    val sourceSnapshotId: String,
    val sourceSnapshotChecksum: String,
    val dataCutoff: String,
    val generatedAt: String,
    val metrics: List<ConceptAuditMetrics>,
    val exclusions: List<Exclusion>,
    val outputChecksum: String,
)

data class ConceptAuditMetrics(
    val conceptId: String,
    val publicationState: PublicationState,
    val mappingCount: Int,
    val validObservationCount: Int,
    val observedDayCount: Int,
    val expectedDayCount: Int,
    val coverageRatio: String,
    val latestObservedOn: String?,
    val recencyDays: Long?,
    val sourceBases: List<String>,
)

data class Exclusion(
    val conceptId: String,
    val reasonCode: String,
    val detail: String,
)

class CatalogAuditRunner {
    fun audit(
        taxonomy: TaxonomyArtifact,
        snapshot: SourceSnapshotArtifact,
        options: AuditOptions,
    ): CatalogAuditResult {
        ArtifactValidator.validateTaxonomy(taxonomy)
        ArtifactValidator.validateSnapshot(snapshot)

        val taxonomyChecksum = ArtifactCodec.taxonomyChecksum(taxonomy)
        require(taxonomy.artifactChecksum == taxonomyChecksum) {
            "Taxonomy checksum mismatch: expected $taxonomyChecksum"
        }
        val snapshotChecksum = ArtifactCodec.snapshotChecksum(snapshot)
        if (snapshot.snapshotChecksum.isNotBlank()) {
            require(snapshot.snapshotChecksum == snapshotChecksum) {
                "Source snapshot checksum mismatch: expected $snapshotChecksum"
            }
        }

        val cutoff = LocalDate.parse(snapshot.dataCutoff)
        val metrics = taxonomy.concepts.sortedBy { it.stableId }.map { concept ->
            metricsFor(concept.stableId, concept.officialMappings, snapshot, cutoff, concept.publicationState)
        }
        val exclusions = buildList {
            taxonomy.concepts.sortedBy { it.stableId }.forEach { concept ->
                val metric = metrics.first { it.conceptId == concept.stableId }
                if (concept.officialMappings.isEmpty()) {
                    add(Exclusion(concept.stableId, "NO_OFFICIAL_MAPPING", "No reviewed official mapping"))
                }
                if (metric.validObservationCount == 0) {
                    add(Exclusion(concept.stableId, "NO_VALID_SOURCE_OBSERVATION", "No valid matching source observation"))
                }
                if (concept.publicationState == PublicationState.PUBLISHED && taxonomy.review.status != tw.taipei.veges.releasetool.artifact.ReviewStatus.APPROVED) {
                    add(Exclusion(concept.stableId, "REVIEW_NOT_APPROVED", "Published state requires approved review metadata"))
                }
            }
        }.sortedWith(compareBy({ it.conceptId }, { it.reasonCode }))

        val serializableMetrics = metrics.map { metric ->
            SerializableConceptAuditMetrics(
                conceptId = metric.conceptId,
                publicationState = metric.publicationState,
                mappingCount = metric.mappingCount,
                validObservationCount = metric.validObservationCount,
                observedDayCount = metric.observedDayCount,
                expectedDayCount = metric.expectedDayCount,
                coverageRatio = metric.coverageRatio,
                latestObservedOn = metric.latestObservedOn,
                recencyDays = metric.recencyDays,
                sourceBases = metric.sourceBases,
            )
        }
        val serializableExclusions = exclusions.map {
            SerializableExclusion(it.conceptId, it.reasonCode, it.detail)
        }
        val withoutChecksum = CatalogAuditDocument(
            schemaVersion = 1,
            taxonomyVersion = taxonomy.taxonomyVersion,
            taxonomyChecksum = taxonomyChecksum,
            sourceSnapshotId = snapshot.snapshotId,
            sourceSnapshotChecksum = snapshotChecksum,
            dataCutoff = snapshot.dataCutoff,
            generatedAt = options.generatedAt,
            metrics = serializableMetrics,
            exclusions = serializableExclusions,
            outputChecksum = "",
        )
        val checksum = ArtifactCodec.sha256(ArtifactCodec.strictJson.encodeToString(withoutChecksum))
        return CatalogAuditResult(
            taxonomyVersion = withoutChecksum.taxonomyVersion,
            taxonomyChecksum = withoutChecksum.taxonomyChecksum,
            sourceSnapshotId = withoutChecksum.sourceSnapshotId,
            sourceSnapshotChecksum = withoutChecksum.sourceSnapshotChecksum,
            dataCutoff = withoutChecksum.dataCutoff,
            generatedAt = withoutChecksum.generatedAt,
            metrics = metrics,
            exclusions = exclusions,
            outputChecksum = checksum,
        )
    }

    private fun metricsFor(
        conceptId: String,
        mappings: List<OfficialMappingArtifact>,
        snapshot: SourceSnapshotArtifact,
        cutoff: LocalDate,
        publicationState: PublicationState,
    ): ConceptAuditMetrics {
        val codes = mappings.map { it.commodityCode }.toSet()
        val matching = snapshot.observations.filter { it.commodityCode in codes }
        val valid = matching.filter { it.valid }
        val observedDates = valid.map { LocalDate.parse(it.observedOn) }.distinct().sorted()
        val first = observedDates.firstOrNull()
        val latest = observedDates.lastOrNull()
        val expectedDays = if (first == null) 0 else ChronoUnit.DAYS.between(first, cutoff).toInt() + 1
        val coverage = if (expectedDays == 0) BigDecimal.ZERO else
            BigDecimal(observedDates.size).divide(BigDecimal(expectedDays), 6, java.math.RoundingMode.HALF_UP)
        return ConceptAuditMetrics(
            conceptId = conceptId,
            publicationState = publicationState,
            mappingCount = mappings.size,
            validObservationCount = valid.size,
            observedDayCount = observedDates.size,
            expectedDayCount = expectedDays,
            coverageRatio = coverage.toPlainString(),
            latestObservedOn = latest?.toString(),
            recencyDays = latest?.let { ChronoUnit.DAYS.between(it, cutoff) },
            sourceBases = mappings.map { it.market }.distinct().sorted(),
        )
    }
}
