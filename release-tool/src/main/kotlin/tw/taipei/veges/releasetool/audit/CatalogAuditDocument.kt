package tw.taipei.veges.releasetool.audit

import kotlinx.serialization.Serializable
import tw.taipei.veges.releasetool.artifact.PublicationState

@Serializable
data class CatalogAuditDocument(
    val schemaVersion: Int,
    val taxonomyVersion: String,
    val taxonomyChecksum: String,
    val sourceSnapshotId: String,
    val sourceSnapshotChecksum: String,
    val dataCutoff: String,
    val generatedAt: String,
    val metrics: List<SerializableConceptAuditMetrics>,
    val exclusions: List<SerializableExclusion>,
    val outputChecksum: String,
)

@Serializable
data class SerializableConceptAuditMetrics(
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

@Serializable
data class SerializableExclusion(
    val conceptId: String,
    val reasonCode: String,
    val detail: String,
)

fun CatalogAuditResult.toDocument(): CatalogAuditDocument = CatalogAuditDocument(
    schemaVersion = 1,
    taxonomyVersion = taxonomyVersion,
    taxonomyChecksum = taxonomyChecksum,
    sourceSnapshotId = sourceSnapshotId,
    sourceSnapshotChecksum = sourceSnapshotChecksum,
    dataCutoff = dataCutoff,
    generatedAt = generatedAt,
    metrics = metrics.map { metric ->
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
    },
    exclusions = exclusions.map { SerializableExclusion(it.conceptId, it.reasonCode, it.detail) },
    outputChecksum = outputChecksum,
)
